package com.clluc.stockmind.core.ethereum

import scala.util.Try

case class EthereumHash(hash: String) {
  private val txPattern = """[a-fA-F0-9]{64}"""
  require(hash.matches(txPattern), s"invalid transaction hash: $hash")

  def toPrefixedHexString: String = s"0x$hash"
}

object EthereumHash {

  def decodePrefixedHexString(prefixedWith0x: String): Option[EthereumHash] =
    Try(EthereumHash(prefixedWith0x.drop(2))).toOption
}
