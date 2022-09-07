package com.clluc.stockmind.port.primary

/**
  * Interface called by the entry point of the application to initialize it. Part of this startup process will take
  * place in the primary (HTTP component) adapter; but part of it relies on actual business logic that falls beyond
  * the scope of a simple HTTP communication plug-in. This trait defines those (second) features.
  */
trait BootstrapPort {
  def asyncBootstrap(): Unit
}
