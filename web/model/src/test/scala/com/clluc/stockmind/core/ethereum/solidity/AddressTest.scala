package com.clluc.stockmind.core.ethereum.solidity

import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class AddressTest extends FlatSpec with Matchers with PropertyChecks {

  def genAddress: Gen[String] =
    for {
      low     <- Gen.choose[Char]('a', 'f')
      number  <- Gen.choose[Char]('0', '9')
      address <- Gen.listOfN(40, Gen.oneOf(low, number)).map(_.mkString)
    } yield address

  "Address" should "not throw exception with well-format address" in {
    noException shouldBe thrownBy {
      forAll(genAddress) { address =>
        Address(address)
      }
    }
  }

  it should "encode to formal Specification of the Encoding" in {
    Address("407d73d8a49eeb85d32cf465507dd71d507100c1").encode shouldBe "000000000000000000000000407d73d8a49eeb85d32cf465507dd71d507100c1"
    Address("407d73d8a49eeb85d32cf465507dd71d507100c3").encode shouldBe "000000000000000000000000407d73d8a49eeb85d32cf465507dd71d507100c3"
  }

  it should "decode to formal Specification of the Decoding" in {
    Address
      .decode("0x000000000000000000000000407d73d8a49eeb85d32cf465507dd71d507100c1")
      .value shouldBe "407d73d8a49eeb85d32cf465507dd71d507100c1"
    Address
      .decode("0x000000000000000000000000407d73d8a49eeb85d32cf465507dd71d507100c3")
      .value shouldBe "407d73d8a49eeb85d32cf465507dd71d507100c3"
  }
}
