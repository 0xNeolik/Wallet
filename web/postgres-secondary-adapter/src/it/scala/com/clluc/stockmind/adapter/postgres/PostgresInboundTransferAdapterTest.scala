package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.core.ethereum.TransferEvent
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.transaction.InboundTransfer
import org.postgresql.util.PSQLException
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.Future

class PostgresInboundTransferAdapterTest extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  val erc20InfoAdapter = new PostgresErc20InfoAdapter(testTransactor)
  val erc20TransferEventDao = new PostgresErc20TransferEventAdapter(testTransactor)(executionContext)
  val inboundTransferDao = new PostgresInboundTransferAdapter(testTransactor)(executionContext)

  override def afterAll() = TableCleaner.clean()

  behavior of "PostgresInboundTransferAdapter"

  import Generators._

  private def writeTransfer: Future[TransferEvent] = {
    val address = Address(genAddress.sample.get)
    val _token = genErc20Token.sample.get
    val _transfer = genErc20Transfer(_token).sample.get.copy(from = address)
    for {
      _ <- erc20InfoAdapter.createEthereumToken(_token)
      transfer <- erc20TransferEventDao.createTransferEvent(_transfer)
    } yield transfer
  }

  it should "not create a InboundTransfer if the first step does not match a known transfer" in {
    recoverToSucceededIf[PSQLException] {
      writeTransfer.flatMap { t2 =>
        inboundTransferDao.create(InboundTransfer(123123L, t2.txHash))
      }
    }
  }

  it should "create an InboundTransfer" in {
    for {
      t1 <- writeTransfer
      t2 <- writeTransfer
      it <- inboundTransferDao.create(InboundTransfer(t1.id, t2.txHash))
    } yield succeed
  }

  it should "not create an InboundTransfer with a duplicate first step" in {
    recoverToSucceededIf[PSQLException] {
      for {
        t1 <- writeTransfer
        t2 <- writeTransfer
        t3 <- writeTransfer
        it1 <- inboundTransferDao.create(InboundTransfer(t1.id, t2.txHash))
        it2 <- inboundTransferDao.create(InboundTransfer(t1.id, t3.txHash))
      } yield it2
    }
  }

  it should "not create an InboundTransfer with a duplicate second step" in {
    recoverToSucceededIf[PSQLException] {
      for {
        t1 <- writeTransfer
        t2 <- writeTransfer
        t3 <- writeTransfer
        it1 <- inboundTransferDao.create(InboundTransfer(t1.id, t2.txHash))
        it2 <- inboundTransferDao.create(InboundTransfer(t3.id, t2.txHash))
      } yield it2
    }
  }

  it should "not find (by second step) a non-existent InboundTransfer" in {
    val txHash = genEthHash.sample.get
    for {
      it <- inboundTransferDao.findBySecondStep(txHash)
    } yield it shouldBe None
  }

  it should "find (by second step) an InboundTransfer" in {
    for {
      t1 <- writeTransfer
      t2 <- writeTransfer
      it <- inboundTransferDao.create(InboundTransfer(t1.id, t2.txHash))
      found <- inboundTransferDao.findBySecondStep(it.secondStepHash.hash)
    } yield found shouldBe Some(it)
  }

}
