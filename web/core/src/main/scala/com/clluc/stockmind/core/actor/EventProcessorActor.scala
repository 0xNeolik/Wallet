package com.clluc.stockmind.core.actor

import akka.actor.Props
import cats.instances.future._
import com.clluc.stockmind.core.actor.Freezable.Unfreeze
import com.clluc.stockmind.core.ethereum.{
  erc20BurnEventSignature,
  erc20MintEventSignature,
  erc20TransferEventSignature,
  LoggedEvent,
  TokenFactoryContract,
  Transaction
}

import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object EventProcessorActor {

  def props(
      timestamp: => DateTime
  )(
      implicit
      executionContext: ExecutionContext,
      eventProcessorOps: EventProcessorOps[Future]
  ) =
    Props(new EventProcessorActor(timestamp))
}

private[actor] class EventProcessorActor(
    timestamp: => DateTime
)(
    implicit
    executionContext: ExecutionContext,
    eventProcessorOps: EventProcessorOps[Future]
) extends Freezable
    with EventProcessorLogic {

  override def receive: Receive = {
    case event @ LoggedEvent(_, _, `erc20TransferEventSignature` :: _, _, _, _, _) =>
      freeze()

      logger.info(s"Received: $event")

      val writeToDb = handleErc20Event(event, timestamp).value
      writeToDb.onComplete(resumeOrLog)

    // Match against transactions with defined destinations. Contract creations shouldn't ever
    // arrive here but this is an almost-free safeguard.
    case event1 @ LoggedEvent(_, _, `erc20MintEventSignature` :: _, _, _, _, _) =>
      freeze()

      logger.info(s"Received: $event1")

      val writeToDb = handleErc20MintEvent(event1, timestamp).value
      writeToDb.onComplete(resumeOrLog)

    case event2 @ LoggedEvent(_, _, `erc20BurnEventSignature` :: _, _, _, _, _) =>
      freeze()

      logger.info(s"Received: $event2")

      val writeToDb = handleErc20BurnEvent(event2, timestamp).value
      writeToDb.onComplete(resumeOrLog)

    case event @ LoggedEvent(_, _, TokenFactoryContract.transferNFTEvent :: _, _, _, _, _) =>
      freeze()

      logger.info(s"Received: $event")

      val writeToDb = handleErc721MintEvent(event, timestamp).value
      writeToDb.onComplete(resumeOrLog)

    case event @ LoggedEvent(_, _, TokenFactoryContract.burnNFTEvent :: _, _, _, _, _) =>
      freeze()

      logger.info(s"Received: $event")

      val writeToDb = handleErc721BurnEvent(event, timestamp).value
      writeToDb.onComplete(resumeOrLog)

    case tx @ Transaction(_, _, Some(_), _, _, _) =>
      freeze()
      logger.info(s"Received $tx")
      val handleTx = handleEtherTransaction(tx, timestamp).value
      handleTx.onComplete(resumeOrLog)

    case unknown =>
      logger.warn(s"Unknown message: $unknown")
  }

  private def resumeOrLog(
      t: Try[Either[Error, EventProcessorLogic.EthereumEventProcessingResult]]): Unit = t match {
    case Failure(e) =>
      logger.error(e.toString)
      self ! Unfreeze // TODO research how the actor should behave
    case Success(result) =>
      result.left.foreach(error => logger.error(s"ERROR handling Ethereum event: $error"))
      self ! Unfreeze
  }
}
