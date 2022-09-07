package com.clluc.stockmind.core.actor

import akka.actor.Props
import cats.syntax.either._
import com.clluc.stockmind.core.actor.EventWatcherActor.{
  CatchupTo,
  Check,
  ReadBlock,
  WatchNewEthToken
}
import com.clluc.stockmind.core.actor.Freezable.Unfreeze
import com.clluc.stockmind.core.ethereum.{Block, EthFilter, Ethtoken, LoggedEvent}
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.JsonRpcLoggedEvents
import com.clluc.stockmind.core.ethereum.solidity.Address

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object EventWatcherActor {

  def props(
      startingBlock: Block,
      addresses: List[Address],
      topics: List[String],
      getLogs: EthFilter => Future[JsonRpcLoggedEvents],
      setBlock: Block => Future[Block],
      onEvent: LoggedEvent => Unit,
  )(
      implicit
      executionContext: ExecutionContext
  ): Props = {

    Props(
      new EventWatcherActor(
        startingBlock,
        addresses,
        topics,
        getLogs,
        setBlock,
        onEvent
      ))
  }

  case class CatchupTo(block: Block)
  case object Check
  case class ReadBlock(block: Block)
  case class WatchNewEthToken(token: Ethtoken)
}

class EventWatcherActor(
    startingBlock: Block,
    addresses: List[Address],
    topics: List[String],
    getLogs: EthFilter => Future[JsonRpcLoggedEvents],
    setBlock: Block => Future[Block],
    onEvent: LoggedEvent => Unit,
)(
    implicit
    executionContext: ExecutionContext,
) extends Freezable {

  private[actor] var currentBlock: Block             = startingBlock
  private[actor] var watchedAddresses: List[Address] = addresses

  override def receive = {

    case CatchupTo(block) =>
      val toBlock = Block(currentBlock.blockNumber + 1000)
      if (toBlock.blockNumber > block.blockNumber) {
        self ! Check
      } else {
        logger.info(
          s"Checking logs on addresses $watchedAddresses for topics $topics, from $currentBlock to $toBlock (catch-up to $block)")
        val filter = EthFilter(currentBlock, toBlock, watchedAddresses, List(topics))
        getLogsAndActOnEvents(filter)
        self ! ReadBlock(toBlock)
        self ! CatchupTo(block)
      }

    case Check =>
      logger.info(
        s"Checking logs on addresses $watchedAddresses for topics $topics, from $currentBlock")
      val filter = EthFilter(currentBlock, watchedAddresses, List(topics))
      getLogsAndActOnEvents(filter)
      context.system.scheduler.scheduleOnce(30.seconds, self, Check)

    case ReadBlock(block) =>
      freeze()
      if (block.blockNumber >= currentBlock.blockNumber) {
        currentBlock = Block(block.blockNumber + 1)
        setBlock(currentBlock).transform(_ => self ! Unfreeze, { e =>
          logger.error(e.toString); e
        })
      } else {
        self ! Unfreeze
      }
    case WatchNewEthToken(token) =>
      if (!watchedAddresses.contains(token.contract)) {
        // We need to ensure all events emitted by this token are processed
        val block = Block(token.birthBlock.getOrElse(0))

        // If the token was created before the current block, there may be transfer events
        // for that token between the token's birth block and the current block. In that
        // case we look for transfer events within that range, just for that token.
        if (block.blockNumber < currentBlock.blockNumber) {
          val limitBlock = Block(currentBlock.blockNumber - 1)
          logger.info(
            s"Checking logs for new token at ${token.contract} for topics $topics, from $block to $limitBlock")
          val filter = EthFilter(block, limitBlock, List(token.contract), List(topics))

          // Since these events are older than the most recent event, we shouldn't update
          // the actor's current block using events found here as a reference.
          getLogsAndActOnEvents(filter, updateBlock = false)
        }
        // All new events will come after the current block. Add the token to the address list and keep on
        logger.info(
          s"Added ${token.contract} to observed addresses as it belongs to a new token ($token)")
        watchedAddresses = watchedAddresses :+ token.contract
      }

    case unknown =>
      logger.warn(s"Unknown message: $unknown")
  }

  private def getLogsAndActOnEvents(filter: EthFilter, updateBlock: Boolean = true): Unit = {

    def sendEventsAndUpdate(events: JsonRpcLoggedEvents) = {
      events
        .leftMap { ethereumFail =>
          logger.error(s"Ethereum fail: $ethereumFail")
        }
        .map { events =>
          events.foreach { event =>
            if (updateBlock) {
              self ! ReadBlock(event.block)
            }
            onEvent(event)
          }
        }
    }

    getLogs(filter).transform(sendEventsAndUpdate, e => { logger.error(e.toString); e })
  }
}
