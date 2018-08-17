package com.rallyhealth.playmodule.jsonerrors

import play.api.UsefulException
import play.api.http.{DefaultHttpErrorHandler, Status, Writeable}
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result, Results}
import play.api.routing.Router

import scala.concurrent.Future

/**
  * Overrides the [[DefaultHttpErrorHandler]] with JSON formatted error messages instead of HTML pages.
  *
  * This makes the errors smaller and cleaner when this service is called by other services and not a web browser.
  * It also avoids the issue of JSON parsing errors on the client.
  *
  * This is generic to the type of Json library you like to use. If you want to use Play Json, see
  * [[PlayJsonHttpErrorHandling]].
  */
trait JsonHttpErrorHandling[Json] extends DefaultHttpErrorHandler {

  protected def config: JsonHttpErrorConfig
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
    if (config.showRoutes) {
      val status = Status.NOT_FOUND
      val error = Left(message)
      val body = jsonify(request, status, error)
      result(request, status, error, body)
    } else {
      Future.successful(NotFound)
    }
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
