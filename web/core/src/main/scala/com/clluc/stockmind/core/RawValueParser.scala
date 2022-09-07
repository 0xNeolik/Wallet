package com.clluc.stockmind.core

private[core] object RawValueParser {

  def parseIntoRawValue(amount: String, decimals: Int): Option[BigInt] = {
    def isValidNumber(amountSplit: Array[String], decimals: Int) =
      (amountSplit.length == 1 ||
        (amountSplit.length == 2 && amountSplit(1).length <= decimals)) &&
        amountSplit(0) != ""

    val splitAmount = amount.split('.')

    if (isValidNumber(splitAmount, decimals)) {
      val decimalAmount     = if (splitAmount.length == 2) splitAmount(1) else ""
      val completeDecAmount = decimalAmount + "0" * (decimals - decimalAmount.length)
      Some(BigInt(splitAmount(0) + completeDecAmount))
    } else None
  }
}
