package com.clluc.stockmind.core.ethereum.solidity

import org.scalatest.{FlatSpec, Matchers}

class BoolTest extends FlatSpec with Matchers {
  "Bool" should "encode to formal Specification of the Encoding" in {
    Bool(false).encode shouldBe "0000000000000000000000000000000000000000000000000000000000000000"
    Bool(true).encode shouldBe "0000000000000000000000000000000000000000000000000000000000000001"
  }

  it should "decode to formal Specification of the Decoding" in {
    Bool
      .decode("0000000000000000000000000000000000000000000000000000000000000000")
      .value shouldBe false
    Bool
      .decode("0000000000000000000000000000000000000000000000000000000000000001")
      .value shouldBe true
  }

  "Canonical bool" should "lowercase name and number of bytes together" in {
    Bool(true).canonical shouldBe "bool"
    Bool(false).canonical shouldBe "bool"
  }
}
