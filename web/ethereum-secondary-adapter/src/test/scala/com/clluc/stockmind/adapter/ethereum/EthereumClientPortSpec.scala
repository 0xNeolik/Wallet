package com.clluc.stockmind.adapter.ethereum

import com.clluc.stockmind.core.ethereum.JsonRpcResponse.Password
import com.clluc.stockmind.core.ethereum.{
  Block,
  EthCall,
  EthCallFrom,
  EthFilter,
  SignableTransaction
}
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.port.secondary.EthereumClientPort
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

class EthereumClientPortSpec extends FlatSpec with Matchers {
  "A create account generating password call" should "give us the expected result" in {
    val address = "0xd6eca7e5c7af707962c37f131b093243a27854b2"

    val port = new EthereumClientPort {
      override implicit def ec = scala.concurrent.ExecutionContext.Implicits.global

      override def createAccountWithPassword(password: Password) =
        Future.successful(Right(address))

      override def findBalanceForAccount(account: Address) = ???

      override def findEthereumBlockNumber() = ???

      override def signMessage(account: Address, message: String) = ???

      override def sendTransaction(signableTx: SignableTransaction) = ???

      override def callMethod(call: EthCall) = ???

      override def callMethodFrom(call: EthCallFrom) = ???

      override def getLoggedEvents(filter: EthFilter) = ???

      override def defaultRightStatusCode = ???

      override def findBlockTransactionsByNumber(blockNumber: Block) = ???
    }

    val eventualResult = port.createAccountGeneratingPassword()

    ScalaFutures.whenReady(eventualResult) { result =>
      // We for now ignore the password thing
      result.right.get.address shouldBe Address.decode(address)
    }
  }
}
