package com.rallyhealth.playmodule.jsonerrors

import play.api.{Configuration, Environment, Mode}

/**
  * Similar to [[play.api.http.HttpErrorConfig]] but for JSON APIs.
  *
  * @param showDevErrors whether to show development level details when encountering errors
  * @param showRoutes whether to list out the routes when encountering a 404
  * @param playEditor hyperlink string to wrap around Play error messages
  */
case class JsonHttpErrorConfig(showDevErrors: Boolean, showRoutes: Boolean, playEditor: Option[String])

object JsonHttpErrorConfig {

  // This only exists for source compatibility with Play 2.8 and older verions of Play Config
  private implicit class ConfigSourceCompat(private val config: Configuration) extends AnyVal {
    def getString(path: String): Option[String] = {
      if (config.underlying.hasPath(path)) Some(config.underlying.getString(path))
      else None
    }
  }

  def fromEnvironment(env: Environment, config: Configuration): JsonHttpErrorConfig = {
    val isDev = env.mode != Mode.Prod
    val editor = config.getString("play.editor")
    JsonHttpErrorConfig(isDev, isDev, editor)
  }

  def fromEnvironment(env: Environment): JsonHttpErrorConfig = {
    fromEnvironment(env, Configuration.load(env))
  }
}
