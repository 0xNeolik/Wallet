package com.clluc.stockmind.core.ethereum

import com.clluc.stockmind.core.ethereum.solidity.{Address, Function}

/**
  * Represents a call to a constant function of a contract
  */
case class EthCall(to: String, data: String)
case class EthCallFrom(from: String, to: String, data: String)

object EthCall {

  def apply(address: Address, data: Function): EthCall =
    EthCall(address.toHex, data.toHex)
}

object EthCallFrom {

  def apply(from: Address, address: Address, data: Function): EthCallFrom =
    EthCallFrom(from.toHex, address.toHex, data.toHex)
}
