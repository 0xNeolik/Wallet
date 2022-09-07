package com.clluc.stockmind.core.actor

import akka.actor.Props
import cats.data.OptionT
import cats.instances.future._
import com.clluc.stockmind.core.actor.Freezable.Unfreeze
import com.clluc.stockmind.core.actor.SupplierAccountsActor.Password
import com.clluc.stockmind.core.ethereum
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.{
  JsonRpcPlainResult,
  UnexpectedEthereumResponse
}
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.clluc.stockmind.core.ethereum.{LoggedEvent, TokenFactoryContract, Transaction}
import com.clluc.stockmind.port.secondary.EthereumAccountPort
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

object SupplierAccountsActor {
  type Password = String

  def props(
      supplier: (Address, String),
      refillThreshold: BigInt,
      amountToRefill: BigInt,
      ethereumAccountPort: EthereumAccountPort,
      ethMasterAccount: Address,
      getBalance: Address => Future[Either[UnexpectedEthereumResponse, BigInt]],
      sendEther: (Address, Address, BigInt, String) => Future[JsonRpcPlainResult]
  )(
      implicit
      executionContext: ExecutionContext
  ) =
    Props(
      new SupplierAccountsActor(
        supplier,
        refillThreshold,
        amountToRefill,
        ethereumAccountPort,
        ethMasterAccount,
        getBalance,
        sendEther
      ))
}

class SupplierAccountsActor(
    supplier: (Address, Password),
    refillThreshold: BigInt,
    amountToRefill: BigInt,
    ethereumAccountPort: EthereumAccountPort,
    ethMasterAccount: Address,
    getBalance: Address => Future[Either[UnexpectedEthereumResponse, BigInt]],
    sendEther: (Address, Address, BigInt, String) => Future[JsonRpcPlainResult]
)(
    implicit
    executionContext: ExecutionContext
) extends Freezable
    with LazyLogging {

  private val erc20TransferEvent = ethereum.erc20TransferEventSignature
  private val erc20MintEvent     = ethereum.erc20MintEventSignature
  private val erc20BurnEvent     = ethereum.erc20BurnEventSignature

  override def receive: Receive = {
    case event @ LoggedEvent(_, _, `erc20TransferEvent` :: tokenSender :: _, _, _, _, _) =>
      freeze()

      val tokenSenderAddress = Address.decode(tokenSender)

      supplyIfNeeded(tokenSenderAddress).value
        .onComplete(_ => self ! Unfreeze)

    case event @ LoggedEvent(_, _, `erc20MintEvent` :: tokenSender :: _, _, _, _, _) =>
      freeze()

      val tokenSenderAddress = Address.decode(tokenSender)

      supplyIfNeeded(tokenSenderAddress).value
        .onComplete(_ => self ! Unfreeze)

    case event @ LoggedEvent(_, _, `erc20BurnEvent` :: tokenSender :: _, _, _, _, _) =>
      freeze()

      val tokenSenderAddress = Address.decode(tokenSender)

      supplyIfNeeded(tokenSenderAddress).value
        .onComplete(_ => self ! Unfreeze)

    case event @ LoggedEvent(_,
                             _,
                             TokenFactoryContract.burnNFTEvent :: tokenSender :: _,
                             _,
                             _,
                             _,
                             _) =>
      freeze()

      val tokenSenderAddress = Address.decode(tokenSender)

      supplyIfNeeded(tokenSenderAddress).value
        .onComplete(_ => self ! Unfreeze)

    case event @ LoggedEvent(_,
                             _,
                             TokenFactoryContract.transferNFTEvent :: tokenSender :: _,
                             _,
                             _,
                             _,
                             _) =>
      freeze()

      val tokenSenderAddress = Address.decode(tokenSender)

      supplyIfNeeded(tokenSenderAddress).value
        .onComplete(_ => self ! Unfreeze)

    case event @ LoggedEvent(_,
                             _,
                             TokenFactoryContract.newToken721Event :: tokenSender :: _,
                             _,
                             _,
                             _,
                             _) =>
      freeze()

      val tokenSenderAddress = Address.decode(tokenSender)

      supplyIfNeeded(tokenSenderAddress).value
        .onComplete(_ => self ! Unfreeze)

    case Transaction(_, sender, Some(_), _, _, Uint(_, value)) =>
      freeze()

      supplyIfNeeded(sender).value
        .onComplete(_ => self ! Unfreeze)

    case unknown => logger.warn(s"Unknown message: $unknown")
  }

  def supplyIfNeeded(sender: Address): OptionT[Future, Any] = {

    def _balance(address: Address): OptionT[Future, BigInt] =
      OptionT(getBalance(address).map(_.toOption))

    def _validDestination(address: Address): OptionT[Future, Address] =
      OptionT {
        if (address == ethMasterAccount) {
          Future.successful(Some(address))
        } else {
          ethereumAccountPort.findAccountByAddress(address).map(_.map(_.address))
        }
      }

    for {
      refillDestination <- _validDestination(sender)
      balance           <- _balance(refillDestination)
    } yield {
      if (balance <= refillThreshold) {
        logger.info(
          s"Account $sender has a balance of $balance, less than the threshold of $refillThreshold. Refilling with $amountToRefill")
        sendEther(supplier._1, refillDestination, amountToRefill, supplier._2)
      }
    }
  }

}
