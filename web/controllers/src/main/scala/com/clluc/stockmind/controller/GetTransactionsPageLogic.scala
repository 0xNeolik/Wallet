package com.clluc.stockmind.controller

import java.util.UUID

import cats.Monad
import cats.data.NonEmptyList
import com.clluc.stockmind.controller.GetTransactionsPageValidations.GetTransactionsPageParameters
import com.clluc.stockmind.controller.RetrieveTransactionsController._
import com.clluc.stockmind.core.transaction.{Stockmind721Transaction, StockmindTransaction}
import com.clluc.stockmind.core.transaction.StockmindTransaction.{
  TransactionsRetrievalError,
  ValidatedTxRetrievalResult
}
import play.api.http.{ContentTypes, Writeable}
import play.api.mvc._

/**
  * Trait meant to separate concerns. Both for modularity and easy testing reasons it's convenient to set apart
  * the outer interface to this logic (Silhouette authentication and Play actions) and what we do afterwards.
  */
private[controller] trait GetTransactionsPageLogic[P[_]]
    extends TransactionsPageValidations
    with ErrorToResultConversions
    with ContentTypes {

  // Function that represents communication with the corresponding primary port
  def callTransactionsPageOnPrimaryPort(
      userId: UUID,
      offset: Int,
      numOfTxs: Int
  ): P[ValidatedTxRetrievalResult[List[StockmindTransaction]]]

  def call721TransactionsPageOnPrimaryPort(
      userId: UUID,
      offset: Int,
      numOfTxs: Int
  ): P[ValidatedTxRetrievalResult[List[Stockmind721Transaction]]]

  def transactionsPage(offsetQueryParam: Seq[String], limitQueryParam: Seq[String], userId: UUID)(
      implicit ev: Monad[P]): P[Result] = {

    import cats.syntax.functor._

    val validatedLogicResult: Either[NonEmptyList[ControllerError],
                                     P[ValidatedTxRetrievalResult[List[StockmindTransaction]]]] = {

      val parameters: Either[NonEmptyList[ControllerError], GetTransactionsPageParameters] =
        validateQueryStringParams(offsetQueryParam, limitQueryParam)

      parameters.map { pars =>
        for {
          validatedTxs <- callTransactionsPageOnPrimaryPort(
            userId,
            pars.offset,
            pars.limit
          )
        } yield validatedTxs

      }
    }

    implicit val writeableControllerError: Writeable[NonEmptyList[ControllerError]] =
      genericPlainTextWriteable[NonEmptyList[ControllerError]] {
        _.map(controllerErrorToMessage).toList.mkString(System.getProperty("line.separator"))
      }

    implicit val writeableTransactionRetrievalError: Writeable[TransactionsRetrievalError] =
      genericPlainTextWriteable[TransactionsRetrievalError](transactionRetrievalErrorToMessage)

    import cats.syntax.applicative._

    validatedLogicResult match {
      case Left(errors) => BadRequest(errors).pure[P]
      case Right(logicResult) =>
        logicResult.map {
          case Left(transactionRetrievalError) =>
            InternalServerError(transactionRetrievalError)
          case Right(transactions) =>
            Ok(transactions.map(_.toView))
        }
    }
  }

  def transactionsPage721(offsetQueryParam: Seq[String],
                          limitQueryParam: Seq[String],
                          userId: UUID)(implicit ev: Monad[P]): P[Result] = {

    import cats.syntax.functor._

    val validatedLogicResult
      : Either[NonEmptyList[ControllerError],
               P[ValidatedTxRetrievalResult[List[Stockmind721Transaction]]]] = {

      val parameters: Either[NonEmptyList[ControllerError], GetTransactionsPageParameters] =
        validateQueryStringParams(offsetQueryParam, limitQueryParam)

      parameters.map { pars =>
        for {
          validatedTxs <- call721TransactionsPageOnPrimaryPort(
            userId,
            pars.offset,
            pars.limit
          )
        } yield validatedTxs

      }
    }

    implicit val writeableControllerError: Writeable[NonEmptyList[ControllerError]] =
      genericPlainTextWriteable[NonEmptyList[ControllerError]] {
        _.map(controllerErrorToMessage).toList.mkString(System.getProperty("line.separator"))
      }

    implicit val writeableTransactionRetrievalError: Writeable[TransactionsRetrievalError] =
      genericPlainTextWriteable[TransactionsRetrievalError](transactionRetrievalErrorToMessage)

    import cats.syntax.applicative._

    validatedLogicResult match {
      case Left(errors) => BadRequest(errors).pure[P]
      case Right(logicResult) =>
        logicResult.map {
          case Left(transactionRetrievalError) =>
            InternalServerError(transactionRetrievalError)
          case Right(transactions) =>
            Ok(transactions.map(_.toView))
        }
    }
  }

}
