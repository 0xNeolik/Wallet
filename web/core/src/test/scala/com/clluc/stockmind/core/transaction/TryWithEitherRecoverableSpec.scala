package com.clluc.stockmind.core.transaction

import com.clluc.stockmind.core.transaction.WithEitherRecoverable.Instances.TryWithEitherRecoverable
import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Failure, Success, Try}

class TryWithEitherRecoverableSpec extends FlatSpec with Matchers {

  private def throwableAsLeft(t: Try[Int]): Try[Either[String, Int]] = {
    import WithEitherRecoverable.Syntax._

    implicit val ev: WithEitherRecoverable[Try, String, Int] = new TryWithEitherRecoverable

    t.throwableAsLeft(_.getMessage)
  }

  "A success" should "be mapped to Right" in {
    val value             = 5
    val success: Try[Int] = Success(value)

    throwableAsLeft(success) shouldBe Success(Right(value))
  }

  "A failure" should "be mapped to Left" in {
    val errorMsg          = "Ups"
    val failure: Try[Int] = Failure(new Exception(errorMsg))

    throwableAsLeft(failure) shouldBe Success(Left(errorMsg))
  }
}
