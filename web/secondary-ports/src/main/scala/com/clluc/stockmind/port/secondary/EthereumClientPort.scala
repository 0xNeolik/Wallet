package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.ethereum.JsonRpcResponse._
import com.clluc.stockmind.core.ethereum._
import solidity.Address

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

trait EthereumClientPort {

  implicit def ec: ExecutionContext

  def createAccountWithPassword(password: String): Future[JsonRpcPlainResult]

  final def createAccountGeneratingPassword(): Future[JsonRpcNewEthereumAccount] = {
    val password = Random.alphanumeric.take(15).mkString

    createAccountWithPassword(password).map(_.right.map(accAdd =>
      NewEthereumAccount(Address.decode(accAdd), password)))
  }

  def findBalanceForAccount(account: Address): Future[JsonRpcBalance]

  def findEthereumBlockNumber(): Future[JsonRpcPlainResult]

  def signMessage(account: Address, message: String): Future[JsonRpcPlainResult]

  def sendTransaction(signableTx: SignableTransaction): Future[JsonRpcPlainResult]

  def callMethod(call: EthCall): Future[JsonRpcPlainResult]

  def callMethodFrom(call: EthCallFrom): Future[JsonRpcPlainResult]

  def getLoggedEvents(filter: EthFilter): Future[JsonRpcLoggedEvents]

  def sendEther(from: Address,
                to: Address,
                amountInWei: BigInt,
                password: String): Future[JsonRpcPlainResult] =
    sendTransaction(SignableTransaction(EthTransaction(from, to, amountInWei), password))

  def findBlockTransactionsByNumber(
      blockNumber: Block): Future[EthereumResponse[Option[List[Transaction]]]]

  def defaultRightStatusCode: Int
}
