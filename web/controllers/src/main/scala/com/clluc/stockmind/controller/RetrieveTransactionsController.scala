package com.clluc.stockmind.controller

import java.util.UUID

import javax.inject.Inject
import com.clluc.stockmind.controller.RetrieveTransactionsController._
import com.clluc.stockmind.core.transaction.CancelPendingTransaction._
import com.clluc.stockmind.core.transaction.StockmindTransaction._
import com.clluc.stockmind.core.transaction._
import com.clluc.stockmind.port.primary.{RetrieveTransactionsPort, UserPort}

import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.mvc._
import cats.data.ValidatedNel

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import play.api.cache.SyncCacheApi
import play.api.http.Writeable

class RetrieveTransactionsController @Inject()(
    // silhouette: Silhouette[DefaultEnv],
    val controllerComponents: ControllerComponents,
    getTransactionsPort: RetrieveTransactionsPort,
    cache: SyncCacheApi,
    usersPort: UserPort,
    socAuthcontroller: SocialAuthController
    //authInfoRepository: AuthInfoRepository
)(
    implicit
    executionContext: ExecutionContext
) extends BaseController
    with GetTransactionsPageLogic[Future] {

  // GET /v1/transactions
  /*def getTransactionsPage: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val offset: Seq[String] = request.queryString.getOrElse("offset", Seq("0"))
    val limit: Seq[String]  = request.queryString.getOrElse("limit", Seq("10"))

    import BoundedContextImplicitConversions._
    import cats.instances.future._ // Bring in Monad[Future]

    for {
      silhouetteOAuth1Info <- authInfoRepository.find[SilhouetteOAuthInfo](
        request.identity.loginInfo.toSilhouetteLoginInfo)
      coreOAuth1Info = silhouetteOAuth1Info.map(_.toCoreOauth1Info)
      result <- transactionsPage(offset, limit, extractUserId(request), coreOAuth1Info)
    } yield result
  }*/

  // GET /v1/erc20-transactions
  def getTransactionsPage: Action[AnyContent] = socAuthcontroller.AuthenticatedAction {
    idToken =>
      request =>
        //val idToken             = request.session.get("idToken").get
        val userID              = cache.get[UUID](idToken + "id").get
        val offset: Seq[String] = request.queryString.getOrElse("offset", Seq("0"))
        val limit: Seq[String]  = request.queryString.getOrElse("limit", Seq("10"))
        // import BoundedContextImplicitConversions._
        import cats.instances.future._ // Bring in Monad[Future]
        for {
          result <- transactionsPage(offset, limit, userID)
        } yield result

  }

  // GET /v1/erc721-transactions
  def get721TransactionsPage: Action[AnyContent] = socAuthcontroller.AuthenticatedAction {
    idToken =>
      request =>
        //val idToken             = request.session.get("idToken").get
        val userID              = cache.get[UUID](idToken + "id").get
        val offset: Seq[String] = request.queryString.getOrElse("offset", Seq("0"))
        val limit: Seq[String]  = request.queryString.getOrElse("limit", Seq("10"))
        // import BoundedContextImplicitConversions._
        import cats.instances.future._ // Bring in Monad[Future]
        for {
          result <- transactionsPage721(offset, limit, userID)
        } yield result

  }

  private def obtainTransaction(txId: String, tx: Try[Future[Option[StockmindTransaction]]]) =
    tx match {
      case Failure(_) => Future.successful(BadRequest(txMustBeANumberMessage))
      case Success(future) =>
        future.map {
          case Some(_tx) => Ok(_tx.toView).as(JSON)
          case None      => NotFound(txNotFoundMessage(txId.toLong))
        }
    }

  private def obtainTransaction721(txId: String, tx: Try[Future[Option[Stockmind721Transaction]]]) =
    tx match {
      case Failure(_) => Future.successful(BadRequest(txMustBeANumberMessage))
      case Success(future) =>
        future.map {
          case Some(_tx) => Ok(_tx.toView).as(JSON)
          case None      => NotFound(txNotFoundMessage(txId.toLong))
        }
    }

  // GET /v1/erc20-transactions/:id
  def getTransaction(id: String): Action[AnyContent] = socAuthcontroller.AuthenticatedAction {
    idToken => implicit request =>
      val userID = cache.get[UUID](idToken + "id").get
      val tx: Try[Future[Option[StockmindTransaction]]] = Try(id.toLong).map { numericTxId =>
        getTransactionsPort.findTransactionById(userID, numericTxId)
      }
      obtainTransaction(id, tx)

  }

  // GET /v1/erc721-transactions/:id
  def get721Transaction(id: String): Action[AnyContent] = socAuthcontroller.AuthenticatedAction {
    idToken => implicit request =>
      val userID = cache.get[UUID](idToken + "id").get
      val tx: Try[Future[Option[Stockmind721Transaction]]] = Try(id.toLong).map { numericTxId =>
        getTransactionsPort.find721TransactionById(userID, numericTxId)
      }
      obtainTransaction721(id, tx)

  }

  // GET /v1/erc20-transactions/pending/:id
  def getPendingTransaction(id: String): Action[AnyContent] =
    socAuthcontroller.AuthenticatedAction { idToken => request =>
      //import BoundedContextImplicitConversions._
      val txDeepInAComplexContext: Try[Future[Option[StockmindTransaction]]] =
        Try(id.toLong).map { numericTxId =>
          getTransactionsPort.findPendingTransactionById(numericTxId)
        }
      obtainTransaction(id, txDeepInAComplexContext)

    }

  // GET /v1/erc721-transactions/pending/:id
  def getPending721Transaction(id: String): Action[AnyContent] =
    socAuthcontroller.AuthenticatedAction { idToken => request =>
      //import BoundedContextImplicitConversions._
      val txDeepInAComplexContext: Try[Future[Option[Stockmind721Transaction]]] =
        Try(id.toLong).map { numericTxId =>
          getTransactionsPort.findPending721TransactionById(numericTxId)
        }
      obtainTransaction721(id, txDeepInAComplexContext)

    }

  // DELETE /v1/transactions/pending/:id
  def cancelPendingTransaction(id: String): Action[AnyContent] =
    socAuthcontroller.AuthenticatedAction { idToken => implicit request =>
      //val idToken = request.session.get("idToken").get
      val userID = cache.get[UUID](idToken + "id").get

      val tx: Try[Future[ValidatedCancelPendingTransactionOp[Unit]]] = Try(id.toLong).map {
        numericTxId =>
          getTransactionsPort.cancelPendingTransactionById(numericTxId, userID)
      }

      tx match {
        case Failure(_) => Future.successful(BadRequest(txMustBeANumberMessage))
        case Success(eventualMaybeTransaction) =>
          eventualMaybeTransaction.map {
            case Left(error) => cancelPendingTxErrorToResult(error)
            case Right(_)    => Ok
          }
    }

    }

  private def cancelPendingTxErrorToResult(error: CancelPendingTransactionError): Result =
    error match {
      case TxNotFromTheCancelActionRequester(_, txId) =>
        Forbidden(s"You're not authorized to do that")

      case PendingTxNotFound(txId) => NotFound(txNotFoundMessage(txId))

      case TxIdNaN(_) => BadRequest(txMustBeANumberMessage)

      case TxIdTooBigToBeALong(_) => BadRequest(txMustBeANumberMessage)
    }

  override def callTransactionsPageOnPrimaryPort(userId: UUID, offset: Int, numOfTxs: Int) =
    getTransactionsPort.findTransactionsPage(userId, offset, numOfTxs)

  override def call721TransactionsPageOnPrimaryPort(userId: UUID, offset: Int, numOfTxs: Int) =
    getTransactionsPort.find721TransactionsPage(userId, offset, numOfTxs)

}

