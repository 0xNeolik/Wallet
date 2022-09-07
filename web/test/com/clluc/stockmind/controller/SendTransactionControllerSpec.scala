package com.clluc.stockmind.controller

import java.util.UUID

import com.clluc.stockmind.controller.SendTransactionControllerSpec.Context
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.UnexpectedEthereumResponse
import com.clluc.stockmind.core.auth.{LoginInfo, OAuth1Info}
import com.clluc.stockmind.core.transaction.TokenTransactionError._
import com.clluc.stockmind.core.transaction.{
  TokenTransactionError,
  TransactionIsCompleted,
  TransactionIsPending,
  TransactionResult
}
import com.clluc.stockmind.core.twitter.TwitterHandle
import com.clluc.stockmind.port.primary.{Oauth1InfoPort, SendTransactionPort}
import com.google.inject.{AbstractModule, Provides}
import net.codingwell.scalaguice.ScalaModule
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Assertion, FunSpec, Matchers}
import play.api.http.Status
import play.api.libs.json.Json.JsValueWrapper
import play.api.mvc.{Action, AnyContent, AnyContentAsJson, Result}
import play.api.test.FakeRequest

import scala.concurrent.Future
import play.api.cache.SyncCacheApi
import javax.inject.Inject
import com.clluc.stockmind.core.auth.LoginInfo
import com.clluc.stockmind.core.user.{LocalDirectoryData, User}

