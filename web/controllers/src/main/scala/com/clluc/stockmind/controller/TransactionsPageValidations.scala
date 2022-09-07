package com.clluc.stockmind.controller

import cats.data.NonEmptyList
import com.clluc.stockmind.controller.GetTransactionsPageValidations.{
  validateInt,
  GetTransactionsPageParameters
}
import com.clluc.stockmind.controller.RetrieveTransactionsController._

import scala.util.{Failure, Success, Try}

/**
  * Trait that deals with parsing request parameters to Int, and validating that are >= 0.
  * It's validateQueryStringParams is able to deal with an arbitrary number of parameters as long as
  * they have the same constraints: to come as an optional sequence of Strings, and contains a single
  * Int element (represented as a single String).
  */
private[controller] trait TransactionsPageValidations {

  def validateQueryStringParams(
      offset: Seq[String],
      limit: Seq[String]
  ): Either[NonEmptyList[ControllerError], GetTransactionsPageParameters] = {
    import cats.syntax.cartesian._

    (validateInt(offset, "offset") |@| validateInt(limit, "limit"))
      .map(GetTransactionsPageParameters)
      .toEither
  }
}

private[controller] object GetTransactionsPageValidations {

  case class GetTransactionsPageParameters(offset: Int, limit: Int)

  import cats.syntax.validated._

  private def validateParam[T](
      paramName: String,
      rawParam: Seq[String],
      conversion: String => Try[T],
      conversionFailMsg: String,
      constraint: T => Boolean,
      constraintFailMsg: String
  ): ValidatedStep[T] = {
    val paramList = rawParam.toList
    paramList match {
      case param :: Nil =>
        conversion(param) match {
          case Success(p) =>
            if (constraint(p)) p.valid
            else ParamBrokenConstraint(paramName, p.toString, constraintFailMsg).invalidNel
          case Failure(_) => ParamBadFormat(paramName, param, conversionFailMsg).invalidNel
        }

      case _ :: _ => ParamMultipleValues(paramName, paramList).invalidNel

      case Nil => ParamEmptyValue(paramName).invalidNel
    }
  }

  def validateInt(param: Seq[String], paramName: String) =
    validateParam[Int](
      paramName,
      param,
      s => Try(s.toInt),
      "Not an integer",
      _ >= 0,
      "Cannot be negative"
    )
}
