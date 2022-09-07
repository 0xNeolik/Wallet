package com.clluc.stockmind.core.actor

import akka.actor.{ActorRef, Props}
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source}
import cats.syntax.either._
import com.clluc.stockmind.core.actor.BlockWatcherActor.{MostRecentBlock, ParseBlock}
import com.clluc.stockmind.core.actor.Freezable.Unfreeze
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.EthereumResponse
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.ethereum.{Block, EthereumAccount, Transaction}

import scala.concurrent.{ExecutionContext, Future}

object BlockWatcherActor {

  def props(
      startingBlock: Block,
      getTransactions: Block => Future[EthereumResponse[Option[List[Transaction]]]],
      findAddress: Address => Future[Option[EthereumAccount]],
      storeLastProcessedBlock: Block => Future[Block],
      masterAddress: Address,
      eventProcessor: ActorRef,
      gasSupplier: ActorRef,
  )(
      implicit
      executionContext: ExecutionContext,
      materializer: Materializer,
  ): Props = {

    Props(
      new BlockWatcherActor(startingBlock,
                            getTransactions,
                            findAddress,
                            storeLastProcessedBlock,
                            masterAddress,
                            eventProcessor,
                            gasSupplier,
      ))
  }

  case class MostRecentBlock(block: Block)
  case class ParseBlock(block: Block)

}

class BlockWatcherActor(
    startingBlock: Block,
    getTransactions: Block => Future[EthereumResponse[Option[List[Transaction]]]],
    findAddress: Address => Future[Option[EthereumAccount]],
    storeLastProcessedBlock: Block => Future[Block],
    masterAddress: Address,
    eventProcessor: ActorRef,
    gasSupplier: ActorRef,
)(
    implicit
    executionContext: ExecutionContext,
    materializer: Materializer,
) extends Freezable {

  var lastProcessedBlock: Block = startingBlock
  var mostRecentBlock: Block    = startingBlock

  override def receive: Receive = {

    case MostRecentBlock(block) =>
      if (block.blockNumber > mostRecentBlock.blockNumber) {

        logger.info(s"$block is the new most recent block (previous: $mostRecentBlock)")

        // If we already processed all previously available blocks, start processing again
        if (lastProcessedBlock == mostRecentBlock) {
          val nextBlock = Block(lastProcessedBlock.blockNumber + 1)
          logger.info(s"Resuming block processing on $nextBlock")
          self ! ParseBlock(nextBlock)
        }

        mostRecentBlock = block
      }

    case ParseBlock(block) =>
      if (block.blockNumber <= mostRecentBlock.blockNumber) {
        // we know this block exists

        logger.debug(s"Parsing $block")

        freeze()

        getTransactions(block)
          .transform(process, e => { logger.error(e.toString); e })
          .map {
            case Left(_) =>
              logger.error(s"Error grabbing transactions from $block")
            case Right(processed) =>
              if (processed) {
                storeLastProcessedBlock(block)
                val nextBlock = Block(block.blockNumber + 1)
                lastProcessedBlock = block
                self ! ParseBlock(nextBlock)
              }
          }
          .onComplete(_ => self ! Unfreeze)

      } else {
        // the block to parse is more recent than the most recent block -> this block doesn't exist yet

        // so we do nothing
        logger.debug(s"Not parsing $block yet (most recent block is $mostRecentBlock")
      }

  }

  def process(evts: EthereumResponse[Option[List[Transaction]]]): Either[Unit, Boolean] = {
    evts
      .leftMap(fail => logger.error(s"Ethereum fail: $fail"))
      .map {
        case None =>
          logger.error("Block doesn't contain a transaction list")
          false
        case Some(transactions) =>
          transactions.map { tx =>
            filterAndSendTransactions.offer(tx)
          }
          true
      }
  }

  def isZeroValueTransaction(tx: Transaction): Boolean = tx.value.value.equals(BigInt(0))

  val filterAndSendTransactions = Source
  // TODO make a conscious decision about queue buffer size
    .queue[Transaction](500, OverflowStrategy.backpressure)

    // Transactions that don't send ether are of no use to us here
    .filterNot(isZeroValueTransaction)

    // Discard contract creation transactions
    .filter(_.to.isDefined)

    // TODO make a conscious decision about parallelism size
    .mapAsync(1) { transaction =>
      // Check for relatedness
      val related = if (transaction.from == masterAddress) {
        Future.successful(true) // Outbound tx
      } else if (transaction.to.contains(masterAddress)) {
        findAddress(transaction.from).map(_.isDefined) // Could be inbound step 2
      } else {
        findAddress(transaction.to.get).map(_.isDefined) // Could be inbound step 1
      }

      related.map((transaction, _))
    }

    // Discard unrelated transactions
    .filter(_._2)
    .map(_._1)

    // Send transactions that concern Stockmind to the event processor and the
    // gas supplier.
    // Note: "successful-completion" will be sent if the stream completes, but
    // we never signal completion at the source.
    .alsoTo(Sink.actorRef(gasSupplier, "successful-completion"))
    .to(Sink.actorRef(eventProcessor, "succesful-completion"))
    .run()

}