class SendTransactionControllerSpec @Inject()(cache: SyncCacheApi)
    extends FunSpec
    with Matchers
    with Context {
  describe("POST /v1/erc20-transactions") {
    //Add user data caché for tests
    val sourceUser =
      User(UUID.randomUUID(), LoginInfo("11", "aa"), LocalDirectoryData(), "identifier")
    cache.set("IDTOKEN" + "profile", sourceUser)
    cache.set("IDTOKEN" + "id", sourceUser.userID)

    val postData: Array[(String, JsValueWrapper)] = Array(
      "destination" -> "destination",
      "tokenSymbol" -> "tokenSymbol",
      "amount"      -> "amount"
    )

    val tx = sendTx(postData) _

    def noHappyPathTest(
        txFx: (ValidatedTransaction[TransactionResult]) => Future[Result],
    )(
        error: TokenTransactionError,
        expectedStatus: Int
    ) = {
      val sendTxResult = txFx(Left(error))

      it("We get the expected error status code") {
        assertOnStatusCode(sendTxResult)(expectedStatus)
      }

      it("We get the expected informative message") {
        assertOnResponseBody(sendTxResult, error)
      }
    }

    def assertOnResponseBody(result: Future[Result], error: TokenTransactionError): Assertion =
      plainResponseBody(result) shouldBe error.message()

    describe("When sending to a twitter handle") {

      val transfer = tx(_.transfer)(_)

      describe("If everything goes smooth") {
        describe("and the user has account ") {
          val sendTxResult = transfer(Right(TransactionIsCompleted))

          it("we get a created (201) result") {
            assertOnStatusCode(sendTxResult)(Status.CREATED)
          }

          it("The body should be empty") {
            plainResponseBody(sendTxResult) shouldBe empty
          }
        }
      }

      describe("but the destination user has no stockmind account yet") {
        val sendTxResult = transfer(Right(TransactionIsPending))

        it("we get an accepted (202) result") {
          assertOnStatusCode(sendTxResult)(Status.ACCEPTED)
        }

        it("The body should be empty") {
          plainResponseBody(sendTxResult) shouldBe empty
        }
      }

      val transferNoHappyPathTest = noHappyPathTest(transfer) _

      describe("If the sender twitter user doesn't exist") {
        val error = nonExistentTwitterUser(TwitterHandle("destination"))
        transferNoHappyPathTest(error, Status.CONFLICT)
      }

      describe("If the sender doesn't have an ethereum account in the platform") {
        val error = userDoesNotHaveEthAccountInPlatform(UUID.randomUUID(), "destination")
        transferNoHappyPathTest(error, Status.INTERNAL_SERVER_ERROR)
      }

      describe("If the token in the transfer request is not supported") {
        val error = tokenForTransferNotInPlatform("symbol")
        transferNoHappyPathTest(error, Status.CONFLICT)
      }

      describe("If the source has no balance at all") {
        val error = sourceUserHasNoBalance()
        transferNoHappyPathTest(error, Status.CONFLICT)
      }

      describe("If the source has not enough balance") {
        val error = sourceUserHasNotEnoughBalance("sourceAdd", "token", 0, 10)
        transferNoHappyPathTest(error, Status.CONFLICT)
      }

      describe("If the destination has no account at all in Twitter (not a twitter user)") {
        val error = destinationUserHasNoTwitterAccount(TwitterHandle("hey"))
        transferNoHappyPathTest(error, Status.CONFLICT)
      }

      describe("If the sender has no valid twitter credentials") {
        val error = twitterCredentialsForTransferSenderNotValid(userId)
        transferNoHappyPathTest(error, Status.INTERNAL_SERVER_ERROR)
      }

      describe(
        "If for some reason after a pending transfer the tweet to the recipient cannot be sent") {

        val error        = tweetToRecipientNotSent(new Exception())
        val sendTxResult = transfer(Left(error))

        it("We accept the operation as valid and returned an accepted status code") {
          assertOnStatusCode(sendTxResult)(Status.ACCEPTED)
        }

        it("We return anyway a message in the body as a warning") {
          assertOnResponseBody(sendTxResult, error)
        }
      }

      describe("If the source of the transfer doesn't exist") {
        val error = transferSourceUserDoesNotExist(userId)
        transferNoHappyPathTest(error, Status.INTERNAL_SERVER_ERROR)
      }

      describe(
        "When the authenticated user does not have a twitter user id registered in the system") {
        val error = notUserWithLoginKey("twitter", "0193836554")
        transferNoHappyPathTest(error, Status.INTERNAL_SERVER_ERROR)
      }

      describe("If a given Stockmind user doesn't have a Twitter account") {
        val error = noTwitterAccountForStockmindUser(userId)
        transferNoHappyPathTest(error, Status.INTERNAL_SERVER_ERROR)
      }

      describe("If a twitter account exists for a user but stockmind has no information about it") {
        val error = twitterAccountNotLinkedToStockMind("gotoalberto")
        transferNoHappyPathTest(error, Status.INTERNAL_SERVER_ERROR)
      }

      describe("If there's an issue when communicating with the ethereum node") {
        val error = ethereumIssue(UnexpectedEthereumResponse("Baaaaad", 200))
        transferNoHappyPathTest(error, Status.INTERNAL_SERVER_ERROR)
      }

      describe("If we try to create a pending transaction with metadata") {
        val error = metaInfoNotAllowedInPendingTransfers()
        transferNoHappyPathTest(error, Status.BAD_REQUEST)
      }
    }
  }

  describe("POST /v1/erc721-transactions") {
    //Add user data caché for tests
    val sourceUser =
      User(UUID.randomUUID(), LoginInfo("11", "aa"), LocalDirectoryData(), "identifier")
    cache.set("IDTOKEN" + "profile", sourceUser)
    cache.set("IDTOKEN" + "id", sourceUser.userID)

    val postData: Array[(String, JsValueWrapper)] = Array(
      "destination" -> "destination",
      "id"          -> "71120786848863412851373030999642871879808768922518165984257232620739138279176"
    )

    val tx = sendTx(postData) _

    def noHappyPathTest(
        txFx: (ValidatedTransaction[TransactionResult]) => Future[Result],
    )(
        error: TokenTransactionError,
        expectedStatus: Int
    ) = {
      val sendTxResult = txFx(Left(error))

      it("We get the expected error status code") {
        assertOnStatusCode(sendTxResult)(expectedStatus)
      }

      it("We get the expected informative message") {
        assertOnResponseBody(sendTxResult, error)
      }
    }

    def assertOnResponseBody(result: Future[Result], error: TokenTransactionError): Assertion =
      plainResponseBody(result) shouldBe error.message()

    describe("When sending to a twitter handle") {

      val transfer = tx(_.transfer721)(_)

      describe("If everything goes smooth") {
        describe("and the user has account ") {
          val sendTxResult = transfer(Right(TransactionIsCompleted))

          it("we get a created (201) result") {
            assertOnStatusCode(sendTxResult)(Status.CREATED)
          }

          it("The body should be empty") {
            plainResponseBody(sendTxResult) shouldBe empty
          }
        }
      }

      describe("but the destination user has no stockmind account yet") {
        val sendTxResult = transfer(Right(TransactionIsPending))

        it("we get an accepted (202) result") {
          assertOnStatusCode(sendTxResult)(Status.ACCEPTED)
        }

        it("The body should be empty") {
          plainResponseBody(sendTxResult) shouldBe empty
        }
      }

      val transferNoHappyPathTest = noHappyPathTest(transfer) _

      describe("If the sender twitter user doesn't exist") {
        val error = nonExistentTwitterUser(TwitterHandle("destination"))
        transferNoHappyPathTest(error, Status.CONFLICT)
      }

      describe("If the sender doesn't have an ethereum account in the platform") {
        val error = userDoesNotHaveEthAccountInPlatform(UUID.randomUUID(), "destination")
        transferNoHappyPathTest(error, Status.INTERNAL_SERVER_ERROR)
      }

      describe("If the token in the transfer request is not supported") {
        val error = tokenForTransferNotInPlatform("symbol")
        transferNoHappyPathTest(error, Status.CONFLICT)
      }

      describe("If the source has no balance at all") {
        val error = sourceUserHasNoBalance()
        transferNoHappyPathTest(error, Status.CONFLICT)
      }

      describe("If the source has not enough balance") {
        val error = sourceUserHasNotEnoughBalance("sourceAdd", "token", 0, 10)
        transferNoHappyPathTest(error, Status.CONFLICT)
      }

      describe("If the destination has no account at all in Twitter (not a twitter user)") {
        val error = destinationUserHasNoTwitterAccount(TwitterHandle("hey"))
        transferNoHappyPathTest(error, Status.CONFLICT)
      }

      describe("If the sender has no valid twitter credentials") {
        val error = twitterCredentialsForTransferSenderNotValid(userId)
        transferNoHappyPathTest(error, Status.INTERNAL_SERVER_ERROR)
      }

      describe(
        "If for some reason after a pending transfer the tweet to the recipient cannot be sent") {

        val error        = tweetToRecipientNotSent(new Exception())
        val sendTxResult = transfer(Left(error))

        it("We accept the operation as valid and returned an accepted status code") {
          assertOnStatusCode(sendTxResult)(Status.ACCEPTED)
        }

        it("We return anyway a message in the body as a warning") {
          assertOnResponseBody(sendTxResult, error)
        }
      }

      describe("If the source of the transfer doesn't exist") {
        val error = transferSourceUserDoesNotExist(userId)
        transferNoHappyPathTest(error, Status.INTERNAL_SERVER_ERROR)
      }

      describe(
        "When the authenticated user does not have a twitter user id registered in the system") {
        val error = notUserWithLoginKey("twitter", "0193836554")
        transferNoHappyPathTest(error, Status.INTERNAL_SERVER_ERROR)
      }

      describe("If a given Stockmind user doesn't have a Twitter account") {
        val error = noTwitterAccountForStockmindUser(userId)
        transferNoHappyPathTest(error, Status.INTERNAL_SERVER_ERROR)
      }

      describe("If a twitter account exists for a user but stockmind has no information about it") {
        val error = twitterAccountNotLinkedToStockMind("gotoalberto")
        transferNoHappyPathTest(error, Status.INTERNAL_SERVER_ERROR)
      }

      describe("If there's an issue when communicating with the ethereum node") {
        val error = ethereumIssue(UnexpectedEthereumResponse("Baaaaad", 200))
        transferNoHappyPathTest(error, Status.INTERNAL_SERVER_ERROR)
      }

      describe("If we try to create a pending transaction with metadata") {
        val error = metaInfoNotAllowedInPendingTransfers()
        transferNoHappyPathTest(error, Status.BAD_REQUEST)
      }
    }
  }
  // As both transfers and withdrawals share the same logic at controller level (differences are encapsulated by
  // the corresponding primary port) testing only transfer calls suffice.

}

