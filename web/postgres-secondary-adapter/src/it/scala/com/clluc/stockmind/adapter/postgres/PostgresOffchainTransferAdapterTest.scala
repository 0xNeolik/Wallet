package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.adapter.postgres.Generators._
import com.clluc.stockmind.core.ethereum.TransferEvent
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.transaction.OffChainTransfer
import org.postgresql.util.PSQLException
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.Future

// TODO This test is not deterministic
class PostgresOffchainTransferAdapterTest extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  val erc20TokenDao = new PostgresErc20InfoAdapter(testTransactor)
  val offchainTransferDao = new PostgresOffChainTransferAdapter(testTransactor)

  val erc20InfoAdapter = new PostgresErc20InfoAdapter(testTransactor)
  val erc20TransferEventAdapter = new PostgresErc20TransferEventAdapter(testTransactor)

  override def afterAll() = TableCleaner.clean()

  behavior of "PostgresOffchainTransferAdapter"

  it should "fail to store an OffchainTransfer regarding an unknown token" in {
    val transfer = genOffchainTransfer("FAIL").sample.get
    recoverToSucceededIf[PSQLException] {
      offchainTransferDao.create(transfer)
    }
  }

  it should "store an OffchainTransfer" in {
    val token = genErc20Token.sample.get
    val transfer = genOffchainTransfer(token.symbol).sample.get
    for {
      _ <- erc20TokenDao.createEthereumToken(token)
      _ <- offchainTransferDao.create(transfer)
    } yield succeed
  }

  it should "not find any page of transfers when there are no transfers" in {
    val address = Address(genAddressString.sample.get)
    for {
      page <- offchainTransferDao.findTransfersInvolvingAddressPage(address, 10, 0)
    } yield page shouldBe empty
  }

  private def _writeTransfer(f: OffChainTransfer => OffChainTransfer): Future[OffChainTransfer] = {
    val token = genErc20Token.sample.get
    val transfer = f(genOffchainTransfer(token.symbol).sample.get)
    for {
      _ <- erc20TokenDao.createEthereumToken(token)
      savedTransfer <- offchainTransferDao.create(transfer)
    } yield savedTransfer
  }

  private def writeTransferFrom(from: Address) = _writeTransfer(_.copy(from = from))
  private def writeTransferTo(to: Address) = _writeTransfer(_.copy(to = to))

  private def testFindTransfersInvolvingAddress(findFx: Address => Future[List[OffChainTransfer]]) = {
    val address = Address(genAddressString.sample.get)
    for {
      sends <- Future.traverse((1 to 5).toList)(_ => writeTransferFrom(address))
      receives <- Future.traverse((1 to 5).toList)(_ => writeTransferTo(address))
      page <- findFx(address)
    } yield {
      page should have length 10
      page should contain theSameElementsAs (sends ++ receives)
    }
  }

  it should "find a page of transfers (both incoming and outgoing)" in {
    testFindTransfersInvolvingAddress(offchainTransferDao.findTransfersInvolvingAddressPage(_, 10, 0))
  }

  it should "find all transfers (both incoming and outgoing) that involve an specific address" in {
    testFindTransfersInvolvingAddress(offchainTransferDao.findTransfersInvolvingAddress)
  }

  it should "find multiple pages when there are enough transfers" in {
    val address = Address(genAddressString.sample.get)
    for {
      firstTen <- Future.traverse((1 to 10).toList)(_ => writeTransferFrom(address))
      secondTen <- Future.traverse((1 to 10).toList)(_ => writeTransferFrom(address))
      firstPage <- offchainTransferDao.findTransfersInvolvingAddressPage(address, 10, 0)
      secondPage <- offchainTransferDao.findTransfersInvolvingAddressPage(address, 10, 1)
    } yield {
      // Newer elements are first
      firstPage should contain theSameElementsAs secondTen
      secondPage should contain theSameElementsAs firstTen
    }

  }

  it should "not find a non-existent transfer" in {
    for {
      found <- offchainTransferDao.find(12345L)
    } yield found shouldBe None
  }

  it should "find a specific transfer" in {
    for {
      transfer <- _writeTransfer(identity)
      found <- offchainTransferDao.find(transfer.id)
    } yield found shouldBe Some(transfer)
  }

  it should "link an off-chain transaction to an on-chain one correctly" in {
    def writeOnchainTransfer(): Future[TransferEvent] = {
      val address = Address(genAddress.sample.get)
      val _token = genErc20Token.sample.get
      val _transfer = genErc20Transfer(_token).sample.get.copy(from = address)
      for {
        _ <- erc20InfoAdapter.createEthereumToken(_token)
        transfer <- erc20TransferEventAdapter.createTransferEvent(_transfer)
      } yield transfer
    }

    // Note that the transactions linked in this test belong to different tokens.
    // The DB doesn't care.
    for {
      onchainTransfer <- writeOnchainTransfer()
      offchainTransfer <- writeTransferFrom(onchainTransfer.from)
      updatedOffchainTransfer <- offchainTransferDao.linkToOnChainTxWithId(offchainTransfer.id, onchainTransfer.id)
    } yield updatedOffchainTransfer shouldBe offchainTransfer.copy(onchainTransferId = Some(onchainTransfer.id))
  }
}
