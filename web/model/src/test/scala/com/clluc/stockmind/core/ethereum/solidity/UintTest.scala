package com.clluc.stockmind.core.ethereum.solidity

import org.scalatest.{FlatSpec, Matchers}

class UintTest extends FlatSpec with Matchers {
  "Unit bits" should "be multiple of 8" in {
    noException shouldBe thrownBy {
      Uint(8, 0)
      Uint(16, 0)
      Uint(256, 0)
    }
    an[IllegalArgumentException] shouldBe thrownBy(Uint(1, 0))
  }

  it should "not be negative" in {
    an[IllegalArgumentException] shouldBe thrownBy(Uint(-1, 0))
  }

  it should "not be greater than 256" in {
    an[IllegalArgumentException] shouldBe thrownBy(Uint(257, 0))
  }

  "Uint" should "encode to formal Specification of the Encoding" in {
    Uint(256, 0).encode shouldBe "0000000000000000000000000000000000000000000000000000000000000000"
    Uint(256, 1).encode shouldBe "0000000000000000000000000000000000000000000000000000000000000001"
    Uint(256, 15).encode shouldBe "000000000000000000000000000000000000000000000000000000000000000f"
    Uint(256, 51966).encode shouldBe "000000000000000000000000000000000000000000000000000000000000cafe"
  }

  it should "decode to formal Specification of the Decoding" in {
    Uint
      .decode(256, "0000000000000000000000000000000000000000000000000000000000000000")
      .value shouldBe 0
    Uint
      .decode(256, "0000000000000000000000000000000000000000000000000000000000000001")
      .value shouldBe 1
    Uint
      .decode(256, "000000000000000000000000000000000000000000000000000000000000000f")
      .value shouldBe 15
    Uint
      .decode(256, "000000000000000000000000000000000000000000000000000000000000cafe")
      .value shouldBe 51966
  }

  it should "not be greater than max value depending its bits" in {
    val maxValue256 =
      BigInt("115792089237316195423570985008687907853269984665640564039457584007913129639936")
    val maxValue8 = BigInt("256")
    noException shouldBe thrownBy {
      Uint(256, maxValue256)
      Uint(8, maxValue8)
    }

    an[IllegalArgumentException] shouldBe thrownBy {
      Uint(256, maxValue256 + BigInt("1"))
      Uint(8, maxValue8 + BigInt("1"))
    }
  }

  it should "not be lower than 0" in {
    noException shouldBe thrownBy {
      Uint(256, BigInt(0))
      Uint(8, BigInt(0))
    }

    an[IllegalArgumentException] shouldBe thrownBy {
      Uint(256, BigInt("-1"))
      Uint(8, BigInt("-1"))
    }
  }

  "Canonical Uint" should "lowercase name and number of bytes together" in {
    Uint(256, 0).canonical shouldBe "uint256"
    Uint(32, 0).canonical shouldBe "uint32"
  }
}