private[controller] object RetrieveTransactionsController extends ErrorToResultConversions {

  case class TransactionView(
      id: String,
      direction: String,
      pending: Boolean,
      counterparty: Counterparty,
      token: String,
      decimals: Int,
      amount: TokenAmount,
      txHash: Option[String],
      date: String
  )

  case class Transaction721View(
      id: String,
      direction: String,
      pending: Boolean,
      counterparty: Counterparty,
      token: String,
      metadata: String,
      token_id: String,
      txHash: Option[String],
      date: String
  )

  // We will need to print out Ok responses
  implicit val writeableTransactionViewList: Writeable[List[TransactionView]] =
    genericJsonWriteable[List[TransactionView]](_.asJson)

  implicit val writeableTransactionView: Writeable[TransactionView] =
    genericJsonWriteable[TransactionView](_.asJson)

  implicit val writeable721TransactionViewList: Writeable[List[Transaction721View]] =
    genericJsonWriteable[List[Transaction721View]](_.asJson)

  implicit val writeable721TransactionView: Writeable[Transaction721View] =
    genericJsonWriteable[Transaction721View](_.asJson)

  implicit class StockmindTransactionToView(tx: StockmindTransaction) {

    def toView: TransactionView = {

      def formatDate(date: DateTime) =
        DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss").print(date)

      TransactionView(
        if (tx.pending) s"pending/${tx.id}" else tx.id.toString,
        tx.direction match {
          case IncomingTx => "receive"
          case OutgoingTx => "send"
        },
        tx.pending,
        tx.counterparty,
        tx.token,
        tx.decimals,
        tx.amount,
        tx.txHash,
        formatDate(tx.date)
      )
    }
  }

  implicit class Stockmind721TransactionToView(tx: Stockmind721Transaction) {

    def toView: Transaction721View = {

      def formatDate(date: DateTime) =
        DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss").print(date)

      Transaction721View(
        if (tx.pending) s"pending/${tx.id}" else tx.id.toString,
        tx.direction match {
          case IncomingTx => "receive"
          case OutgoingTx => "send"
        },
        tx.pending,
        tx.counterparty,
        tx.token,
        tx.meta,
        tx.token_id.toString(),
        tx.txHash,
        formatDate(tx.date)
      )
    }
  }

  def txNotFoundMessage(id: Long): String =
    s"Transaction with id $id has not been found"

  val txMustBeANumberMessage = "The transaction id has to be a 64 bits signed integer"

  // validations and error handling
  type ValidatedStep[A] = ValidatedNel[ControllerError, A]

  // ADT for validation errors
  sealed trait ControllerError
  sealed trait ControllerParamValidationError extends ControllerError
  case class ParamEmptyValue(param: String)   extends ControllerParamValidationError
  case class ParamMultipleValues(param: String, values: List[String])
      extends ControllerParamValidationError
  case class ParamBadFormat(param: String, value: String, reason: String)
      extends ControllerParamValidationError
  case class ParamBrokenConstraint(param: String, value: String, reason: String)
      extends ControllerParamValidationError
  case class LogicError(error: TransactionsRetrievalError) extends ControllerError

  /*def fromSilhouetteToCoreOAuthInfo(info: SilhouetteOAuthInfo): CoreOAuth1Info =
    CoreOAuth1Info(
      info.token,
      info.secret
    )*/

  def transactionRetrievalErrorToMessage(error: TransactionsRetrievalError): String = {

    val suffix = System.getProperty("line.separator") +
      "This is a data integrity issue. Please contact the Stockmind development team"

    error match {
      case TokenNotSupported(symbol) =>
        s"Found a transaction for the current user for a token not currently supported: $symbol" + suffix
      case CurrentUserWithoutStockmindEthereumAddress(uid) =>
        s"The current user (with id $uid) has no ethereum address in the system database" + suffix
      case NeitherFromNorToHaveScreenNames(from, to) =>
        s"Found a transaction for which neither the origin nor the destination ethereum address belongs to a SolidGo " +
          s"user. From ${from.toHex}, to ${to.toHex}" + suffix
      case TwitterIdDoesNotExistAsTwitterUser(id) =>
        s"The user with Twitter id $id found as destination of a pending transaction has no twitter account. " +
          "This transaction shouldn't have been sent" + suffix
      case CurrentUserDoesNotHaveOAuth1InfoInTheSystem(id) =>
        s"The user (with id $id) for which we want to list it's transactions does not have oAuth1 info in the " +
          "system database" + suffix
      case ParamShouldNotBeNegative(name, value) =>
        s"The query string parameter $name cannot be negative (was $value)"
    }
  }

  def controllerErrorToMessage(error: ControllerError): String =
    error match {
      case ParamEmptyValue(param) => s"Query string parameter $param doesn't contain any value"
      case ParamMultipleValues(param, values) =>
        s"Query string parameter $param contains multiple values: ${values.mkString(", ")}"
      case ParamBadFormat(param, value, reason) =>
        s"Query string parameter $param (value: $value) has incorrect format: $reason"
      case ParamBrokenConstraint(param, value, reason) =>
        s"Query string parameter $param (value: $value) disobeys constraint: $reason"
      case LogicError(err) => transactionRetrievalErrorToMessage(err)
    }
}
