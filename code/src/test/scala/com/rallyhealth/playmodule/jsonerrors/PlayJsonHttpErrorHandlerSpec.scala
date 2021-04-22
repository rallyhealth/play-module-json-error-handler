package com.rallyhealth.playmodule.jsonerrors

import akka.util.ByteString
import org.scalatest.freespec.AsyncFreeSpec
import play.api.http._
import play.api.libs.json.{JsObject, Json}
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.routing.Router
import play.api.routing.Router.Routes
import play.api.test.ops.AsyncResultExtractors
import play.api.test.{FakeHeaders, FakeRequest}

import scala.concurrent.Future
import scala.util.matching.Regex

class PlayJsonHttpErrorHandlerSpec extends AsyncFreeSpec with AsyncResultExtractors {

  class FixtureParam(val router: Router, showDevErrors: Boolean, showRoutes: Boolean) {
    val config: JsonHttpErrorConfig = JsonHttpErrorConfig(showDevErrors, showRoutes, None)
    lazy val handler: HttpErrorHandler = new PlayJsonHttpErrorHandler(router, config, None)
    val testRequest: FakeRequest[AnyContentAsEmpty.type] = {
      FakeRequest("GET", "/?test=true", FakeHeaders(Seq("test" -> "true")), AnyContentAsEmpty)
    }
  }

  private val it = classOf[PlayJsonHttpErrorHandling].getSimpleName

