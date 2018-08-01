package com.rallyhealth.playmodule.jsonerrors.guice

import com.google.inject.{AbstractModule, Inject, Provider}
import com.rallyhealth.playmodule.jsonerrors.PlayJsonHttpErrorHandler
import play.api.http.{HttpErrorConfig, HttpErrorHandler}
import play.api.routing.Router
import play.api.{Configuration, Environment, Mode, OptionalSourceMapper}

/**
  * A guice module for configuring the [[HttpErrorConfig]] to [[PlayJsonHttpErrorHandler]].
  */
class PlayJsonHttpErrorHandlerModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[HttpErrorHandler]).toProvider(classOf[PlayJsonHttpErrorHandlerProvider])
  }
}

/**
  * A customized provider that handles the intricacies of guice bindings for lazy [[Router]] and
  * other internal Play types.
  */
class PlayJsonHttpErrorHandlerProvider @Inject() (
  environment: Environment,
  configuration: Configuration,
  router: Provider[Router],
  sourceMapper: OptionalSourceMapper
) extends Provider[PlayJsonHttpErrorHandler] {
  private lazy val instance = new PlayJsonHttpErrorHandler(
    router.get(),
    HttpErrorConfig(environment.mode != Mode.Prod, configuration.getString("play.editor")),
    sourceMapper.sourceMapper
  )
  override def get(): PlayJsonHttpErrorHandler = instance
}
