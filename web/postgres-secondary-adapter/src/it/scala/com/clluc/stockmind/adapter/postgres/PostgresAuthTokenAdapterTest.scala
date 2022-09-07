package com.clluc.stockmind.adapter.postgres

import java.util.UUID

import com.clluc.stockmind.adapter.postgres.Generators._
import com.clluc.stockmind.core.user.AuthToken
import org.joda.time.DateTime
import org.postgresql.util.PSQLException
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.Future

class PostgresAuthTokenAdapterTest extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  override def afterAll() = TableCleaner.clean()

  val pgUsers = new PostgresUsersRepositoryAdapter(testTransactor)
  val pgAuthTokens = new PostgresAuthTokenAdapter(() => DateTime.now(), testTransactor)

  private def createValidAuthToken = {
    val userToSave = genUser.sample.get
    val authTokenToSave = genAuthToken(userToSave.userID).sample.get
    (userToSave, authTokenToSave)
  }

  private def writeToken: Future[AuthToken] = {
    val (userToSave, authTokenToSave) = createValidAuthToken
    for {
      u <- pgUsers.save(userToSave)
      t <- pgAuthTokens.saveAuthToken(authTokenToSave)
    } yield t
  }

  behavior of "PostgresAuthTokenAdapter"

  it should "fail when creating a token referencing a non-existing user" in {
    recoverToSucceededIf[PSQLException] {
      val (userToSave, authTokenToSave) = createValidAuthToken
      for {
        u <- pgUsers.save(userToSave)
        t <- pgAuthTokens.saveAuthToken(authTokenToSave.copy(userID = UUID.randomUUID()))
      } yield t
    }
  }

  it should "create a valid token correctly" in {
    writeToken.map(_ => succeed)
  }

  it should "remove an existing token" in {
    for {
      token <- writeToken
      removedToken <- pgAuthTokens.removeAuthTokenWithId(token.id)
      foundToken <- pgAuthTokens.findAuthTokenByUserId(token.id)
    } yield foundToken shouldBe None
  }

  it should "not find a non-existing token" in {
    writeToken.flatMap { authToken =>
      pgAuthTokens.findAuthTokenByUserId(java.util.UUID.randomUUID).map(_ shouldBe None)
    }
  }

  it should "find an existing token by id" in {
    writeToken.flatMap { authToken =>
      pgAuthTokens.findAuthTokenByUserId(authToken.id).map(_ shouldBe Some(authToken))
    }
  }

  it should "not find expired tokens when there are not any" in {
    TableCleaner.deleteAuthTokens
    writeToken.flatMap { authToken =>
      pgAuthTokens.findAuthTokensExpired(authToken.expiry.minus(1)).map(_ shouldBe empty)
    }
  }

  it should "find all existing expired tokens" in {
    TableCleaner.deleteAuthTokens
    def isAfter(x: DateTime, y: DateTime) = if (x.isAfter(y)) x else y
    val numberOfTokens = 42
    for {
      tokens <- Future.sequence(List.fill(numberOfTokens)(writeToken))
      maxDateTime = tokens.map(_.expiry).reduceLeft(isAfter)
      expiredTokens <- pgAuthTokens.findAuthTokensExpired(maxDateTime.plus(1))
    } yield expiredTokens.length shouldBe numberOfTokens
  }

}