  def testHttpErrorHandler(whenShowDevErrors: Boolean, whenShowRoutes: Boolean): Unit = {
    val showDevErrorIsSet = s"showDevErrors = $whenShowDevErrors"
    val showRoutesIsSet = s"showRoutes = $whenShowRoutes"

    def newFixture(router: Router = Router.empty): FixtureParam = {
      new FixtureParam(Router.empty, whenShowDevErrors, whenShowRoutes)
    }

    s"$it should return json for 400 when $showDevErrorIsSet and $showRoutesIsSet" in {
      val fixture = newFixture()
      import fixture._
      val expectedMessage = "test exception"
      val expectedJson = Json.obj(
        "url" -> testRequest.toString(),
        "error" -> PlayJsonHttpErrorHandling.CLIENT_ERROR,
        "message" -> expectedMessage
      )
      for {
        result <- handler.onClientError(testRequest, Status.BAD_REQUEST, expectedMessage)
        body <- contentAsJson(result)
      } yield {
        assertResult(Status.BAD_REQUEST)(result.header.status)
        assertResult(expectedJson)(body)
      }
    }

    s"$it should return json for 403 when $showDevErrorIsSet and $showRoutesIsSet" in {
      val fixture = newFixture()
      import fixture._
      val expectedMessage = "test exception"
      val expectedJson = Json.obj(
        "url" -> testRequest.toString(),
        "error" -> PlayJsonHttpErrorHandling.CLIENT_ERROR,
        "message" -> expectedMessage
      )
      for {
        result <- handler.onClientError(testRequest, Status.FORBIDDEN, expectedMessage)
        body <- contentAsJson(result)
      } yield {
        assertResult(Status.FORBIDDEN)(result.header.status)
        assertResult(expectedJson)(body)
      }
    }

    s"$it should return json for 404 when $showDevErrorIsSet and $showRoutesIsSet" in {
      val fixture = newFixture()
      import fixture._
      val expectedMessage = "test exception"
      for {
        result <- handler.onClientError(testRequest, Status.NOT_FOUND, expectedMessage)
        maybeBody <- {
          if (config.showDevErrors && config.showRoutes) contentAsJson(result).map(Some(_))
          else Future.successful(None)
        }
      } yield {
        for (body <- maybeBody) {
          val expectedJson = Json.obj(
            "url" -> testRequest.toString(),
            "error" -> PlayJsonHttpErrorHandling.CLIENT_ERROR,
            "message" -> s"Unmatched URL: $expectedMessage",
            "routes" -> Json.obj()
          )
          assertResult(expectedJson)(body)
        }
        assertResult(Status.NOT_FOUND)(result.header.status)
      }
    }

    s"$it should return a list of routes for 404 when $showDevErrorIsSet and $showRoutesIsSet" in {
      val fixture = newFixture(DocumentedRouter.fromRegexMap(
        ("GET", "/unmatched/1".r, "test 1 documentation") -> PlayAction(Results.Ok),
        ("GET", "/unmatched/2".r, "test 2 documentation") -> PlayAction(Results.Ok)
      ))
      import fixture._
      val expectedMessage = "test exception"
      for {
        result <- handler.onClientError(testRequest, Status.NOT_FOUND, expectedMessage)
        maybeBody <- {
          if (config.showRoutes) contentAsJson(result).map(Some(_))
          else Future.successful(None)
        }
      } yield {
        for (body <- maybeBody) {
          val expectedRouteMap = router.documentation.map {
            case (method, pattern, docs) => (s"$method $pattern", docs)
          }.toMap
          val expectedJson = Json.obj(
            "url" -> testRequest.toString(),
            "error" -> PlayJsonHttpErrorHandling.CLIENT_ERROR,
            "message" -> s"Unmatched URL: $expectedMessage",
            "routes" -> expectedRouteMap
          )
          assertResult(expectedJson)(body)
        }
        assertResult(Status.NOT_FOUND)(result.header.status)
      }
    }

    s"$it should return json for 500 with the correct format when $showDevErrorIsSet and $showRoutesIsSet" in {
      val fixture = newFixture()
      import fixture._

      /* Setup the following exception hierarchy:
       *
       * usefulException:
       * - suppressed:
       *   * suppressedException1
       * - cause:
       *   * unwrappedException
       *     - suppressed:
       *       * suppressedException2
       *     - cause:
       *       * nestedException
       *         - suppressed: null
       *         - cause: null
       */
      val nestedException = new RuntimeException("test nested exception")
      val unwrappedException = new RuntimeException("test exception", nestedException)
      val suppressedException1 = new RuntimeException("test suppressed exception 1")
      unwrappedException.addSuppressed(suppressedException1)
      val usefulException = HttpErrorHandlerExceptions
        .throwableToUsefulException(None, !config.showDevErrors, unwrappedException)
      val suppressedException2 = new RuntimeException("test suppressed exception 2")
      usefulException.addSuppressed(suppressedException2)

      for {
        result <- handler.onServerError(testRequest, usefulException)
        body <- contentAsJson(result)
      } yield {
        assertResult(Status.INTERNAL_SERVER_ERROR)(result.header.status)

        // Test the usefulException at the root
        val error = (body \ "error").as[String]
        assertResult(PlayJsonHttpErrorHandling.UNCAUGHT_EXCEPTION)(error)
        val usefulExceptionMessage = (body \ "message").as[String]
        assertResult(usefulException.getMessage)(usefulExceptionMessage)
        // Test that the root exception supports suppressed exceptions
        val usefulExceptionSuppressedLength = (body \ "suppressed").toOption
          .flatMap(_.asOpt[Seq[JsObject]]).map(_.size)
        // There should always be a suppressed exception regardless of showDevErrors
        assert(usefulExceptionSuppressedLength contains 1)
        val traceLength = (body \ "trace").toOption.flatMap(_.asOpt[Seq[String]]).map(_.size)
        // We don't care about the trace length as long as it is absent on prod and some positive number on dev
        assert(config.showDevErrors && traceLength.exists(_ > 0) || traceLength.isEmpty)

        // Test the unwrappedException that caused the usefulException
        val unwrappedExceptionMessage = (body \ "traceCause" \ "message").as[String]
        assertResult(unwrappedException.getMessage)(unwrappedExceptionMessage)
        // Test that a nested exception supports suppressed exceptions
        val unwrappedExceptionSuppressedLength = (body \ "traceCause" \ "suppressed").toOption
          .flatMap(_.asOpt[Seq[JsObject]]).map(_.size)
        // There should always be a suppressed exception regardless of showDevErrors
        assert(unwrappedExceptionSuppressedLength contains 1)
        val unwrappedTraceLength = (body \ "traceCause" \ "trace").toOption.flatMap(_.asOpt[Seq[String]]).map(_.size)
        // We don't care about the trace length as long as it is absent on prod and some positive number on dev
        assert(config.showDevErrors && unwrappedTraceLength.exists(_ > 0) || unwrappedTraceLength.isEmpty)

        // Test the nestedException that caused the unwrappedException
        val nestedExceptionMessage = (body \ "traceCause" \ "traceCause" \ "message").as[String]
        assertResult(nestedException.getMessage)(nestedExceptionMessage)
        val nestedExceptionSuppressedLength = (body \ "traceCause" \ "traceCause" \ "suppressed").toOption
          .flatMap(_.asOpt[Seq[JsObject]]).map(_.size)
        // We only added a suppressed exception to the first unwrapped and nested exception, but not its cause
        assert(nestedExceptionSuppressedLength.isEmpty)
        val nestedExceptionTraceLength = (body \ "traceCause" \ "traceCause" \ "trace").toOption
          .flatMap(_.asOpt[Seq[String]]).map(_.size)
        // We don't care about the trace length as long as it is absent on prod and some positive number on dev
        assert(config.showDevErrors && nestedExceptionTraceLength.exists(_ > 0) || nestedExceptionTraceLength.isEmpty)
      }
    }
  }

