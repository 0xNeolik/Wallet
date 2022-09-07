package com.clluc.stockmind.core.actor

import akka.actor.Props
import akka.testkit.{TestActorRef, TestProbe}
import com.clluc.stockmind.core.Generators
import com.clluc.stockmind.core.actor.ActorTestUtils.{ActorTest, CommonFixture}
import com.clluc.stockmind.core.actor.EventWatcherActor.{
  CatchupTo,
  Check,
  ReadBlock,
  WatchNewEthToken
}
import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.core.ethereum.solidity.Address

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._

class EventWatcherActorTest extends ActorTest("EventWatcherActorTest") {

  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  trait Fixture extends CommonFixture {

    val sampleEventConsumer = TestProbe()

    def getWatcherActor = TestActorRef(
      new EventWatcherActor(
        Block(100),
        List(Address("1234567890123456789012345678901234567890")),
        List("0x0"),
        _ => Future.successful(Right(loggedEvents)),
        Future.successful,
        event => {
          sampleEventConsumer.ref ! event
        }
      )
    )

  }

  behavior of "Actor construction"

  it should "have a 'props' helper" in {
    val props = EventWatcherActor.props(
      Block(1),
      List(),
      List(),
      _ => ???,
      _ => ???,
      _ => ()
    )

    props shouldBe a[Props]

    EventWatcherActor.getClass.getSimpleName should startWith(props.actorClass().getSimpleName)
  }

  behavior of "Block updating"

  it should "update the current block when the incoming block has an equal number" in new Fixture {
    val watcher      = getWatcherActor
    val watcherActor = watcher.underlyingActor
    val newBlock     = Block(100)
    val storedBlock  = Block(newBlock.blockNumber + 1)

    watcher ! ReadBlock(newBlock)
    watcherActor.currentBlock shouldBe storedBlock
  }

  it should "update the current block when the incoming block has a bigger number" in new Fixture {
    val watcher      = getWatcherActor
    val watcherActor = watcher.underlyingActor
    val newBlock     = Block(340)
    val storedBlock  = Block(newBlock.blockNumber + 1)

    watcher ! ReadBlock(newBlock)
    watcherActor.currentBlock shouldBe storedBlock
  }

  it should "not update the current block when the incoming block has a smaller block number" in new Fixture {
    val watcher      = getWatcherActor
    val watcherActor = watcher.underlyingActor
    val newBlock     = Block(50)
    val oldBlock     = watcher.underlyingActor.currentBlock

    watcher ! ReadBlock(newBlock)
    watcherActor.currentBlock shouldBe oldBlock
  }

  it should "catch up to the current block when significantly behind" in new Fixture {
    val blockNotify = TestProbe()

    val watcher = system.actorOf(
      Props(
        new EventWatcherActor(
          Block(10000),
          List(Address("1234567890123456789012345678901234567890")),
          List("0x0"),
          _ =>
            Future.successful(
              Right(
                List.empty[LoggedEvent]
              )
          ),
          block =>
            Future.successful {
              blockNotify.ref ! block; block
          },
          _ => ()
        )
      ))
    watcher ! CatchupTo(Block(15000))
    blockNotify.expectMsg(Block(11001))
    blockNotify.expectMsg(Block(12002))
    blockNotify.expectMsg(Block(13003))
    blockNotify.expectMsg(Block(14004))
  }

  behavior of "Event handling"

  it should "call the event handler function for every parsed event" in new Fixture {
    val watcher = getWatcherActor
    watcher ! Check

    sampleEventConsumer.expectMsg(10.seconds, loggedEvent1)
    sampleEventConsumer.expectMsg(10.seconds, loggedEvent2)
  }

  behavior of "New token watching"

  it should "look for events on the new token if it was created before the current block, and add the token" in new Fixture {
    val newToken     = Generators.genEthToken.sample.get.copy(birthBlock = Some(50))
    val watcher      = getWatcherActor
    val watcherActor = watcher.underlyingActor
    val actorBlock   = watcherActor.currentBlock

    watcher ! WatchNewEthToken(newToken)

    sampleEventConsumer.expectMsg(10.seconds, loggedEvent1)
    sampleEventConsumer.expectMsg(10.seconds, loggedEvent2)

    watcherActor.watchedAddresses should contain(newToken.contract)
    watcherActor.currentBlock shouldBe actorBlock
  }

  it should "just add the token if it was created after or on the current watched block" in new Fixture {
    val newToken1    = Generators.genEthToken.sample.get.copy(birthBlock = Some(100))
    val newToken2    = Generators.genEthToken.sample.get.copy(birthBlock = Some(120))
    val watcher      = getWatcherActor
    val watcherActor = watcher.underlyingActor

    watcher ! WatchNewEthToken(newToken1)
    watcher ! WatchNewEthToken(newToken2)

    sampleEventConsumer.expectNoMsg(1.second)

    (watcherActor.watchedAddresses should contain).allOf(newToken1.contract, newToken2.contract)
  }

  it should "do nothing if the token is already being watched" in new Fixture {
    val existingTokenAddress = Address("1234567890123456789012345678901234567890")
    val newToken             = Generators.genEthToken.sample.get.copy(contract = existingTokenAddress)

    val watcher      = getWatcherActor
    val watcherActor = watcher.underlyingActor

    watcherActor.watchedAddresses should contain(existingTokenAddress)

    watcher ! WatchNewEthToken(newToken)

    sampleEventConsumer.expectNoMsg(1.second)
    watcherActor.watchedAddresses should contain(existingTokenAddress)
  }

}
