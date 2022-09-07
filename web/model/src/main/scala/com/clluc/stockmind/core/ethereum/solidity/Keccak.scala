package com.clluc.stockmind.core.ethereum.solidity

private[ethereum] object Keccak {

  def encode(input: Array[Byte]): Array[Byte] = {
    new Keccak256().digest(input)
  }

}
