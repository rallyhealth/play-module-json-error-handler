package com.rallyhealth.playmodule.jsonerrors

import org.scalatest.AsyncFreeSpec
import play.api.http.{DefaultHttpErrorHandler, HttpErrorConfig, HttpErrorHandlerExceptions, Status}
import play.api.libs.json.Json
import play.api.mvc._
import play.api.routing.Router
import play.api.routing.Router.Routes
import play.api.test.ops.AsyncResultExtractors
import play.api.test.{FakeHeaders, FakeRequest}
import play.core.SourceMapper

import scala.concurrent.Future
import scala.util.matching.Regex

class JsonHttpErrorHandlingSpec extends AsyncFreeSpec with AsyncResultExtractors {

  class FixtureParam(val router: Router, showDevErrors: Boolean) {
    val config: HttpErrorConfig = TestHttpErrorHandler.simpleErrorConfig.copy(showDevErrors = showDevErrors)
    lazy val handler: TestHttpErrorHandler = new TestHttpErrorHandler(router, config)
    val testRequest: FakeRequest[AnyContentAsEmpty.type] = {
      FakeRequest("GET", "/?test=true", FakeHeaders(Seq("test" -> "true")), AnyContentAsEmpty)
    }
  }

  private val it = classOf[PlayJsonHttpErrorHandling].getSimpleName

  def testHttpErrorHandler(whenShowDevErrors: Boolean): Unit = {
    val showDevErrorIsSet = s"showDevErrors = $whenShowDevErrors"

    s"$it should return json for 400 when $showDevErrorIsSet" in {
      val fixture = new FixtureParam(Router.empty, whenShowDevErrors)
      import fixture._
      val expectedMessage = "test exception"
      val expectedJson = Json.obj(
        "url" -> testRequest.toString(),
        "error" -> expectedMessage
      )
      for {
        result <- handler.onClientError(testRequest, Status.BAD_REQUEST, expectedMessage)
        body <- contentAsJson(result)
      } yield {
        assertResult(Status.BAD_REQUEST)(result.header.status)
        assertResult(expectedJson)(body)
      }
    }

    s"$it should return json for 403 when $showDevErrorIsSet" in {
      val fixture = new FixtureParam(Router.empty, whenShowDevErrors)
      import fixture._
      val expectedMessage = "test exception"
      val expectedJson = Json.obj(
        "url" -> testRequest.toString(),
        "error" -> expectedMessage
      )
      for {
        result <- handler.onClientError(testRequest, Status.FORBIDDEN, expectedMessage)
        body <- contentAsJson(result)
      } yield {
        assertResult(Status.FORBIDDEN)(result.header.status)
        assertResult(expectedJson)(body)
      }
    }

    s"$it should return json for 404 when $showDevErrorIsSet" in {
      val fixture = new FixtureParam(Router.empty, whenShowDevErrors)
      import fixture._
      val expectedMessage = "test exception"
      for {
        result <- handler.onClientError(testRequest, Status.NOT_FOUND, expectedMessage)
        maybeBody <- {
          if (config.showDevErrors) contentAsJson(result).map(Some(_))
          else Future.successful(None)
        }
      } yield {
        for (body <- maybeBody) {
          val expectedJson = Json.obj(
            "url" -> testRequest.toString(),
            "error" -> s"Unmatched URL: $expectedMessage",
            "routes" -> Json.obj()
          )
          assertResult(expectedJson)(body)
        }
        assertResult(Status.NOT_FOUND)(result.header.status)
      }
    }

    s"$it should return a list of routes for 404 when $showDevErrorIsSet" in {
      val fixture = new FixtureParam(DocumentedRouter.fromRegexMap(
        ("GET", "/unmatched/1".r, "test 1 documentation") -> Action(Results.Ok),
        ("GET", "/unmatched/2".r, "test 2 documentation") -> Action(Results.Ok)
      ), whenShowDevErrors)
      import fixture._
      val expectedMessage = "test exception"
      for {
        result <- handler.onClientError(testRequest, Status.NOT_FOUND, expectedMessage)
        maybeBody <- {
          if (config.showDevErrors) contentAsJson(result).map(Some(_))
          else Future.successful(None)
        }
      } yield {
        for (body <- maybeBody) {
          val expectedRouteMap = router.documentation.map {
            case (method, pattern, docs) => (s"$method $pattern", docs)
          }.toMap
          val expectedJson = Json.obj(
            "url" -> testRequest.toString(),
            "error" -> s"Unmatched URL: $expectedMessage",
            "routes" -> expectedRouteMap
          )
          assertResult(expectedJson)(body)
        }
        assertResult(Status.NOT_FOUND)(result.header.status)
      }
    }

    s"$it should return json for 500 when $showDevErrorIsSet" in {
      val fixture = new FixtureParam(Router.empty, whenShowDevErrors)
      import fixture._
      val nestedException = new RuntimeException("test nested exception")
      val unwrappedException = new RuntimeException("test exception", nestedException)
      val expectedException = HttpErrorHandlerExceptions
        .throwableToUsefulException(None, !config.showDevErrors, unwrappedException)
      for {
        result <- handler.onServerError(testRequest, expectedException)
        body <- contentAsJson(result)
      } yield {
        assertResult(Status.INTERNAL_SERVER_ERROR)(result.header.status)
        val traceLength = (body \ "trace").toOption.flatMap(_.asOpt[Seq[String]]).map(_.size)
        assert(config.showDevErrors || traceLength.isEmpty)
        val traceCauseMessage = (body \ "traceCause" \ "message").as[String]
        assertResult(unwrappedException.getMessage)(traceCauseMessage)
        val traceCauseLength = (body \ "traceCause" \ "trace").toOption.flatMap(_.asOpt[Seq[String]]).map(_.size)
        assert(config.showDevErrors || traceCauseLength.isEmpty)
        val nestedTraceCauseMessage = (body \ "traceCause" \ "traceCause" \ "message").as[String]
        assertResult(nestedException.getMessage)(nestedTraceCauseMessage)
        val nestedTraceCauseLength = (body \ "traceCause" \ "traceCause" \ "trace").toOption
          .flatMap(_.asOpt[Seq[String]]).map(_.size)
        assert(config.showDevErrors || nestedTraceCauseLength.isEmpty)
        assertResult("Uncaught Exception") {
          (body \ "error").as[String]
        }
        assertResult(expectedException.getMessage) {
          (body \ "message").as[String]
        }
      }
    }
  }

