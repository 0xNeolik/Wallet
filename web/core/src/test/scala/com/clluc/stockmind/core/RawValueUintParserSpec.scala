package com.clluc.stockmind.core

import com.clluc.stockmind.core
import RawValueParser._
import core.Generators.genStringNumber
import org.scalatest.{FlatSpec, Matchers}

class RawValueUintParserSpec extends FlatSpec with Matchers {
  behavior of "parseIntoRawValue"

  it should "add 0s to the end of a number when there are more decimals" in {
    val integerPart = genStringNumber(2).sample.get
    val decimalPart = genStringNumber(2).sample.get
    val amount      = parseIntoRawValue(integerPart + "." + decimalPart, 3)
    amount shouldBe Some(BigInt(integerPart + decimalPart + "0"))
  }

  it should "add 0s to the end of a number without decimals when there are decimals" in {
    val integerPart = genStringNumber(2).sample.get
    val amount      = parseIntoRawValue(integerPart, 3)
    amount shouldBe Some(BigInt(integerPart + "000"))
  }

  it should "return the introduced number when the number has the same length than the number of decimals" in {
    val integerPart = genStringNumber(2).sample.get
    val decimalPart = genStringNumber(2).sample.get
    val amount      = parseIntoRawValue(integerPart + "." + decimalPart, 2)
    amount shouldBe Some(BigInt(integerPart + decimalPart))
  }

  it should "return None when the number has more decimals than the allowed quantity" in {
    val integerPart = genStringNumber(2).sample.get
    val decimalPart = genStringNumber(2).sample.get
    val amount      = parseIntoRawValue(integerPart + "." + decimalPart, 1)
    amount shouldBe None
  }

  it should "return None when the number is empty" in {
    val amount = parseIntoRawValue("", 1)
    amount shouldBe None
  }

  it should "return None when the number is malformed" in {
    val amount = parseIntoRawValue("2.3.4.5", 1)
    amount shouldBe None
  }

}
