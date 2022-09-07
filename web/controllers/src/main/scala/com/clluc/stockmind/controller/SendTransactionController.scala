package com.clluc.stockmind.controller

import java.util.UUID

import javax.inject.Inject
import com.clluc.stockmind.controller.SendTransactionController.{
  SendTransaction721PostData,
  SendTransactionPostData
}
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.transaction._
import com.clluc.stockmind.core.twitter.TwitterHandle
import com.clluc.stockmind.core.user.EmailHandle
import com.clluc.stockmind.port.primary._
import com.google.inject.name.Named
//import com.mohiva.play.silhouette.api.Silhouette
//import com.mohiva.play.silhouette.api.actions.SecuredRequest
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
//import com.mohiva.play.silhouette.impl.providers

import io.circe.generic.auto._
import io.circe.parser.decode
import org.joda.time.DateTime
import play.api.cache.SyncCacheApi
import play.api.mvc._
import com.clluc.stockmind.port.primary.Oauth1InfoPort

import scala.concurrent.{ExecutionContext, Future}

class SendTransactionController @Inject()(
    // val silhouette: Silhouette[DefaultEnv],
    val controllerComponents: ControllerComponents,
    transactionPort: SendTransactionPort,
    cache: SyncCacheApi,
    socAuthcontroller: SocialAuthController,
    authInfoRepository: AuthInfoRepository,
    @Named("masterAccountAddress") masterAccountAddress: Address,
    @Named("masterAccountPassword") masterAccountPassword: String,
    @Named("auth0Method") auth0Method: String,
    timestampFx: => DateTime,
    auth1InfoPort: Oauth1InfoPort,
    usersPort: UserPort
)(
    implicit
    executionContext: ExecutionContext
) extends BaseController
    with PrimaryPortCallResultHandler
    with ErrorToResultConversions {

  def transfer = socAuthcontroller.AuthenticatedAction { idToken => implicit request =>
    val userID = cache.get[UUID](idToken + "id").get
    auth0Method match {
      case "TWITTER"   => transaction(userID, dest => Right(TwitterHandle(dest)))
      case "USER_PASS" => transactionUser(userID, dest => Right(EmailHandle(dest)))
    }
  }

  def transfer721 = socAuthcontroller.AuthenticatedAction { idToken => implicit request =>
    val userID = cache.get[UUID](idToken + "id").get
    auth0Method match {
      case "TWITTER"   => transaction721(userID, dest => Right(TwitterHandle(dest)))
      case "USER_PASS" => transactionUser721(userID, dest => Right(EmailHandle(dest)))
    }
  }

  def withdraw = socAuthcontroller.AuthenticatedAction { idToken => implicit request =>
    val userID = cache.get[UUID](idToken + "id").get
    auth0Method match {
      case "TWITTER"   => transaction(userID, dest => Left(Address(dest)))
      case "USER_PASS" => transactionUser(userID, dest => Left(Address(dest)))
    }
  }

  def withdraw721 = socAuthcontroller.AuthenticatedAction { idToken => implicit request =>
    val userID = cache.get[UUID](idToken + "id").get
    auth0Method match {
      case "TWITTER"   => transaction721(userID, dest => Left(Address(dest)))
      case "USER_PASS" => transactionUser721(userID, dest => Left(Address(dest)))
    }
  }

  private def transaction(userID: UUID,
                          destinationProducer: String => Either[Address, TwitterHandle])(
      implicit request: Request[AnyContent]
  ): Future[Result] = {

    val maybeBodyText: Option[String] = request.body.asJson.map(_.toString)

    val maybeBodyJson: Option[Either[RequestParsingError, SendTransactionPostData]] =
      maybeBodyText.map(SendTransactionController.parseRequestContent)

    def auxTransaction(sendTransactionPostData: SendTransactionPostData): Future[Result] = {
      val transactionRequest = TransactionRequest(
        userID,
        masterAccountAddress,
        destinationProducer(sendTransactionPostData.destination),
        sendTransactionPostData.tokenSymbol,
        "ERC-20",
        sendTransactionPostData.amount,
        sendTransactionPostData.metaInf
      )

      val transactionResult: Future[Either[TokenTransactionError, TransactionResult]] =
        for {
          user     <- usersPort.findFromId(userID)
          auth1nfo <- auth1InfoPort.find(user.get.loginInfo)
          txResult <- transactionPort.sendTransaction(transactionRequest,
                                                      auth1nfo.get.token,
                                                      auth1nfo.get.secret,
                                                      timestampFx,
                                                      masterAccountPassword)
        } yield txResult

      def leftFx(error: TokenTransactionError): Result = {

        val response = error match {
          case e: NonExistentTwitterUser                      => Conflict
          case e: UserDoesNotHaveEthAccountInPlatform         => InternalServerError
          case e: TokenForTransferNotInPlatform               => Conflict
          case e: SourceUserHasNoBalance.type                 => Conflict
          case e: SpecifiedAmountAsStringNotValid             => BadRequest
          case e: SourceUserHasNotEnoughBalance               => Conflict
          case e: DestinationUserHasNoTwitterAccount          => Conflict
          case e: TwitterCredentialsForTransferSenderNotValid => InternalServerError
          case e: TweetToRecipientNotSend =>
            Accepted // Special case in which we only warn, but not fail
          case e: TransferSourceUserDoesNotExist     => InternalServerError
          case e: NoUserWithLoginKey                 => InternalServerError
          case e: TriedWithdrawToInvalidAccount      => Conflict
          case e: NoEthereumAccountForAddress        => InternalServerError
          case e: NoTwitterAccountForStockmindUser   => InternalServerError
          case e: TwitterAccountNotLinkedToStockmind => InternalServerError
          case e: EthereumIssue                      => InternalServerError
          // TODO Write unit test for the following use cases
          case e: ResultingEthereumTxHashNotValid           => InternalServerError
          case e: MetaInfoNotAllowedInPendingTransfers.type => BadRequest
          case x =>
            throw new RuntimeException(
              s"This is a bug. Got $x as result of the send transaction logic. That should not happen")
        }

        response(error.message())
      }

      def rightFx(transactionResult: TransactionResult): Result =
        transactionResult match {
          case TransactionIsCompleted => Created
          case TransactionIsPending   => Accepted
        }

      transactionResult.map {
        case Left(error) => leftFx(error)
        case Right(tr)   => rightFx(tr)
      }
    }

    doCallAndHandleResult(maybeBodyJson)(auxTransaction)
  }

  private def transactionUser(userID: UUID,
                              destinationProducer: String => Either[Address, EmailHandle])(
      implicit request: Request[AnyContent]
  ): Future[Result] = {

    val maybeBodyText: Option[String] = request.body.asJson.map(_.toString)

    val maybeBodyJson: Option[Either[RequestParsingError, SendTransactionPostData]] =
      maybeBodyText.map(SendTransactionController.parseRequestContent)

    def auxTransaction(sendTransactionPostData: SendTransactionPostData): Future[Result] = {

      val transactionRequest = TransactionRequestUser(
        userID,
        masterAccountAddress,
        destinationProducer(sendTransactionPostData.destination),
        sendTransactionPostData.tokenSymbol,
        "ERC-20",
        sendTransactionPostData.amount,
        sendTransactionPostData.metaInf
      )

      val transactionResult: Future[Either[TokenTransactionError, TransactionResult]] =
        transactionPort.sendTransactionUser(
          transactionRequest,
          timestampFx,
          masterAccountPassword
        )
      //  }

      def leftFx(error: TokenTransactionError): Result = {

        val response = error match {
          case e: NonExistentUser                               => Conflict
          case e: UserDoesNotHaveEthAccountInPlatformIdentifier => InternalServerError
          case e: TokenForTransferNotInPlatform                 => Conflict
          case e: SourceUserHasNoBalance.type                   => Conflict
          case e: SpecifiedAmountAsStringNotValid               => BadRequest
          case e: SourceUserHasNotEnoughBalance                 => Conflict
          case e: DestinationUserHasNoTwitterAccount            => Conflict
          case e: TransferSourceUserDoesNotExist                => InternalServerError
          case e: NoUserWithLoginKey                            => InternalServerError
          case e: TriedWithdrawToInvalidAccount                 => Conflict
          case e: NoEthereumAccountForAddress                   => InternalServerError
          case e: EthereumIssue                                 => InternalServerError
          // TODO Write unit test for the following use cases
          case e: ResultingEthereumTxHashNotValid           => InternalServerError
          case e: MetaInfoNotAllowedInPendingTransfers.type => BadRequest
          case x =>
            throw new RuntimeException(
              s"This is a bug. Got $x as result of the send transaction logic. That should not happen")
        }

        response(error.message())
      }

      def rightFx(transactionResult: TransactionResult): Result =
        transactionResult match {
          case TransactionIsCompleted => Created
          case TransactionIsPending   => Accepted
        }

      transactionResult.map {
        case Left(error) => leftFx(error)
        case Right(tr)   => rightFx(tr)
      }
    }

    doCallAndHandleResult(maybeBodyJson)(auxTransaction)
  }

  private def transaction721(userID: UUID,
                             destinationProducer: String => Either[Address, TwitterHandle])(
      implicit request: Request[AnyContent]
  ): Future[Result] = {

    val maybeBodyText: Option[String] = request.body.asJson.map(_.toString)

    val maybeBodyJson: Option[Either[RequestParsingError, SendTransaction721PostData]] =
      maybeBodyText.map(SendTransactionController.parseRequest721Content)

    def auxTransaction(sendTransactionPostData: SendTransaction721PostData): Future[Result] = {
      val transactionRequest = TransactionRequest721(
        userID,
        masterAccountAddress,
        destinationProducer(sendTransactionPostData.destination),
        BigInt(sendTransactionPostData.id),
        sendTransactionPostData.metaInf
      )

      val transactionResult: Future[Either[TokenTransactionError, TransactionResult]] =
        for {
          user     <- usersPort.findFromId(userID)
          auth1nfo <- auth1InfoPort.find(user.get.loginInfo)
          txResult <- transactionPort.sendTransaction721(transactionRequest,
                                                         auth1nfo.get.token,
                                                         auth1nfo.get.secret,
                                                         timestampFx,
                                                         masterAccountPassword)
        } yield txResult

      def leftFx(error: TokenTransactionError): Result = {

        val response = error match {
          case e: NonExistentTwitterUser                      => Conflict
          case e: UserDoesNotHaveEthAccountInPlatform         => InternalServerError
          case e: TokenForTransferNotInPlatform               => Conflict
          case e: SourceUserHasNoBalance.type                 => Conflict
          case e: SpecifiedAmountAsStringNotValid             => BadRequest
          case e: SourceUserHasNotEnoughBalance               => Conflict
          case e: DestinationUserHasNoTwitterAccount          => Conflict
          case e: TwitterCredentialsForTransferSenderNotValid => InternalServerError
          case e: TweetToRecipientNotSend =>
            Accepted // Special case in which we only warn, but not fail
          case e: TransferSourceUserDoesNotExist     => InternalServerError
          case e: NoUserWithLoginKey                 => InternalServerError
          case e: TriedWithdrawToInvalidAccount      => Conflict
          case e: NoEthereumAccountForAddress        => InternalServerError
          case e: NoTwitterAccountForStockmindUser   => InternalServerError
          case e: TwitterAccountNotLinkedToStockmind => InternalServerError
          case e: EthereumIssue                      => InternalServerError
          // TODO Write unit test for the following use cases
          case e: ResultingEthereumTxHashNotValid           => InternalServerError
          case e: MetaInfoNotAllowedInPendingTransfers.type => BadRequest
          case x =>
            throw new RuntimeException(
              s"This is a bug. Got $x as result of the send transaction logic. That should not happen")
        }

        response(error.message())
      }

      def rightFx(transactionResult: TransactionResult): Result =
        transactionResult match {
          case TransactionIsCompleted => Created
          case TransactionIsPending   => Accepted
        }

      transactionResult.map {
        case Left(error) => leftFx(error)
        case Right(tr)   => rightFx(tr)
      }
    }

    doCallAndHandleResult(maybeBodyJson)(auxTransaction)
  }

  private def transactionUser721(userID: UUID,
                                 destinationProducer: String => Either[Address, EmailHandle])(
      implicit request: Request[AnyContent]
  ): Future[Result] = {

    val maybeBodyText: Option[String] = request.body.asJson.map(_.toString)

    val maybeBodyJson: Option[Either[RequestParsingError, SendTransaction721PostData]] =
      maybeBodyText.map(SendTransactionController.parseRequest721Content)

    def auxTransaction(sendTransactionPostData: SendTransaction721PostData): Future[Result] = {

      val transactionRequest = TransactionRequestUser721(
        userID,
        masterAccountAddress,
        destinationProducer(sendTransactionPostData.destination),
        BigInt(sendTransactionPostData.id),
        sendTransactionPostData.metaInf
      )

      val transactionResult: Future[Either[TokenTransactionError, TransactionResult]] =
        transactionPort.sendTransactionUser721(
          transactionRequest,
          timestampFx,
          masterAccountPassword
        )
      //  }

      def leftFx(error: TokenTransactionError): Result = {

        val response = error match {
          case e: NonExistentUser                               => Conflict
          case e: UserDoesNotHaveEthAccountInPlatformIdentifier => InternalServerError
          case e: TokenForTransferNotInPlatform                 => Conflict
          case e: SourceUserHasNoBalance.type                   => Conflict
          case e: SpecifiedAmountAsStringNotValid               => BadRequest
          case e: SourceUserHasNotEnoughBalance                 => Conflict
          case e: DestinationUserHasNoTwitterAccount            => Conflict
          case e: TransferSourceUserDoesNotExist                => InternalServerError
          case e: NoUserWithLoginKey                            => InternalServerError
          case e: TriedWithdrawToInvalidAccount                 => Conflict
          case e: NoEthereumAccountForAddress                   => InternalServerError
          case e: EthereumIssue                                 => InternalServerError
          // TODO Write unit test for the following use cases
          case e: ResultingEthereumTxHashNotValid           => InternalServerError
          case e: MetaInfoNotAllowedInPendingTransfers.type => BadRequest
          case x =>
            throw new RuntimeException(
              s"This is a bug. Got $x as result of the send transaction logic. That should not happen")
        }

        response(error.message())
      }

      def rightFx(transactionResult: TransactionResult): Result =
        transactionResult match {
          case TransactionIsCompleted => Created
          case TransactionIsPending   => Accepted
        }

      transactionResult.map {
        case Left(error) => leftFx(error)
        case Right(tr)   => rightFx(tr)
      }
    }

    doCallAndHandleResult(maybeBodyJson)(auxTransaction)
  }
}

private[controller] object SendTransactionController {

  case class SendTransactionPostData(
      destination: String,
      tokenSymbol: String,
      amount: String,
      metaInf: Option[Map[String, String]]
  )

  case class SendTransaction721PostData(
      destination: String,
      id: String,
      metaInf: Option[Map[String, String]]
  )

  def parseRequestContent(content: String): Either[RequestParsingError, SendTransactionPostData] =
    fromCirceErrorToRequestParsingError(decode[SendTransactionPostData](content))

  def parseRequest721Content(
      content: String): Either[RequestParsingError, SendTransaction721PostData] =
    fromCirceErrorToRequestParsingError(decode[SendTransaction721PostData](content))
}
