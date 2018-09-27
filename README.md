[![Build Status](https://travis-ci.org/rallyhealth/play-module-json-error-handler.svg?branch=master)](https://travis-ci.org/rallyhealth/play-module-json-error-handler)
[![codecov](https://codecov.io/gh/rallyhealth/play-module-json-error-handler/branch/master/graph/badge.svg)](https://codecov.io/gh/rallyhealth/play-module-json-error-handler)

| play25-module-json-error-handler | play26-module-json-error-handler |
| -------------------------------- | -------------------------------- |
| [ ![Download](https://api.bintray.com/packages/rallyhealth/maven/play25-module-json-error-handler/images/download.svg) ](https://bintray.com/rallyhealth/maven/play25-module-json-error-handler/_latestVersion) | [ ![Download](https://api.bintray.com/packages/rallyhealth/maven/play26-module-json-error-handler/images/download.svg) ](https://bintray.com/rallyhealth/maven/play26-module-json-error-handler/_latestVersion) |

# Summary

A Play plugin that displays exceptions as JSON instead of HTML. 

Add it to your build with Guice by adding the following config:
```hocon
play.modules.enabled += "com.rallyhealth.playmodule.jsonerrors.guice.PlayJsonHttpErrorHandlerModule"
play.http.errorHandler = "com.rallyhealth.playmodule.jsonerrors.PlayJsonHttpErrorHandler"
```

Or for compile-time dependency injection, just override the `httpErrorHandler` component:

```scala
import com.rallyhealth.playmodule.jsonerrors.PlayJsonHttpErrorHandler
import play.api.{ApplicationLoader, BuiltInComponentsFromContext}
import play.api.http.HttpErrorConfig

class MyModuleFromContext(context: ApplicationLoader.Context) extends BuiltInComponentsFromContext(context) {

  lazy val httpErrorConfig: JsonHttpErrorConfig = JsonHttpErrorConfig.fromEnvironment(environment)

  override lazy val httpErrorHandler: HttpErrorHandler = new PlayJsonHttpErrorHandler(
    router,
    httpErrorConfig,
    sourceMapper
  )

  // ... other required components
}
```

# Customization

You can create your own custom error handling with a mixin by extending `JsonHttpErrorHandling[Json]` or as a class
by extending `DefaultJsonHttpErrorHandler(...) with JsonHttpErrorHandling[Json]`. The trait is parameterized on the
type of json library you would like to use.

For a custom json library, all you need to do is implement:
```scala
import com.rallyhealth.playmodule.jsonerrors.JsonHttpErrorHandling
import play.api.UsefulException
import play.api.http.Writeable
import play.api.mvc.RequestHeader

trait MyJsonHttpErrorHandling extends JsonHttpErrorHandling[MyJson] {
  override implicit protected def writeable: Writeable[MyJson] = {
    // TODO: Provide a converter for MyJson into a ByteString with the appropriate content-type 
    ???  
  }
  override protected def jsonify(
    request: RequestHeader,
    responseStatus: Int,
    error: Either[String, UsefulException]): MyJson = {
    // TODO: Serialize these values into a json response
    ???    
  }
}
```

If you want to use Play's json library, you can just extend `PlayJsonHttpErrorHandling` or the
`PlayJsonHttpErrorHandler` class (depending on whether you want to mixin other behavior). You can override the way
exceptions are serialized, as well as transform or replace the shape of the json with: 

```scala
import com.rallyhealth.playmodule.jsonerrors.PlayJsonHttpErrorHandling
import play.api.UsefulException
import play.api.http.Writeable
import play.api.mvc.RequestHeader

trait MyJsonHttpErrorHandling extends PlayJsonHttpErrorHandling {
  override protected def exceptionJsonWriter: OWrites[Throwable] = {
    // TODO: Provide Writes for exception to Json (potentially recursively on causes / suppressed exceptions)
    ???
  } 
  override protected def jsonify(
    request: RequestHeader,
    responseStatus: Int,
    error: Either[String, UsefulException]): JsValue = {
    // TODO: Transform the json result
    super.jsonify(request, responseStatus, error)
  }
}
```
