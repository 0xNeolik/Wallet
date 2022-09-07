package com.clluc.stockmind.core.actor

import com.clluc.stockmind.core.actor.Scheduler.SchedulingConfig
import scala.concurrent.duration._
import akka.actor.{ActorRef, ActorSystem}

/**
  * Schedules the jobs.
  */
private[actor] class Scheduler(
    system: ActorSystem,
    licenseActor: ActorRef,
    schedulingConfig: SchedulingConfig
) {
  case object checkLicense
  //QuartzSchedulerExtension(system).schedule("License", licenseActor, checkLicense)
}

object Scheduler {
  case class SchedulingConfig(
      triggerLag: FiniteDuration
  )

  def apply(
      system: ActorSystem,
      licenseActor: ActorRef,
      schedulingConfig: SchedulingConfig
  ): Scheduler = new Scheduler(system, licenseActor, schedulingConfig)
}
