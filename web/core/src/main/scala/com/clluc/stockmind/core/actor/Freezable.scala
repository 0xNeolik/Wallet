package com.clluc.stockmind.core.actor

import akka.actor.{Actor, Stash}
import com.clluc.stockmind.core.actor.Freezable.Unfreeze
import com.typesafe.scalalogging.LazyLogging

object Freezable {
  case object Unfreeze
}

private[actor] trait Freezable extends Actor with Stash with LazyLogging {

  def frozen: Receive = {
    case Unfreeze =>
      context.unbecome()
      unstashAll()
    case _ =>
      stash()
  }

  def freeze() = context.become(frozen, discardOld = false)

}
