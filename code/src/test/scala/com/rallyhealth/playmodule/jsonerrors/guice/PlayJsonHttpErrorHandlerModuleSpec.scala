package com.rallyhealth.playmodule.jsonerrors.guice

import com.google.inject.AbstractModule
import com.google.inject.util.Modules
import com.rallyhealth.playmodule.jsonerrors.{JsonHttpErrorConfig, PlayJsonHttpErrorHandler}
import org.scalatest.freespec.AnyFreeSpec
import play.api.http.HttpErrorHandler
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}

class PlayJsonHttpErrorHandlerModuleSpec extends AnyFreeSpec {

  private val it = classOf[PlayJsonHttpErrorHandlerModule].getSimpleName

  s"$it should inject the correct error handler when configured via overrides" in {
    val app = GuiceApplicationBuilder(
      overrides = Seq(GuiceableModule.guiceable(new PlayJsonHttpErrorHandlerModule))
    ).build()
    val handler = app.injector.instanceOf(classOf[HttpErrorHandler])
    assert(handler.isInstanceOf[PlayJsonHttpErrorHandler])
  }

  s"$it should inject the correct json error config when using a custom module" in {
    val expected = JsonHttpErrorConfig(showDevErrors = false, showRoutes = true, None)
    val app = GuiceApplicationBuilder(
      overrides = Seq(
        Modules
          .`override`(new PlayJsonHttpErrorHandlerModule)
          .`with`(new TestOverrideJsonHttpErrorConfigModule(expected))
      )
    ).build()
    val config = app.injector.instanceOf(classOf[JsonHttpErrorConfig])
    assertResult(expected)(config)
  }
}

class TestOverrideJsonHttpErrorConfigModule(
  overrideConfig: JsonHttpErrorConfig
) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[JsonHttpErrorConfig]).toInstance(overrideConfig)
  }
}
