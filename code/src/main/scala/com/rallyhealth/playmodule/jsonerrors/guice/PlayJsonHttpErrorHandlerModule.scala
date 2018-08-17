package com.rallyhealth.playmodule.jsonerrors.guice

import com.google.inject.{AbstractModule, Inject, Provider}
import com.rallyhealth.playmodule.jsonerrors.{JsonHttpErrorConfig, PlayJsonHttpErrorHandler}
import play.api.http.HttpErrorHandler
import play.api.routing.Router
import play.api.{Configuration, Environment, OptionalSourceMapper}

/**
  * A guice module for configuring the [[JsonHttpErrorConfig]] to [[PlayJsonHttpErrorHandler]].
  */
class PlayJsonHttpErrorHandlerModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[JsonHttpErrorConfig]).toProvider(classOf[JsonHttpErrorConfigProvider])
    bind(classOf[HttpErrorHandler]).toProvider(classOf[PlayJsonHttpErrorHandlerProvider])
  }
}

/**
  * Provides a [[JsonHttpErrorConfig]] based on the bound environment and configuration.
  */
class JsonHttpErrorConfigProvider @Inject() (
  environment: Environment,
  configuration: Configuration
) extends Provider[JsonHttpErrorConfig] {
  private lazy val instance = JsonHttpErrorConfig.fromEnvironment(environment, configuration)
  override def get(): JsonHttpErrorConfig = instance
}

/**
  * A customized provider that handles the intricacies of Guice bindings for lazy [[Router]] and
  * other internal Play types.
  */
class PlayJsonHttpErrorHandlerProvider @Inject() (
  config: JsonHttpErrorConfig,
  router: Provider[Router],
  sourceMapper: OptionalSourceMapper
) extends Provider[PlayJsonHttpErrorHandler] {
  private lazy val instance = new PlayJsonHttpErrorHandler(
    router.get(),
    config,
    sourceMapper.sourceMapper
  )
  override def get(): PlayJsonHttpErrorHandler = instance
}
