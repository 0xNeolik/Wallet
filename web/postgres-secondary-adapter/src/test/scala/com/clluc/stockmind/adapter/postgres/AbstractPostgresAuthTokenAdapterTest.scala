package com.clluc.stockmind.adapter.postgres

import java.util.UUID

import com.clluc.stockmind.core.user.AuthToken
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class AbstractPostgresAuthTokenAdapterTest extends FlatSpec with Matchers {

  implicit val _executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  trait Fixture {
    val baseDate = DateTime.parse("2017-01-15T13:30Z")

    def pgAuthTokens(empty: Boolean = false) = new AbstractPostgresAuthTokenAdapter {

      override val clock = () => baseDate

      override def removeAuthTokenWithId(id: UUID): Future[Unit] =
        Future.successful(Unit)

      override def findAuthTokensExpired(dateTime: DateTime): Future[Seq[AuthToken]] = {
        val token = AuthToken(UUID.randomUUID(), UUID.randomUUID(), dateTime)
        Future.successful(List(token, token, token))
      }

      override def saveAuthToken(token: AuthToken): Future[AuthToken] =
        Future.successful(token)

      override def findAuthTokenByUserId(id: UUID): Future[Option[AuthToken]] =
        Future.successful {
          if (empty) {
            None
          } else {
            Some(AuthToken(id, UUID.randomUUID(), baseDate))
          }
        }

      override implicit val executionContext: ExecutionContext = _executionContext
    }
  }

  behavior of "AbstractPostgresAuthTokenAdapter"

  it should "create a new AuthToken" in new Fixture {
    val userId        = UUID.randomUUID()
    val expiry        = 5.minutes
    val expectedToken = AuthToken(UUID.randomUUID(), userId, baseDate.plus(expiry.toMillis))
    for {
      createdToken <- pgAuthTokens().createAuthTokenForUser(userId, expiry)
    } yield {
      createdToken.copy(id = expectedToken.id) shouldBe expectedToken
    }
  }

  it should "validate an existing token" in new Fixture {
    val tokenId = UUID.randomUUID()
    for {
      validatedTokenO <- pgAuthTokens().validateAuthTokenForUser(tokenId)
      validatedToken = validatedTokenO.get
    } yield {
      validatedToken.id shouldBe tokenId
    }

  }

  it should "not validate a non-existing token" in new Fixture {
    val tokenId = UUID.randomUUID()
    for {
      validatedTokenO <- pgAuthTokens(empty = true).validateAuthTokenForUser(tokenId)
    } yield {
      validatedTokenO shouldBe None
    }

  }

  it should "clean all expired tokens" in new Fixture {
    for {
      removedTokens <- pgAuthTokens().cleanExpiredAuthTokens()
    } yield {
      removedTokens.length shouldBe 3
    }

  }

}