  testHttpErrorHandler(true)
  testHttpErrorHandler(false)
}

class TestHttpErrorHandler(
  override protected val router: Router,
  override protected val config: HttpErrorConfig = TestHttpErrorHandler.simpleErrorConfig,
  sourceMapper: Option[SourceMapper] = None
) extends DefaultHttpErrorHandler(config, sourceMapper, Some(router))
  with PlayJsonHttpErrorHandling

object TestHttpErrorHandler {
  val simpleErrorConfig: HttpErrorConfig = HttpErrorConfig(showDevErrors = false, None)
}

class DocumentedRouter(
  override val routes: Routes,
  override val documentation: Seq[(String, String, String)]
) extends Router {
  self =>

  override def withPrefix(prefix: String): DocumentedRouter = {
    val p = if (prefix.endsWith("/")) prefix else s"$prefix/"
    val prefixStringOp: PartialFunction[String, String] = {
      case path if path startsWith p => path.drop(p.length - 1)
    }
    val prefixRequestHeader: PartialFunction[RequestHeader, RequestHeader] = {
      case r if prefixStringOp isDefinedAt r.path => r.copy(path = prefixStringOp(r.path))
    }
    val prefixedRoutes = Function.unlift(prefixRequestHeader.lift.andThen(_.flatMap(self.routes.lift)))
    val prefixedDocumentation = documentation.map {
      case (method, path, controllerMethod) if prefixStringOp isDefinedAt path =>
        (method, prefixStringOp(path), controllerMethod)
    }
    new DocumentedRouter(prefixedRoutes, prefixedDocumentation) {
      override def withPrefix(prefix: String): DocumentedRouter = {
        self.withPrefix(prefix)
      }
    }

  }
}

object DocumentedRouter {

  def fromRegexMap(entries: ((String, Regex, String), Handler)*): DocumentedRouter = {
    val router = (header: RequestHeader) => {
      entries.collectFirst {
        case ((method, re, _), handler) if method == header.method && re.findFirstMatchIn(header.uri).isDefined =>
          handler
      }
    }
    val documentation = entries.map {
      case ((method, re, docs), _) => (method, re.pattern.toString, docs)
    }
    new DocumentedRouter(Function.unlift(router), documentation)
  }
}
