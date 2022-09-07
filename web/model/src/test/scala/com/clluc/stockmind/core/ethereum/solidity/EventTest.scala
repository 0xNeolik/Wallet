package com.clluc.stockmind.core.ethereum.solidity

import org.scalatest.{FlatSpec, Matchers}

// TODO This test gives a non-deterministic result (sometimes fails)
class EventTest extends FlatSpec with Matchers {
  "Event" should "have a canonical function parameterless" in {
    Event("getName", List.empty).canonical shouldBe "getName()"
  }
  it should "have a canonical function with one parameter" in {
    Event("setNumber", List(Uint(256, 1))).canonical shouldBe "setNumber(uint256)"
  }
  it should "have a canonical function with two parameters" in {
    Event("Deposit", List(Address.default, Bytes(32))).canonical shouldBe
      "Deposit(address,bytes32)"
  }
  it should "encode a parameterless event" in {
    Event("SetNumber", List.empty).encode shouldBe
      "c86aa3e5b1bc5a674de25655f9a3ccf734594e22d008e71d7ede3fe5c93e1384"
  }
  it should "encode an event" in {
    Event("Deposit", List(Address.default, Uint(256))).encode shouldBe
      "e1fffcc4923d04b559f4d29a8bfc6cda04eb5b0d3c460751c2402c5c5cc9109c"
  }
}
