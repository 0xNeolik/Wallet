package com.clluc.stockmind.util

import cats.Functor
import cats.data.EitherT
import com.typesafe.scalalogging.LazyLogging

object LogicUtils extends LazyLogging {

  implicit class PimpEitherT[F[_]: Functor, L, R](either: EitherT[F, L, R]) {

    def logErrors() = {
      either.leftMap { error =>
        logger.error(error.toString)
        error
      }
    }

  }
}
