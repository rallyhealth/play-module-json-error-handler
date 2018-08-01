package com.rallyhealth.playmodule.jsonerrors.guice

import com.rallyhealth.playmodule.jsonerrors.PlayJsonHttpErrorHandler
import org.scalatest.FreeSpec
import play.api.http.HttpErrorHandler
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}

class PlayJsonHttpErrorHandlerModuleSpec extends FreeSpec {

  private val it = classOf[PlayJsonHttpErrorHandlerModule].getSimpleName

  s"$it should inject the correct error handler when configured via overrides" in {
    val app = GuiceApplicationBuilder(
      overrides = Seq(GuiceableModule.guiceable(new PlayJsonHttpErrorHandlerModule))
    ).build()
    val handler = app.injector.instanceOf(classOf[HttpErrorHandler])
    assert(handler.isInstanceOf[PlayJsonHttpErrorHandler])
  }
}
