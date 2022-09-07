package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.core.ethereum.TransferEvent
import com.clluc.stockmind.core.ethereum.solidity.Address
import org.postgresql.util.PSQLException
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.Future

class PostgresErc20TransferEventAdapterTest extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  val erc20InfoAdapter = new PostgresErc20InfoAdapter(testTransactor)
  val erc20TransferEventAdapter = new PostgresErc20TransferEventAdapter(testTransactor)

  override def afterAll() = TableCleaner.clean()

  behavior of "ERC20 TransferEvent DAO (Postgres)"

  import Generators._

  it should "fail when creating a transfer with a non-existing token" in {
    val token = genErc20Token.sample.get
    val transfer = genErc20Transfer(token).sample.get
    recoverToSucceededIf[PSQLException] {
      erc20TransferEventAdapter.createTransferEvent(transfer)
    }
  }

  private def writeTransferByTo(to: Address): Future[TransferEvent] = {
    val _token = genErc20Token.sample.get
    val _transfer = genErc20Transfer(_token).sample.get.copy(to = to)
    for {
      _ <- erc20InfoAdapter.createEthereumToken(_token)
      transfer <- erc20TransferEventAdapter.createTransferEvent(_transfer)
    } yield transfer
  }

  private def writeTransferByFrom(from: Address): Future[TransferEvent] = {
    val _token = genErc20Token.sample.get
    val _transfer = genErc20Transfer(_token).sample.get.copy(from = from)
    for {
      _ <- erc20InfoAdapter.createEthereumToken(_token)
      transfer <- erc20TransferEventAdapter.createTransferEvent(_transfer)
    } yield transfer
  }

  private def writeTransfer: Future[TransferEvent] = {
    val address = Address(genAddress.sample.get)
    writeTransferByFrom(address)
  }

  it should "write a transfer that uses known tokens" in {
    writeTransfer.map(_ => succeed)
  }

  it should "not find any transfer from address that doesn't make any transfer" in {
    val address = Address(genAddress.sample.get)
    erc20TransferEventAdapter.findTransfersInvolvingAddress(address).map(_ shouldBe List.empty)
  }

  it should "not find any page of transfers from address that doesn't make any transfer" in {
    val address = Address(genAddress.sample.get)
    erc20TransferEventAdapter.findTransfersInvolvingAddressPage(address, 20, 0).map(_ shouldBe List.empty)
  }

  it should "find first page of transfers" in {
    val from = Address(genAddress.sample.get)
    for {
      transfers0 <- Future.traverse((1 to 5).toList)(_ => writeTransferByFrom(from))
      transfers1 <- Future.traverse((1 to 5).toList)(_ => writeTransferByTo(from))
      paginatedTransfers <- erc20TransferEventAdapter.findTransfersInvolvingAddressPage(from, 10, 0)
    } yield {
      paginatedTransfers should have length 10
      paginatedTransfers should contain theSameElementsAs (transfers0 ++ transfers1)
    }
  }

  it should "find first and second page of transfers" in {
    val from = Address(genAddress.sample.get)
    for {
      transfersSecond <- Future.traverse((1 to 10).toList)(_ => writeTransferByFrom(from))
      transfersFirst <- Future.traverse((1 to 10).toList)(_ => writeTransferByTo(from))
      firstPaginatedTransfers <- erc20TransferEventAdapter.findTransfersInvolvingAddressPage(from, 10, 0)
      secondPaginatedTransfers <- erc20TransferEventAdapter.findTransfersInvolvingAddressPage(from, 10, 1)
    } yield {
      firstPaginatedTransfers should contain theSameElementsAs transfersFirst
      secondPaginatedTransfers should contain theSameElementsAs transfersSecond
    }
  }

  it should "find all transfers sent by one address" in {
    val token = genErc20Token.sample.get
    val from = Address(genAddress.sample.get)
    for {
      t <- erc20InfoAdapter.createEthereumToken(token)
      txs = (1 to 10).map(_ => genErc20Transfer(t).sample.get.copy(from = from))
      storedTransferEvents <- Future.traverse(txs)(erc20TransferEventAdapter.createTransferEvent)
    } yield storedTransferEvents should have length 10
  }

  it should "find all transfers received by one address" in {
    val token = genErc20Token.sample.get
    val to = Address(genAddress.sample.get)
    for {
      t <- erc20InfoAdapter.createEthereumToken(token)
      txs = (1 to 10).map(_ => genErc20Transfer(t).sample.get.copy(to = to))
      storedTransferEvents <- Future.traverse(txs)(erc20TransferEventAdapter.createTransferEvent)
    } yield storedTransferEvents should have length 10
  }

  it should "find all transfers sent and received by one address" in {
    val token = genErc20Token.sample.get
    val address = Address(genAddress.sample.get)
    for {
      t <- erc20InfoAdapter.createEthereumToken(token)
      txsFrom = (1 to 5).map(_ => genErc20Transfer(t).sample.get.copy(from = address))
      txsTo = (1 to 5).map(_ => genErc20Transfer(t).sample.get.copy(to = address))
      txs = txsFrom ++ txsTo
      storedTransferEvents <- Future.traverse(txs)(erc20TransferEventAdapter.createTransferEvent)
    } yield storedTransferEvents should have length 10
  }

  it should "not find a specific transfer" in {
    for {
      found <- erc20TransferEventAdapter.find(12345L)
    } yield found shouldBe None
  }

  it should "find a specific transfer" in {
    for {
      transfer <- writeTransfer
      found <- erc20TransferEventAdapter.find(transfer.id)
    } yield found shouldBe Some(transfer)
  }
}
