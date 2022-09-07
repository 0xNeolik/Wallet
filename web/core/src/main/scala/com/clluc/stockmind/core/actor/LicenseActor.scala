package com.clluc.stockmind.core.actor

import com.clluc.stockmind.port.primary.LicensePort

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import akka.actor.ActorSystem
import javax.inject.Inject

class LicenseActor @Inject()(licensePort: LicensePort, actorSystem: ActorSystem)(
    implicit ec: ExecutionContext) {

  def configure() = {}

  actorSystem.scheduler.schedule(initialDelay = 1.seconds, interval = 1.days) {
    licensePort.checkLicense()
  }

}

object LicenseActor {

  def apply(licensePort: LicensePort, actorSystem: ActorSystem)(
      implicit ec: ExecutionContext): LicenseActor =
    new LicenseActor(licensePort, actorSystem)(ec)
}
