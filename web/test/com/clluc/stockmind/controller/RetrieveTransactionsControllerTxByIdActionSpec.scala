package com.clluc.stockmind.controller

import java.util.UUID

import com.clluc.stockmind.controller.RetrieveTransactionsController.TransactionView
import com.clluc.stockmind.controller.RetrieveTransactionsControllerTxByIdActionSpec.Context
import com.clluc.stockmind.core.transaction.{
  Counterparty,
  OutgoingTx,
  StockmindTransaction,
  TokenAmount
}
import com.clluc.stockmind.port.primary.RetrieveTransactionsPort
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.{AuthInfo, LoginInfo}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import io.circe.ParsingFailure
import net.codingwell.scalaguice.ScalaModule
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.scalatest.{EitherValues, FunSpec, Matchers}
import play.api.http.Status
import play.api.mvc.Result

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Random
import play.api.cache.SyncCacheApi
import javax.inject.Inject
import com.clluc.stockmind.core.auth.{LoginInfo => LoginInfoAuth}
import com.clluc.stockmind.core.user.{LocalDirectoryData, User}

/**
  * Test that will cover all methods in RetrieveTransactionsController that receive a transaction id
  * as parameter and ask for information regarding the transaction with that id.
  */
class RetrieveTransactionsControllerTxByIdActionSpec @Inject()(cache: SyncCacheApi)
    extends FunSpec
    with Matchers
    with Context
    with EitherValues {

  //Add user data caché for tests
  val sourceUser =
    User(UUID.randomUUID(), LoginInfoAuth("11", "aa"), LocalDirectoryData(), "identi")
  cache.set("IDTOKEN" + "profile", sourceUser)
  cache.set("IDTOKEN" + "id", sourceUser.userID)

  private def genericBadTxIdTestCase(
      txId: String
  )(
      findFx: (Option[StockmindTransaction], String) => Future[Result]
  ) = {
    val result: Future[Result] = findFx(None, txId)

    it("The status is bad request") {
      assertOnStatusCode(result)(Status.BAD_REQUEST)
    }

    it("The message in the response explain what happened") {
      plainResponseBody(result) shouldBe "The transaction id has to be a 64 bits signed integer"
    }
  }

  private def genericTxNotFoundTestCase(
      findFx: (Option[StockmindTransaction], String) => Future[Result]
  ) = {
    val txIdPar = "10"
    val result  = findFx(None, txIdPar)

    it("We get a not found status code") {
      assertOnStatusCode(result)(Status.NOT_FOUND)
    }

    it("We get a response message that tells what's going on") {
      plainResponseBody(result) shouldBe s"Transaction with id $txIdPar has not been found"
    }
  }

  private def genericTxFoundTestCase(
      findFx: (Option[StockmindTransaction], String) => Future[Result]
  ) = {

    val now  = DateTime.now()
    val txId = Random.nextLong()

    val transactionFixture = StockmindTransaction(
      id = txId,
      direction = OutgoingTx,
      pending = false,
      counterparty = Counterparty(
        None,
        None
      ),
      token = "XXX",
      erc_type = "ERC-20",
      tokenDescription = "Some token",
      decimals = 10,
      amount = TokenAmount(
        "234",
        "32536"
      ),
      txHash = None,
      date = now
    )

    val transactionViewFixture = TransactionView(
      id = txId.toString,
      direction = "send",
      pending = false,
      counterparty = Counterparty(
        None,
        None
      ),
      token = "XXX",
      decimals = 10,
      amount = TokenAmount(
        "234",
        "32536"
      ),
      txHash = None,
      date = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss").print(now)
    )

    val result = findFx(Some(transactionFixture), "10")

    it("We get an Ok status code") {
      assertOnStatusCode(result)(Status.OK)
    }

    it("We get the transaction as JSON") {
      import io.circe.generic.auto._
      import io.circe.syntax._

      val actualJson: Either[ParsingFailure, String] =
        resultAsJsonStringWithoutNulls(result)

      val expectedJson = jsonToStringWithoutNulls(transactionViewFixture.asJson)

      actualJson.right.value shouldBe expectedJson
    }
  }

  describe("Find transaction by id") {
    describe("Transaction id not valid") {

      def badTxIdTestCase(txId: String) =
        genericBadTxIdTestCase(txId)(getTransaction)

      describe("empty string") {
        badTxIdTestCase("")
      }

      describe("Not a number") {
        badTxIdTestCase("This is not a number")
      }

      describe("Too big to be a long") {
        badTxIdTestCase("725348734902398429382983742873692384927832374827323742837827")
      }
    }

    describe("Transaction id valid") {
      describe("Transaction not found") {
        genericTxNotFoundTestCase(getTransaction)
      }

      describe("Transaction found") {
        genericTxFoundTestCase(getTransaction)
      }
    }
  }

  describe("Find pending transaction by id") {
    describe("Transaction id not valid") {
      def badPendingTxIdTestCase(txId: String) = genericBadTxIdTestCase(txId)(getPendingTransaction)

      describe("empty string") {
        badPendingTxIdTestCase("")
      }

      describe("Not a number") {
        badPendingTxIdTestCase("This is not a number")
      }

      describe("Too big to be a long") {
        badPendingTxIdTestCase("725348734902398429382983742873692384927832374827323742837827")
      }
    }

    describe("Transaction id valid") {
      describe("Transaction not found") {
        genericTxNotFoundTestCase(getPendingTransaction)
      }

      describe("Transaction found") {
        genericTxFoundTestCase(getPendingTransaction)
      }
    }
  }
}

