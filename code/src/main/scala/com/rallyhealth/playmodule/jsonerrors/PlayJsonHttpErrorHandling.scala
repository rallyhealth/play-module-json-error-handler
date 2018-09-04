package com.rallyhealth.playmodule.jsonerrors

import play.api.UsefulException
import play.api.http.{Status, Writeable}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{Codec, RequestHeader}

/**
  * Adds [[JsonHttpErrorHandling]] using Play Json.
  *
  * `error` = "Client Error" or "Uncaught Exception" for an exception that is thrown inside an action
  *           and not caught.
  * `message` = The message from exception thrown by Netty or the action
  *
  * Formats 404 errors in the following shape:
  * <code>
  *   {
  *     "url": "GET /example",
  *     "error": "Client Error",
  *     "message": "Unmatched URL: GET /example",
  *     "routes": {
  *       "POST /example": "An example action that only accepts POST requests",
  *       ... lists all unmatched routes if config.showDevErrors = true ...
  *     }
  *   }
  * </code>
  *
  * Formats all other client errors:
  * <code>
  *   {
  *     "url": "GET /example",
  *     "error": "Client Error",
  *     "message": "Forbidden"
  *   }
  * </code>
  *
  * Formats exceptions in a recursive manner:
  * <code>
  *   {
  *     "url": "GET /example",
  *     "error": "Uncaught Exception",
  *     "message": "UsefulException: Exception message",
  *     "suppressed": [
  *       {
  *         "message": "RuntimeException: Suppressed exception message",
  *         "traceCause": { /* ...recursion into suppressed exception... */ },
  *         "trace": [ /* stacktrace of suppressed exception */ ]
  *       }
  *     ],
  *     "traceCause": {
  *       "message": "RuntimeException: Underlying exception cause message",
  *       "traceCause": { /* ...recursion into exceptions... */ },
  *       "trace": [ /* stacktrace of cause exception */ ]
  *     },
  *     "trace": [ /* stacktrace of original exception */ ]
  *   }
  * </code>
  *
  * @note some fields are redundant with the request headers but are included to
  *       show how the original request was or was not modified.
  */
trait PlayJsonHttpErrorHandling extends JsonHttpErrorHandling[JsValue] {
  import PlayJsonHttpErrorHandling._

  protected def exceptionJsonWriter: OWrites[Throwable] = {
    if (config.showDevErrors) writesThrowableWithStack
    else writesThrowableWithoutStack
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
      case Left(message) =>
        fields += "error" -> JsString(CLIENT_ERROR)
        responseStatus match {
          case Status.NOT_FOUND =>
            val fullError = s"Unmatched URL${if (message.isEmpty) "" else s": $message"}"
            fields += "message" -> JsString(fullError)
            if (config.showRoutes) {
              fields += "routes" -> JsObject(router.documentation.map {
                case (method, pathPattern, controllerMethod) =>
                  (s"$method $pathPattern", JsString(controllerMethod))
              })
            }
          case _ =>
            fields += "message" -> JsString(message)
        }
      case Right(exception) =>
        fields += "error" -> JsString(UNCAUGHT_EXCEPTION)
        if (!exception.getSuppressed.isEmpty) {
          fields += "suppressed" -> JsArray(exception.getSuppressed.map(exceptionJsonWriter.writes))
        }
        fields ++= exceptionJsonWriter.writes(exception).fields
    }
    JsObject(fields)
  }
}

object PlayJsonHttpErrorHandling {

  final val CLIENT_ERROR = "Client Error"
  final val UNCAUGHT_EXCEPTION = "Uncaught Exception"

  /**
    * Recursively writes a [[Throwable]] with its cause, but without a stack trace.
    */
  lazy val writesThrowableWithoutStack: OWrites[Throwable] = {
    val writer = (
      (__ \ "message").write[String] and
      (__ \ "suppressed").lazyWriteNullable[Seq[Throwable]](Writes.seq[Throwable](writesThrowableWithoutStack)) and
      (__ \ "traceCause").lazyWriteNullable[Throwable](writesThrowableWithoutStack)
    ).tupled
    OWrites { ex =>
      writer.writes((
        ex.getMessage,
        Option(ex.getSuppressed.toSeq).filterNot(_.isEmpty),
        Option(ex.getCause)
      ))
    }
  }

  /**
    * Recursively writes a [[Throwable]] with its cause and stack trace.
    */
  lazy val writesThrowableWithStack: OWrites[Throwable] = {
    val writer = (
      (__ \ "message").write[String] and
      (__ \ "suppressed").lazyWriteNullable[Seq[Throwable]](Writes.seq[Throwable](writesThrowableWithStack)) and
      (__ \ "trace").write[Array[String]] and
      (__ \ "traceCause").lazyWriteNullable[Throwable](writesThrowableWithStack)
    ).tupled
    OWrites { ex =>
      writer.writes((
        ex.getMessage,
        Option(ex.getSuppressed.toSeq).filterNot(_.isEmpty),
        ex.getStackTrace.map(_.toString),
        Option(ex.getCause)
      ))
    }
  }
}
