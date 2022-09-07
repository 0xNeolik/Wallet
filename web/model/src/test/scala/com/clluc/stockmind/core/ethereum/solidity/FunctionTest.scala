package com.clluc.stockmind.core.ethereum.solidity

import org.scalatest.{FlatSpec, Matchers}

class FunctionTest extends FlatSpec with Matchers {

  behavior of "Solidity concepts - Function"

  it should "return the appropriate function signatures" in {
    Function("noParams", List.empty).canonical shouldBe "noParams()"
    Function("oneParam", List(Uint(256, 1))).canonical shouldBe "oneParam(uint256)"
    Function("twoParams", List(Uint(32, 1), Bool(true))).canonical shouldBe "twoParams(uint32,bool)"
    Function("allTypes", List(Uint(64, 1), Bytes(32), SolidityString("holi"))).canonical shouldBe "allTypes(uint64,bytes32,string)"
  }

  it should "encode method calls according to the Ethereum ABI - no parameters" in {
    Function("baz", List.empty).encode shouldBe "a7916fac"
  }

  it should "encode method calls according to the Ethereum ABI - static parameters" in {
    Function("baz", List(Uint(32, 69), Bool(true))).encode shouldBe "cdcd77c0" +
      "0000000000000000000000000000000000000000000000000000000000000045" + // 69 in hex
      "0000000000000000000000000000000000000000000000000000000000000001" // 1 for true
    Function("multiply", List(Uint(256, 6))).encode shouldBe "c6888fa1" +
      "0000000000000000000000000000000000000000000000000000000000000006"
  }

  it should "encode method calls according to the Ethereum ABI - dynamic parameters" in {
    val expected = "08216c0f" +
      "0000000000000000000000000000000000000000000000000000000000002710" + // 10000 in hex
      "0000000000000000000000000000000000000000000000000000000000000080" + // pointer to start of 5th chunk
      "0000000000000000000000000000000000000000000000000000000000000003" + // 3 in hex
      "00000000000000000000000000000000000000000000000000000000000000c0" + // pointer to start of 7th chunk
      "0000000000000000000000000000000000000000000000000000000000000005" + // length of data: 5
      "6162636465000000000000000000000000000000000000000000000000000000" + // data: "abcde" in UTF-8 encoding
      "0000000000000000000000000000000000000000000000000000000000000003" + // length of data: 7
      "6162630000000000000000000000000000000000000000000000000000000000" // data: "abc" in UTF-8 encoding
    Function(
      "createHumanStandardToken",
      List(Uint(256, 10000), SolidityString("abcde"), Uint(8, 3), SolidityString("abc"))
    ).encode shouldBe expected
  }

}
