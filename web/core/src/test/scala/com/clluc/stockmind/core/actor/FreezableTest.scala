package com.clluc.stockmind.core.actor

import akka.actor.Props
import com.clluc.stockmind.core.actor.ActorTestUtils.ActorTest
import com.clluc.stockmind.core.actor.Freezable.Unfreeze

class FreezableTest extends ActorTest("FreezableTest") {

  case object Ping
  case object Pong
  case object Freeze

  class SampleFreezableActor extends Freezable {
    override def receive = {
      case Ping =>
        sender() ! Pong
      case Freeze =>
        freeze()
    }
  }

  trait Fixture {
    def getFreezableActor = system.actorOf(Props(new SampleFreezableActor))
  }

  behavior of "Freezable actor trait"

  it should "Act as usual when unfrozen" in new Fixture {
    val actor = getFreezableActor
    actor ! Ping
    expectMsg(Pong)
  }

  it should "Freeze when ordered to" in new Fixture {
    val actor = getFreezableActor
    actor ! Freeze
    actor ! Ping
    expectNoMsg()
  }

  it should "Resume its operation when unfreezing" in new Fixture {
    val actor = getFreezableActor
    actor ! Freeze
    actor ! Ping
    expectNoMsg()
    actor ! Unfreeze
    expectMsg(Pong)
  }

}
