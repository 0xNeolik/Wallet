package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.ethereum.Block
import org.scalatest.{FlatSpec, Matchers}

class BlockTest extends FlatSpec with Matchers {

  behavior of "Block"

  it should "reject negative block numbers" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      Block(-1)
    }
  }

  it should "hold the block number" in {
    Block(12345).blockNumber shouldBe 12345
  }

  it should "output the block number as a hex string" in {
    Block(63236).toHex shouldBe "0xf704"
  }

  it should "reject hex strings not starting with '0x'" in {
    Block.fromHexString("f704") shouldBe None
  }

  it should "reject invalid hex strings" in {
    Block.fromHexString("0xpera") shouldBe None
  }

  it should "reject negative hex strings" in {
    Block.fromHexString("0x-f704") shouldBe None
  }

  it should "parse a hex string into a valid Block" in {
    Block.fromHexString("0xf704") shouldBe Some(Block(63236))
  }

}
