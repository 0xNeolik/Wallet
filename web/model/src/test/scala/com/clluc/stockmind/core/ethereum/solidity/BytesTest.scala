package com.clluc.stockmind.core.ethereum.solidity

import org.scalatest.{FlatSpec, Matchers}

class BytesTest extends FlatSpec with Matchers {

  behavior of "Bytes (Solidity)"

  it should "allow sizes between 1 to 32" in {
    noException shouldBe thrownBy {
      Bytes(1, "")
      Bytes(2, "")
      Bytes(32, "")
    }
    an[IllegalArgumentException] shouldBe thrownBy(Bytes(0, ""))
    an[IllegalArgumentException] shouldBe thrownBy(Bytes(33, "123"))
  }

  it should "allow hex encoded strings as content" in {
    noException shouldBe thrownBy {
      Bytes(32, "deadbeef")
      Bytes(32, "cafebabe")
      Bytes(32, "0123456789abcdef")
    }
    an[IllegalArgumentException] shouldBe thrownBy(Bytes(32, "g"))
    an[IllegalArgumentException] shouldBe thrownBy(Bytes(32, "!"))
  }

  it should "not allow content that exceeds the given size" in {
    an[IllegalArgumentException] shouldBe thrownBy(Bytes(0, "a"))
    an[IllegalArgumentException] shouldBe thrownBy(Bytes(4, "112233445"))
  }

  it should "encode strings into ASCII byte sequences" in {
    Bytes
      .stringAsAscii(32, "")
      .encode shouldBe "0000000000000000000000000000000000000000000000000000000000000000"
    Bytes
      .stringAsAscii(32, "gavofyork")
      .encode shouldBe "6761766f66796f726b0000000000000000000000000000000000000000000000"
    Bytes
      .stringAsAscii(32, "1234567890")
      .encode shouldBe "3132333435363738393000000000000000000000000000000000000000000000"
    Bytes
      .stringAsAscii(32, "easydao")
      .encode shouldBe "6561737964616f00000000000000000000000000000000000000000000000000"
  }

  it should "left-pad its contents" in {
    Bytes(32, "123").leftPadded.encode shouldBe "0000000000000000000000000000000000000000000000000000000000000123"
    Bytes(2, "bf").leftPadded.encode shouldBe "00bf000000000000000000000000000000000000000000000000000000000000"
  }

  it should "lowercase name and number of bytes together" in {
    Bytes(1, "").canonical shouldBe "bytes1"
    Bytes(32, "").canonical shouldBe "bytes32"
    Bytes(32).canonical shouldBe "bytes32"
  }

}
