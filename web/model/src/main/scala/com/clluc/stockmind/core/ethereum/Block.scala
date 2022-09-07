package com.clluc.stockmind.core.ethereum

import scala.util.Try

case class Block(blockNumber: Int) {
  require(blockNumber >= 0, "blocNumber must be >= 0")

  def toHex: String = "0x" + blockNumber.toHexString
}

object Block {

  def fromHexString(prefixedWith0x: String): Option[Block] = {
    if (prefixedWith0x.startsWith("0x")) {
      Try {
        val number = Integer.parseInt(prefixedWith0x.substring(2), 16)
        Block(number)
      }.toOption
    } else {
      None
    }
  }
}
