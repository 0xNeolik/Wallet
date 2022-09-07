package com.clluc.stockmind

import com.clluc.stockmind.core.ethereum.Amount
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class AmountTest extends FlatSpec with Matchers with GeneratorDrivenPropertyChecks {

  private object Generators {

    def genNumber: Gen[Int] =
      for {
        number <- Gen.choose(1, Int.MaxValue)
      } yield number

    def genDecimals: Gen[Int] =
      for {
        number <- Gen.choose(0, 18)
      } yield number

    def genThreeDigitsInt: Gen[Int] =
      for {
        number <- Gen.choose(100, 999)
      } yield number
  }

  behavior of "Amount"

  import Generators._

  it should "have the same value after splitting it" in {
    forAll(genNumber, genDecimals) { (number, decimals) =>
      val amount = Amount.fromRawIntegerValue(number.toString, decimals)
      BigInt(amount.integerPart + amount.decimalPart) shouldBe BigInt(number)
    }
  }

  it should "set to 0 the integer balance part when the decimals are equal to the number of digits" in {
    val number = genThreeDigitsInt.sample.get
    val amount = Amount.fromRawIntegerValue(number.toString, 3)
    amount.decimalPart shouldBe number.toString
    amount.integerPart shouldBe "0"
  }

  it should "add 0s to the decimal balance part when there are more decimals than digits" in {
    val number = genThreeDigitsInt.sample.get
    val amount = Amount.fromRawIntegerValue(number.toString, 4)
    amount.decimalPart shouldBe "0" + number.toString
    amount.integerPart shouldBe "0"
  }

  it should "not show decimal balance when the token has 0 decimals" in {
    val number = genThreeDigitsInt.sample.get
    val amount = Amount.fromRawIntegerValue(number.toString, 0)
    amount.decimalPart shouldBe ""
    amount.integerPart shouldBe number.toString
  }

}