private[controller] object SendTransactionControllerSpec {

  trait Context extends FixturesContext with MockFactory {

    class FakeModule(txResult: ValidatedTransaction[TransactionResult])
        extends AbstractModule
        with ScalaModule {
      override def configure() = {}

      @Provides
      def sendTxPort(): SendTransactionPort = {
        val _mock = mock[SendTransactionPort]
        (_mock.sendTransaction _)
          .expects(*, *, *, *, *)
          .returning(Future.successful(txResult))
          .noMoreThanOnce()
        _mock
      }

      @Provides
      def oauth1InfoPort(): Oauth1InfoPort = {
        val _mock = mock[Oauth1InfoPort]

        (_mock.find _)
          .expects(LoginInfo(loginInfo.providerID, loginInfo.providerKey))
          .returning(Future.successful(Some(OAuth1Info(oauth1Info.token, oauth1Info.secret))))
          .noMoreThanOnce()

        _mock
      }
    }

    def sendTx(
        postData: Array[(String, JsValueWrapper)],
    )(
        action: SendTransactionController => Action[AnyContent]
    )(
        txResult: ValidatedTransaction[TransactionResult]
    ): Future[Result] = {
      callActionOnControllerInstance[SendTransactionController, AnyContentAsJson](
        new FakeModule(txResult)
      )(
        FakeRequest()
          .withMethod("POST")
          .withJsonBody(
            play.api.libs.json.Json.obj(postData: _*)
          )
      )(action)
    }
  }
}
