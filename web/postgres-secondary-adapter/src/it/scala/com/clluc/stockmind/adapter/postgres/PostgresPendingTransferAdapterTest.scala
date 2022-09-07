package com.clluc.stockmind.adapter.postgres

import java.util.UUID

import com.clluc.stockmind.adapter.postgres.Generators._
import com.clluc.stockmind.core.auth.LoginInfo
import com.clluc.stockmind.core.transaction.PendingTransfer
import org.joda.time.{DateTime, DateTimeZone}
import org.postgresql.util.PSQLException
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.Future

class PostgresPendingTransferAdapterTest extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  val userDao = new PostgresUsersRepositoryAdapter(testTransactor)
  val erc20TokenDao = new PostgresErc20InfoAdapter(testTransactor)
  val pendingTransferDao = new PostgresPendingTransferAdapter(testTransactor)

  override def afterAll() = TableCleaner.clean()

  behavior of "PostgresPendingTransferAdapter"

  it should "fail when creating a PendingTransfer with a non-existing sender" in {
    val user = genUser.sample.get
    val token = genErc20Token.sample.get
    val pendingTransfer = genPendingTransfer(user.userID, token.symbol).sample.get
    recoverToSucceededIf[PSQLException] {
      erc20TokenDao.createEthereumToken(token).flatMap { _ =>
        pendingTransferDao.create(pendingTransfer)
      }
    }
  }

  it should "fail when creating a PendingTransfer with a non-existing token" in {
    val user = genUser.sample.get
    val token = genErc20Token.sample.get
    val pendingTransfer = genPendingTransfer(user.userID, token.symbol).sample.get
    recoverToSucceededIf[PSQLException] {
      userDao.save(user).flatMap { _ =>
        pendingTransferDao.create(pendingTransfer)
      }
    }
  }

  private def writePendingTransfer: Future[PendingTransfer] = {
    genericWritePendingTransfer(identity)
  }

  private def writeProcessedPendingTransfer(processDate: DateTime): Future[PendingTransfer] = {
    genericWritePendingTransfer(
      _.copy(
        processed = Some(processDate)
      )
    )
  }

  private def genericWritePendingTransfer(txFx: PendingTransfer => PendingTransfer): Future[PendingTransfer] = {
    val user = genUser.sample.get
    val token = genErc20Token.sample.get
    val pendingTransfer = genPendingTransfer(user.userID, token.symbol).sample.get
    for {
      _ <- userDao.save(user)
      _ <- erc20TokenDao.createEthereumToken(token)
      pendingTransfer <- pendingTransferDao.create(txFx(pendingTransfer))
    } yield pendingTransfer
  }

  it should "store a PendingTransfer" in {
    writePendingTransfer.map(_ => succeed)
  }

  it should "not find pending transfers by ID when there are none" in {
    for {
      pt <- writePendingTransfer
      found <- pendingTransferDao.findById(1234L)
    } yield found shouldBe None
  }

  it should "find pending transfers by ID" in {
    for {
      pt <- writePendingTransfer
      found <- pendingTransferDao.findById(pt.id)
    } yield found shouldBe Some(pt)
  }

  it should "not find pending transfers by origin when there are none" in {
    for {
      pt <- writePendingTransfer
      found <- pendingTransferDao.findByOrigin(UUID.randomUUID())
    } yield found shouldBe List()
  }

  it should "find pending transfers by origin" in {
    for {
      pt <- writePendingTransfer
      found <- pendingTransferDao.findByOrigin(pt.fromUser)
    } yield found shouldBe List(pt)
  }

  it should "not find pending transfers by destination when there are none" in {
    for {
      pt <- writePendingTransfer
      found <- pendingTransferDao.findPendingByDestination(LoginInfo("not", "stored"))
    } yield found shouldBe List()
  }

  it should "find pending transfers by destination" in {
    for {
      pt <- writePendingTransfer
      found <- pendingTransferDao.findPendingByDestination(pt.toFutureUser)
    } yield found shouldBe List(pt)
  }

  it should "find processed pending transfers by origin" in {
    for {
      pt <- writeProcessedPendingTransfer(DateTime.now())
      found <- pendingTransferDao.findPendingByOrigin(pt.fromUser)
    } yield found shouldBe List(pt)
  }

  it should "mark a pending transfer as processed" in {
    for {
      pt <- writePendingTransfer
      processed <- pendingTransferDao.markAsProcessed(pt.id, DateTime.now(DateTimeZone.UTC))
    } yield {
      processed.copy(processed = None) shouldBe pt
      processed.processed should not be empty
    }
  }

  it should "delete a pending transfer" in {
    for {
      pt <- writePendingTransfer
      _ <- pendingTransferDao.delete(pt.id)
      found <- pendingTransferDao.findById(pt.id)
    } yield found shouldBe None

  }
}
