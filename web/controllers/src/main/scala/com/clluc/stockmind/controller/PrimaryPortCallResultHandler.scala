package com.clluc.stockmind.controller

import play.api.mvc.{BaseController, Result}

import scala.concurrent.Future

private[controller] trait PrimaryPortCallResultHandler {
  this: BaseController =>

  def doCallAndHandleResult[T](maybeRequest: Option[Either[RequestParsingError, T]])(
      call: T => Future[Result]): Future[Result] = {

    val maybeEventualResult: Option[Future[Result]] = maybeRequest.map {
      case Right(request)                   => call(request)
      case Left(error: RequestParsingError) => Future.successful(BadRequest(error.cause.getMessage))
    }

    maybeEventualResult.getOrElse(
      Future.successful(BadRequest("Body, form or parameters required for this POST request"))
    )
  }
}
