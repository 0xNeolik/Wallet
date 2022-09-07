package com.clluc.stockmind.controller

import com.clluc.stockmind.port.primary.BootstrapPort
import com.google.inject.Inject
import play.Environment
import com.clluc.stockmind.port.primary.LicensePort

class Startup @Inject()(
    environment: Environment,
    bootstrapPort: BootstrapPort,
    licensePort: LicensePort
) {

  // The code here will be executed at startup, when Guice instances this class as a singleton.
  // This is the recommended pattern for startup code in the current Play docs.
  // https://www.playframework.com/documentation/2.5.x/GlobalSettings

  if (!environment.isTest) {
    bootstrapPort.asyncBootstrap()
    licensePort.checkLicense()
  }

}
