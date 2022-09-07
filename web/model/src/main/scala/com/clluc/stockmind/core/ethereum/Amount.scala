package com.clluc.stockmind.core.ethereum

case class Amount(integerPart: String, decimalPart: String)

object Amount {

  def fromRawIntegerValue(amount: String, decimals: Int) = {
    val completeAmount =
      if (amount.length <= decimals) "0" * (decimals - amount.length + 1) + amount else amount
    val (int, decimal) = completeAmount.splitAt(completeAmount.length - decimals)
    Amount(int, decimal)
  }

}
