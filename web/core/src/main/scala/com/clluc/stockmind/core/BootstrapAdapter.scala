package com.clluc.stockmind.core

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import cats.instances.future._
import com.clluc.stockmind.core.Bootstrap.{BootstrapConfigurationDependencies, BootstrapException}
import com.clluc.stockmind.core.actor.BlockWatcherActor.MostRecentBlock
import com.clluc.stockmind.core.actor.EventWatcherActor.CatchupTo
import com.clluc.stockmind.core.actor._
import com.clluc.stockmind.core.ethereum.{
  erc20BurnEventSignature,
  erc20MintEventSignature,
  erc20TransferEventSignature,
  Block,
  TokenFactoryContract
}
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.tokens.{TokenCreationLogic, TokenCreationOps, TokenCreationOpsImpl}
import com.clluc.stockmind.port.primary.BootstrapPort
import com.clluc.stockmind.port.secondary._
import com.clluc.stockmind.util.LogicUtils._
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

private[core] class BootstrapAdapter(
    erc20InfoPort: Erc20InfoPort,
    erc721InfoPort: Erc721InfoPort,
    erc20TransferEventPort: Erc20TransferEventPort,
    ethereumAccountPort: EthereumAccountPort,
    appConfigPort: AppConfigPort,
    offchainTransferPort: OffChainTransferPort,
    inboundTransferPort: InboundTransferPort,
    outboundTransferPort: OutboundTransferPort,
    ethereumClientPort: EthereumClientPort,
    conf: BootstrapConfigurationDependencies,
    timestamp: => DateTime
)(
    implicit
    system: ActorSystem,
    executionContext: ExecutionContext
) extends BootstrapPort
    with TokenCreationLogic
    with LazyLogging {

  implicit val materializer = ActorMaterializer()

  implicit val tokenCreationOps: TokenCreationOps[Future] =
    new TokenCreationOpsImpl(erc20InfoPort, erc721InfoPort, ethereumClientPort)

  override def asyncBootstrap(): Unit = {
    val erc20TransferEvent  = erc20TransferEventSignature
    val erc20MintEvent      = erc20MintEventSignature
    val erc20BurnEvent      = erc20BurnEventSignature
    val ethMasterAccount    = Address.decode(conf.ethereumMasterAccount)
    val tokenFactoryAddress = Address.decode(conf.tokenFactoryAddress)
    val gasSupplierAccount  = Address.decode(conf.etherSupplierAddress)
    val supplierPassword    = conf.etherSupplierAccountPassword
    val refillThreshold     = conf.etherRefillThreshold
    val amountToRefill      = conf.etherAmountToRefill

    val eventualStartupResult: Future[Unit] = for {
      tokenWatcherBlock    <- appConfigPort.getBlock("newtokenblock")
      token721WatcherBlock <- appConfigPort.getBlock("newtoken721block")
      transferWatcherBlock <- appConfigPort.getBlock("transferblock")
      mintWatcherBlock     <- appConfigPort.getBlock("mintblock")
      mint721WatcherBlock  <- appConfigPort.getBlock("createNFTblock")
      burnWatcherBlock     <- appConfigPort.getBlock("burnblock")
      burn721WatcherBlock  <- appConfigPort.getBlock("burn721block")
      blockWatcherBlock    <- appConfigPort.getBlock("blockwatcherblock")
      tokensErc20          <- erc20InfoPort.findAllErc20Tokens()
      tokensErc721         <- erc721InfoPort.findAllErc721Tokens()
      tokensAddressesErc20  = tokensErc20.map(_.contract)
      tokensAddressesErc721 = tokensErc721.map(_.contract)
      lastKnownBlockResponse <- ethereumClientPort.findEthereumBlockNumber()
    } yield {
      lastKnownBlockResponse match {
        case Right(successfulResponse) =>
          val maybeBlock: Option[Block] = Block.fromHexString(successfulResponse)

          implicit val eventProcessorOps: EventProcessorOps[Future] = new EventProcessorOpsImpl(
            erc20InfoPort,
            erc721InfoPort,
            erc20TransferEventPort,
            inboundTransferPort,
            offchainTransferPort,
            outboundTransferPort,
            ethereumAccountPort,
            ethereumClientPort,
            ethMasterAccount,
            gasSupplierAccount,
            system
          )

          val eventProcessor = system.actorOf(EventProcessorActor.props(timestamp))

          val supplierAccount = system.actorOf(
            SupplierAccountsActor.props(
              (gasSupplierAccount, supplierPassword),
              refillThreshold,
              amountToRefill,
              ethereumAccountPort,
              ethMasterAccount,
              ethereumClientPort.findBalanceForAccount,
              ethereumClientPort.sendEther
            ))

          val erc20EventWatcherActor = system.actorOf(
            EventWatcherActor.props(
              transferWatcherBlock,
              tokensAddressesErc20,
              List(erc20TransferEvent),
              ethereumClientPort.getLoggedEvents,
              block => { appConfigPort.setBlock("transferblock", block) },
              event => {
                eventProcessor ! event
                supplierAccount ! event
              }
            ))

          erc20EventWatcherActor ! CatchupTo(maybeBlock.get)

          val erc20EventMintWatcherActor = system.actorOf(
            EventWatcherActor.props(
              mintWatcherBlock,
              tokensAddressesErc20,
              List(erc20MintEvent),
              ethereumClientPort.getLoggedEvents,
              block => { appConfigPort.setBlock("mintblock", block) },
              event => {
                eventProcessor ! event
                supplierAccount ! event
              }
            ))

          erc20EventMintWatcherActor ! CatchupTo(maybeBlock.get)

          val erc721EventMintWatcherActor = system.actorOf(
            EventWatcherActor.props(
              mint721WatcherBlock,
              tokensAddressesErc721,
              List(TokenFactoryContract.transferNFTEvent),
              ethereumClientPort.getLoggedEvents,
              block => { appConfigPort.setBlock("createNFTblock", block) },
              event => {
                eventProcessor ! event
                supplierAccount ! event
              }
            ))

          erc721EventMintWatcherActor ! CatchupTo(maybeBlock.get)

          val erc20EventBurnWatcherActor = system.actorOf(
            EventWatcherActor.props(
              burnWatcherBlock,
              tokensAddressesErc20,
              List(erc20BurnEvent),
              ethereumClientPort.getLoggedEvents,
              block => { appConfigPort.setBlock("burnblock", block) },
              event => {
                eventProcessor ! event
                supplierAccount ! event
              }
            ))

          erc20EventBurnWatcherActor ! CatchupTo(maybeBlock.get)

          val erc721EventBurnWatcherActor = system.actorOf(
            EventWatcherActor.props(
              burn721WatcherBlock,
              tokensAddressesErc721,
              List(TokenFactoryContract.burnNFTEvent),
              ethereumClientPort.getLoggedEvents,
              block => { appConfigPort.setBlock("burn721block", block) },
              event => {
                eventProcessor ! event
                supplierAccount ! event
              }
            ))

          erc721EventBurnWatcherActor ! CatchupTo(maybeBlock.get)

          val newTokenWatcherActor = system.actorOf(
            EventWatcherActor.props(
              tokenWatcherBlock,
              List(tokenFactoryAddress),
              List(TokenFactoryContract.newTokenEvent),
              ethereumClientPort.getLoggedEvents,
              block => { appConfigPort.setBlock("newtokenblock", block) },
              event => {
                registerNewToken(event, erc20EventWatcherActor).logErrors()
                //eventProcessor ! event
              }
            ))

          newTokenWatcherActor ! CatchupTo(maybeBlock.get)

          val newToken721WatcherActor = system.actorOf(
            EventWatcherActor.props(
              token721WatcherBlock,
              List(tokenFactoryAddress),
              List(TokenFactoryContract.newToken721Event),
              ethereumClientPort.getLoggedEvents,
              block => { appConfigPort.setBlock("newtoken721block", block) },
              event => {
                registerNewCollection721(event, erc721EventMintWatcherActor).logErrors()
                //eventProcessor ! event
              }
            ))

          newToken721WatcherActor ! CatchupTo(maybeBlock.get)

          val blockWatcherActor = system.actorOf(
            BlockWatcherActor.props(
              blockWatcherBlock,
              ethereumClientPort.findBlockTransactionsByNumber,
              ethereumAccountPort.findAccountByAddress,
              block => { appConfigPort.setBlock("blockwatcherblock", block) },
              ethMasterAccount,
              eventProcessor,
              supplierAccount,
            ))

          // Block ticker - asks the node which is the most recent block every 30 secs
          Source
            .tick(5.seconds, 30.seconds, true)
            .mapAsync(1) { _ =>
              ethereumClientPort.findEthereumBlockNumber()
            }
            .map { response =>
              val blockString = response.getOrElse(
                throw new RuntimeException("recentBlockTicker failed (connection)"))
              MostRecentBlock(
                Block
                  .fromHexString(blockString)
                  .getOrElse(throw new RuntimeException("recentBlockTicker failed (parsing)")))
            }
            .to(Sink.actorRef(blockWatcherActor, "recentBlockTickerFinished"))
            .run()

          ()

        case Left(unsuccessfulResponse) =>
          throw BootstrapException(unsuccessfulResponse.networkResponseBody)
      }
    }

    // In case the bootstrap of the actors didn't work let it crash!
    eventualStartupResult.onComplete {
      case Success(_) => ()
      case Failure(e) => e
    }
  }
}

object BootstrapAdapter {

  def apply(
      erc20InfoPort: Erc20InfoPort,
      erc721InfoPort: Erc721InfoPort,
      erc20TransferEventPort: Erc20TransferEventPort,
      ethereumAccountPort: EthereumAccountPort,
      appConfigPort: AppConfigPort,
      offchainTransferPort: OffChainTransferPort,
      inboundTransferPort: InboundTransferPort,
      outboundTransferPort: OutboundTransferPort,
      ethereumClientPort: EthereumClientPort,
      conf: BootstrapConfigurationDependencies,
      timestamp: => DateTime
  )(
      implicit
      system: ActorSystem,
      executionContext: ExecutionContext
  ): BootstrapAdapter =
    new BootstrapAdapter(
      erc20InfoPort,
      erc721InfoPort,
      erc20TransferEventPort,
      ethereumAccountPort,
      appConfigPort,
      offchainTransferPort,
      inboundTransferPort,
      outboundTransferPort,
      ethereumClientPort,
      conf,
      timestamp
    )
}
