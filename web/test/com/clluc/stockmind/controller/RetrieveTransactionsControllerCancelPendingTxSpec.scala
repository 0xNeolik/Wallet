package com.clluc.stockmind.controller

import java.util.UUID
import com.clluc.stockmind.controller.RetrieveTransactionsControllerCancelPendingTxSpec.Context
import com.clluc.stockmind.core.transaction.CancelPendingTransaction.{
  PendingTxNotFound,
  TxNotFromTheCancelActionRequester,
  ValidatedCancelPendingTransactionOp
}
import com.clluc.stockmind.port.primary.RetrieveTransactionsPort
import com.google.inject.{AbstractModule, Provides}
import net.codingwell.scalaguice.ScalaModule
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpec, Matchers}
import play.api.http.Status

import scala.concurrent.Future
import play.api.cache.SyncCacheApi
import javax.inject.Inject
import com.clluc.stockmind.core.auth.LoginInfo
import com.clluc.stockmind.core.user.{LocalDirectoryData, User}

class RetrieveTransactionsControllerCancelPendingTxSpec @Inject()(cache: SyncCacheApi)
    extends FunSpec
    with Matchers
    with Context {

  describe("Failing scenarios") {
    //Add user data caché for tests
    val sourceUser =
      User(UUID.randomUUID(), LoginInfo("11", "aa"), LocalDirectoryData(), "identifier")
    cache.set("IDTOKEN" + "profile", sourceUser)
    cache.set("IDTOKEN" + "id", sourceUser.userID)
    describe("Transaction id not valid") {
      def testFailingUseCaseForInvalidTxId(txId: String) = {

        val result = cancelTxWithId(txId)

        it("We get a bad request status code") {
          assertOnStatusCode(result)(Status.BAD_REQUEST)
        }

        it("We get an appropriate error message in the response body") {
          plainResponseBody(result) shouldBe "The transaction id has to be a 64 bits signed integer"
        }
      }

      describe("empty string") {
        testFailingUseCaseForInvalidTxId("")
      }

      describe("Not a number") {
        testFailingUseCaseForInvalidTxId("En un lugar de la mancha")
      }

      describe("Too big to be a long") {
        testFailingUseCaseForInvalidTxId(
          "24872784726348276483642736129374987428972874428736428376482374629")
      }
    }

    describe("Transaction id valid") {

      val txId = "100"

      describe("Transaction not found") {
        val fixture = Left(PendingTxNotFound(100L))
        val result  = cancelTxWithId(txId, Some(fixture))

        it("We get a not found status") {
          assertOnStatusCode(result)(Status.NOT_FOUND)
        }

        it("We get an informative message in the response body") {
          plainResponseBody(result) shouldBe "Transaction with id 100 has not been found"
        }
      }

      describe("Transaction found") {
        describe("Issuer of Tx different from current user") {
          val fixture = Left(TxNotFromTheCancelActionRequester(UUID.randomUUID(), 100L))
          val result  = cancelTxWithId(txId, Some(fixture))

          it("We get a bad request status") {
            assertOnStatusCode(result)(Status.FORBIDDEN)
          }

          it("We get an informative message in the response body") {
            plainResponseBody(result) shouldBe "You're not authorized to do that"
          }
        }
      }
    }
  }

  describe("Happy path") {
    pending
  }
}

object RetrieveTransactionsControllerCancelPendingTxSpec {

  trait Context extends FixturesContext {

    class FakeModule(
        cancelActionFixture: Option[ValidatedCancelPendingTransactionOp[Unit]]
    ) extends AbstractModule
        with ScalaModule
        with MockFactory {

      val txId = 100L

      override def configure() = {}

      @Provides
      def retrieveTxPort(): RetrieveTransactionsPort = {
        val _mock = mock[RetrieveTransactionsPort]

        val mockExpectation = (_mock.cancelPendingTransactionById _).expects(txId, userId)
        cancelActionFixture.foreach { fixture =>
          if (fixture.isRight)
            mockExpectation.returning(Future.successful(Right(())))
          else
            mockExpectation.returning(Future.successful(fixture))
        }

        _mock
      }
    }

    def cancelTxWithId(
        txId: String,
        cancelActionFixture: Option[ValidatedCancelPendingTransactionOp[Unit]] = None) = {
      callActionOnControllerInstanceWithEmptyRequest[RetrieveTransactionsController](
        new FakeModule(cancelActionFixture)
      )(_.cancelPendingTransaction(txId))
    }
  }
}
