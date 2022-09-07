package com.clluc.stockmind.core.actor

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import com.clluc.stockmind.core.Generators._
import com.clluc.stockmind.core.actor.ActorTestUtils.ActorTest
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.JsonRpcPlainResult
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.port.secondary.EthereumAccountPort

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SupplierAccountActorTest extends ActorTest("SupplierAccountsActorTest") {

  behavior of "SupplierAccountActor"

  it should "send ether when account is below the threshold (token transfer)" in new Fixture {

    val probe = TestProbe()
    val event = genLoggedEvent.sample.get

    val transferEvent = event.copy(
      topics = erc20TransferEventSignature :: event.topics.tail
    )
    val addressToRefill = Address.decode(transferEvent.topics(1))

    val supplier = getSupplier { (_, dest, _, _) =>
      probe.ref ! dest
      Future.successful(Right(""))
    }

    supplier ! transferEvent
    probe.expectMsg(addressToRefill)
  }

  it should "send ether when the account is below the threshold (ether transfer)" in new Fixture {
    val probe           = TestProbe()
    val addressToRefill = Address(genAddress.sample.get)

    val tx = Transaction(EthereumHash(genEthHash.sample.get),
                         addressToRefill,
                         Some(Address.default),
                         Block(0),
                         0,
                         Uint(value = 102983571L))

    val supplier = getSupplier { (_, dest, _, _) =>
      probe.ref ! dest
      Future.successful(Right(""))
    }

    supplier ! tx
    probe.expectMsg(addressToRefill)
  }
}

trait Fixture {

  trait MockEthereumAccountAdapter extends EthereumAccountPort {
    override def findAccountByUserId(user: UUID): Future[Option[EthereumAccount]]        = ???
    override def findAccountByAddress(address: Address): Future[Option[EthereumAccount]] = ???
    override def saveAccount(userAccount: EthereumAccount): Future[EthereumAccount]      = ???
    override def accountFor(userId: UUID): Future[(EthereumAccount, Boolean)]            = ???
  }

  val supplierAddress  = Address(genAddress.sample.get)
  val supplierPassword = "password"
  val refillThreshold  = BigInt(100)
  val amountToRefill   = BigInt(100)
  val ethMasterAccount = Address(genAddress.sample.get)

  val mockAdapter = new MockEthereumAccountAdapter {
    override def findAccountByAddress(address: solidity.Address) =
      Future.successful(Some(EthereumAccount(UUID.randomUUID(), address, "")))
  }

  def getSupplier(sendEther: (Address, Address, BigInt, String) => Future[JsonRpcPlainResult])(
      implicit system: ActorSystem) =
    system.actorOf(
      SupplierAccountsActor.props(
        (supplierAddress, supplierPassword),
        refillThreshold,
        amountToRefill,
        mockAdapter,
        ethMasterAccount,
        _ => {
          Future.successful(
            Right(BigInt(100))
          )
        },
        sendEther
      ))

}