private[controller] object RetrieveTransactionsControllerTxByIdActionSpec {

  trait Context extends FixturesContext {

    class FakeModule(transactionFixture: Option[StockmindTransaction])
        extends AbstractModule
        with ScalaModule {
      override def configure() = {}

      @Provides
      def retrieveTransactionsPort(): RetrieveTransactionsPort = new RetrieveTransactionsPort {
        override def cancelPendingTransactionById(txId: Long, transactionOwnerUserId: UUID) = ???
        override def findPendingTransactionById(txId: Long) =
          Future.successful(transactionFixture)
        override def findTransactionById(userId: UUID, txId: Long) =
          Future.successful(transactionFixture)
        override def findTransactionsPage(userId: UUID, offset: Int, numOfTxs: Int)    = ???
        override def find721TransactionsPage(userId: UUID, offset: Int, numOfTxs: Int) = ???
        override def find721TransactionById(userId: UUID, txId: Long) =
          ???
        override def findPending721TransactionById(txId: Long) =
          ???
      }

      @Provides
      def authInfoRepository(): AuthInfoRepository = new AuthInfoRepository {
        override def find[T <: AuthInfo](loginInfo: LoginInfo)(implicit tag: ClassTag[T]) =
          Future.successful(Some(oauth1Info.asInstanceOf[T]))
        override def add[T <: AuthInfo](loginInfo: LoginInfo, authInfo: T)                  = ???
        override def update[T <: AuthInfo](loginInfo: LoginInfo, authInfo: T)               = ???
        override def save[T <: AuthInfo](loginInfo: LoginInfo, authInfo: T)                 = ???
        override def remove[T <: AuthInfo](loginInfo: LoginInfo)(implicit tag: ClassTag[T]) = ???
      }
    }

    private def genericGetTransaction(transactionFixture: Option[StockmindTransaction],
                                      txIdParameter: String) =
      callActionOnControllerInstanceWithEmptyRequest[RetrieveTransactionsController](
        new FakeModule(transactionFixture)
      )(_)

    def getTransaction(transactionFixture: Option[StockmindTransaction], txIdParameter: String) =
      genericGetTransaction(transactionFixture, txIdParameter)(_.getTransaction(txIdParameter))

    def getPendingTransaction(transactionFixture: Option[StockmindTransaction],
                              txIdParameter: String) =
      genericGetTransaction(transactionFixture, txIdParameter)(
        _.getPendingTransaction(txIdParameter))
  }
}
