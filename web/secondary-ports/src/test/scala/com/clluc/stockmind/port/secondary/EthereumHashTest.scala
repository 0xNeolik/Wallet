package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.ethereum.EthereumHash
import org.scalacheck._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class EthereumHashTest extends FlatSpec with Matchers with PropertyChecks {

  behavior of "TransactionHash"

  def genTxHash: Gen[String] =
    for {
      low    <- Gen.choose[Char]('a', 'f')
      upper  <- Gen.choose[Char]('A', 'F')
      number <- Gen.choose[Char]('0', '9')
      hash   <- Gen.listOfN(64, Gen.oneOf(low, upper, number)).map(_.mkString)
    } yield hash

  it should "not throw exception with well-format" in {
    noException shouldBe thrownBy {
      forAll(genTxHash) { EthereumHash(_) }
    }
  }

  it should "not allow invalid hashes" in {
    def exceptionMessage(hash: String) = s"requirement failed: invalid transaction hash: $hash"
    (the[IllegalArgumentException] thrownBy EthereumHash("a" * 63) should have)
      .message(exceptionMessage("a" * 63))
    (the[IllegalArgumentException] thrownBy EthereumHash("a" * 65) should have)
      .message(exceptionMessage("a" * 65))
    (the[IllegalArgumentException] thrownBy EthereumHash("g" * 64) should have)
      .message(exceptionMessage("g" * 64))
  }

  it should "encode its value into a '0x'-refixed string" in {
    forAll(genTxHash) { hash =>
      EthereumHash(hash).toPrefixedHexString shouldBe s"0x$hash"
    }
  }

  it should "decode valid prefixed txHashes into TransactionHashes" in {
    forAll(genTxHash) { hash =>
      EthereumHash.decodePrefixedHexString(s"0x$hash") shouldBe Some(EthereumHash(hash))
    }
  }

  it should "not decode invalid prefixed txHashes" in {
    EthereumHash.decodePrefixedHexString("") shouldBe None
  }

}
