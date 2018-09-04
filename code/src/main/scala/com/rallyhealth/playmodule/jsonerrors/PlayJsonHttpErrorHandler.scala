package com.rallyhealth.playmodule.jsonerrors

import play.api.http.{DefaultHttpErrorHandler, HttpErrorConfig}
import play.api.routing.Router
import play.core.SourceMapper

/**
  * An implementation of [[DefaultHttpErrorHandler]] with [[PlayJsonHttpErrorHandling]] already extended.
  *
  * @param lzyRouter a thunk that produces that router (lazy since most routers depend on an
  *                  [[play.api.http.HttpErrorHandler]] either directly or transitively)
  * @param config a config that determines whether to show development-level error details / play editor
  * @param sourceMapper an optional object that finds the source code associated with a thrown exception
  */
class PlayJsonHttpErrorHandler(
  lzyRouter: => Router,
  override protected val config: JsonHttpErrorConfig,
  sourceMapper: Option[SourceMapper]
) extends DefaultHttpErrorHandler(
  HttpErrorConfig(config.showDevErrors, config.playEditor),
  sourceMapper,
  Some(lzyRouter)
) with PlayJsonHttpErrorHandling {
  override protected lazy val router: Router = lzyRouter
}
