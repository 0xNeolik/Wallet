package com.clluc.stockmind.core.transaction

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * Type class of higher kinded types that can wrap a value and an exception; and that defines for
  * them a function to give back an Either, which Left is derived from the exception and the Right
  * contains it's wrapped value.
  *
  * In other words; given a higher kind that could wrap either a value or an exception, give as inside
  * an Scala Either the value or the exception mapped by a function.
  * @tparam P The higher kind that could possible contain a value or an exception.
  * @tparam L The type to which we want to map the exception to.
  * @tparam R The type of the value inside P, in case of no Exception.
  */
private[transaction] trait WithEitherRecoverable[P[_], L, R] {

  /**
    * Convert a possibly faulty value of type P[R] into a P[Either]
    * @param r The value to be transformed
    * @param thFx The function that will map the exception case to a value
    * @return A P containing either a Left[T] or a Right[R]
    */
  def throwableAsLeft(r: P[R], thFx: Throwable => L): P[Either[L, R]]
}

private[transaction] object WithEitherRecoverable {

  object Syntax {
    private[transaction] implicit class RecoverSyntax[P[_], R](p: P[R]) {

      def throwableAsLeft[L](thFx: Throwable => L)(
          implicit ev: WithEitherRecoverable[P, L, R]): P[Either[L, R]] =
        ev.throwableAsLeft(p, thFx)
    }
  }

  object Instances {

    class FutureWithEitherRecoverable[L, R](implicit ec: ExecutionContext, ct: ClassTag[R])
        extends WithEitherRecoverable[Future, L, R] {

      override def throwableAsLeft(r: Future[R], left: Throwable => L): Future[Either[L, R]] = {
        r.map {
          case Success(x: R) => Right(x)
          case Failure(t)    => Left(left(t))
        }
      }
    }

    class TryWithEitherRecoverable[L, R](implicit t: ClassTag[R])
        extends WithEitherRecoverable[Try, L, R] {
      override def throwableAsLeft(r: Try[R], thFx: Throwable => L): Try[Either[L, R]] =
        Try(
          r match {
            case Success(x: R) => Right(x)
            case Failure(th)   => Left(thFx(th))
          }
        )
    }
  }
}
