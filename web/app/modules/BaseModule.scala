package modules

import com.clluc.stockmind.controller.Startup
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

/**
  * The base Guice module.
  */
class BaseModule extends AbstractModule with ScalaModule {

  /**
    * Configures the module.
    */
  def configure(): Unit = {
    bind[Startup].asEagerSingleton()
  }
}
