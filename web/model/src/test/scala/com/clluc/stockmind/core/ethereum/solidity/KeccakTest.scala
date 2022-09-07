package com.clluc.stockmind.core.ethereum.solidity

import org.scalatest.{FlatSpec, Matchers}

class KeccakTest extends FlatSpec with Matchers {

  "Kecccak" should "encode a value" in {
    val input  = "easydao".getBytes
    val output = "0e1dc15029b6177895e04191e915cabd6b05ff96097b774b73246aec25c43f45"

    Keccak.encode(input).map("%02x".format(_)).mkString shouldBe output
  }
}
