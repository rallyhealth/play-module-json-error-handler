package com.rallyhealth.playmodule.jsonerrors

import play.api.UsefulException
import play.api.http.{DefaultHttpErrorHandler, HttpErrorConfig, Writeable}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.Results._
import play.api.mvc.{Codec, RequestHeader, Result, Results}
import play.api.http.Status
import play.api.routing.Router

import scala.concurrent.Future

/**
  * Overrides the [[DefaultHttpErrorHandler]] with JSON formatted error messages instead of HTML pages.
  *
  * This makes the errors smaller and cleaner when this service is called by other services and not a web browser.
  * It also avoids the issue of JSON parsing errors on the client.
  */
trait JsonHttpErrorHandling[Json] extends DefaultHttpErrorHandler {

  protected def config: HttpErrorConfig
  protected def router: Router
  implicit protected def writeable: Writeable[Json]

  protected def jsonify(
    request: RequestHeader,
    responseStatus: Int,
    error: Either[String, UsefulException]): Json

  protected def result(
    request: RequestHeader,
    initialStatus: Int,
    error: Either[String, UsefulException],
    body: Json): Future[Result] = {
    Future.successful(Results.Status(initialStatus)(body))
  }

  override protected def onForbidden(request: RequestHeader, message: String): Future[Result] = {
    if (config.showDevErrors) onDevForbidden(request, message)
    else onProdForbidden(request, message)
  }

  protected def onDevForbidden(request: RequestHeader, message: String): Future[Result] = {
    val status = Status.FORBIDDEN
    val error = Left(message)
    val body = jsonify(request, status, error)
    result(request, status, error, body)
  }

  protected def onProdForbidden(request: RequestHeader, message: String): Future[Result] = {
    val status = Status.FORBIDDEN
    val error = Left(message)
    val body = jsonify(request, status, error)
    result(request, status, error, body)
  }

  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    if (config.showDevErrors) onDevNotFound(request, message)
    else onProdNotFound(request, message)
  }

  protected def onDevNotFound(request: RequestHeader, message: String): Future[Result] = {
    val status = Status.NOT_FOUND
    val error = Left(message)
    val body = jsonify(request, status, error)
    result(request, status, error, body)
  }

  protected def onProdNotFound(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(NotFound)
  }

  override protected def onBadRequest(request: RequestHeader, message: String): Future[Result] = {
    val status = Status.BAD_REQUEST
    val error = Left(message)
    val body = jsonify(request, status, error)
    result(request, status, error, body)
  }

  override protected def onOtherClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    val error = Left(message)
    val body = jsonify(request, statusCode, error)
    result(request, statusCode, error, body)
  }

  override protected def onDevServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    val status = Status.INTERNAL_SERVER_ERROR
    val error = Right(exception)
    val body = jsonify(request, status, error)
    result(request, status, error, body)
  }

  override protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    val status = Status.INTERNAL_SERVER_ERROR
    val error = Right(exception)
    val body = jsonify(request, status, error)
    result(request, status, error, body)
  }
}

trait PlayJsonHttpErrorHandling extends JsonHttpErrorHandling[JsValue] {

  protected def exceptionJsonWriter: OWrites[Throwable] = {
    if (config.showDevErrors)
      PlayJsonHttpErrorHandling.writesThrowableWithStack
    else
      PlayJsonHttpErrorHandling.writesThrowableWithoutStack
  }

  protected implicit def codec: Codec = Codec.utf_8

  override implicit protected lazy val writeable: Writeable[JsValue] = Writeable.writeableOf_JsValue

  override protected def jsonify(
    request: RequestHeader,
    responseStatus: Int,
    error: Either[String, UsefulException]
  ): JsValue = {
    var fields: Map[String, JsValue] = Map(
      "url" -> JsString(request.toString)
    )
    error match {
      case Left(message) if Status.NOT_FOUND == responseStatus =>
        val fullError = s"Unmatched URL${if (message.isEmpty) "" else s": $message"}"
        fields += "error" -> JsString(fullError)
        if (config.showDevErrors) {
          fields += "routes" -> JsObject(router.documentation.map {
            case (method, pathPattern, controllerMethod) =>
              (s"$method $pathPattern", JsString(controllerMethod))
          })
        }
      case Left(message) =>
        fields += "error" -> JsString(message)
      case Right(exception) =>
        fields ++= Map(
          "error" -> JsString("Uncaught Exception")
        ) ++ exceptionJsonWriter.writes(exception).fields
    }
    JsObject(fields)
  }
}

object PlayJsonHttpErrorHandling {

  lazy val writesThrowableWithoutStack: OWrites[Throwable] = {
    val writer = (
      (__ \ "message").write[String] and
      (__ \ "traceCause").lazyWriteNullable[Throwable](writesThrowableWithoutStack) // recursion
    ).tupled
    OWrites { ex =>
      writer.writes((ex.getMessage, Option(ex.getCause)))
    }
  }

  lazy val writesThrowableWithStack: OWrites[Throwable] = {
    val writer = (
      (__ \ "message").write[String] and
      (__ \ "trace").write[Array[String]] and
      (__ \ "traceCause").lazyWriteNullable[Throwable](writesThrowableWithStack) // recursion
    ).tupled
    OWrites { ex =>
      writer.writes((ex.getMessage, ex.getStackTrace.map(_.toString), Option(ex.getCause)))
    }
  }
}
