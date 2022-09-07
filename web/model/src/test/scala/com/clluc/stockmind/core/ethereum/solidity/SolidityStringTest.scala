package com.clluc.stockmind.core.ethereum.solidity

import org.scalatest.{FlatSpec, Matchers, OptionValues}

class SolidityStringTest extends FlatSpec with Matchers with OptionValues {

  val dave          = SolidityString("dave")
  val tildes        = SolidityString("aáaaá")
  val eur           = SolidityString("€")
  val encodedDave   = "64617665"
  val encodedTildes = "61c3a16161c3a1"
  val encodedEur    = "e282ac"

  behavior of "String (Solidity type)"

  it should "be encoded as hex according to the Ethereum ABI" in {
    val dave   = SolidityString("dave")
    val tildes = SolidityString("aáaaá")
    val eur    = SolidityString("€")

    dave.encode shouldBe encodedDave
    tildes.encode shouldBe encodedTildes
    eur.encode shouldBe encodedEur
  }

  it should "decode a hex string that conforms to the Ethereum ABI" in {
    SolidityString.decode(encodedDave) shouldBe dave.value
    SolidityString.decode(encodedTildes) shouldBe tildes.value
    SolidityString.decode(encodedEur) shouldBe eur.value
  }

  behavior of "String (Solidity type) - decoding from ABI-encoded dynamic type"

  it should "decode a correctly formed string" in {
    val encoded =
      "0000000000000000000000000000000000000000000000000000000000000003" +
        "6162630000000000000000000000000000000000000000000000000000000000"
    SolidityString.decodeDynamicEncoded(encoded).value shouldBe "abc"
  }

  it should "fail if the encoded string doesn't contain full chunks" in {
    val tooShort = "616263"
    val wholeResponse = "0x" +
      "0000000000000000000000000000000000000000000000000000000000000020" +
      "0000000000000000000000000000000000000000000000000000000000000003" +
      "6162630000000000000000000000000000000000000000000000000000000000"
    SolidityString.decodeDynamicEncoded(tooShort) shouldBe None
    SolidityString.decodeDynamicEncoded(wholeResponse) shouldBe None
  }

  it should "fail if there are more than 2 chunks" in {
    // This is a temporal test acknowledging the current impl limitation
    val encoded =
      "0000000000000000000000000000000000000000000000000000000000000040" +
        "0000000000000000000000000000000000000000000000000000000000000003" +
        "6162630000000000000000000000000000000000000000000000000000000000"
    SolidityString.decodeDynamicEncoded(encoded) shouldBe None
  }

  it should "fail if the padding contains anything but 0's" in {
    val encoded =
      "0000000000000000000000000000000000000000000000000000000000000003" +
        "6162630000000000000000100000000000000000000000000000000000000000"
    SolidityString.decodeDynamicEncoded(encoded) shouldBe None
  }
}