  testHttpErrorHandler(true, true)
  testHttpErrorHandler(true, false)
  testHttpErrorHandler(false, true)
  testHttpErrorHandler(false, false)
}

class DocumentedRouter(
  override val routes: Routes,
  override val documentation: Seq[(String, String, String)]
) extends Router {
  self =>
  import DocumentedRouter._

  override def withPrefix(prefix: String): DocumentedRouter = {
    val p = if (prefix.endsWith("/")) prefix else s"$prefix/"
    val prefixStringOp: PartialFunction[String, String] = {
      case path if path startsWith p => path.drop(p.length - 1)
    }
    val prefixRequestHeader: PartialFunction[RequestHeader, RequestHeader] = {
      case r if prefixStringOp isDefinedAt r.path =>
        r.withTarget(r.target.withPath(prefixStringOp(r.path)))
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

  /**
    * This shim gives the Play 2.5 code the same methods that are available in Play 2.6 and Play 2.7
    *
    * If the method exists on the class at compile-time, then it will use that, however, if not, the compiler
    * will find this implicit class and apply the appropriate adapters.
    */
  private implicit class NewRequestHeaderShim(val rh: RequestHeader) extends AnyVal {

    /**
      * This adds the `.target` method to [[RequestHeader]] that is available in versions of Play 2.6 and above.
      *
      * @note this does not return the RequestTarget class, because this class is not available in Play 2.5, but we
      *       don't need it because the method that accepts that parameter is also not available in Play 2.5, so the
      *       `.withTarget` method we define below as a shim doesn't need to accept it.
      */
    def target: RequestTargetShim = new RequestTargetShim(rh.path)

    /**
      * Takes the value returned from [[target]] (in Play 2.5) and copies the path.
      */
    def withTarget(target: RequestTargetShim): RequestHeader = {
      rh.copy(path = target.path)
    }
  }

  // This is never used. It only exists to make Play 2.6 and above code compile with the Play 2.5 shims above
  private implicit class UnusedRequestHeaderShim(rh: RequestHeader) {
    def copy(path: String): RequestHeader = {
      throw new UnsupportedOperationException("UnusedRequestHeaderShim.copy should never be called")
    }
  }

  /**
    * This shim gives the Play 2.5 a version of RequestTarget that supports the minimal version of methods
    * needed to be compatible with the source code for Play 2.6 and above.
    *
    * If the method exists on the class at compile-time, then it will use that, however, if not, the compiler
    * will find this implicit class and apply the appropriate adapters.
    */
  private class RequestTargetShim(val path: String) extends AnyVal {

    /**
      * Return a new RequestTargetShim with the updated path.
      */
    def withPath(path: String): RequestTargetShim = new RequestTargetShim(path)
  }

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

// Used for source compatibility with Play 2.8 and earlier versions that used Action.apply()
object PlayAction {
  def apply(result: => Result): EssentialAction = EssentialAction(_ => Accumulator.done(result))
}
