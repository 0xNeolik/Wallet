package com.clluc.stockmind.core.ethereum

import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import org.joda.time.DateTime

case class Transaction(
    hash: EthereumHash,
    from: Address,
    to: Option[Address], // Contract creation transactions have no specific destination
    blockNumber: Block,
    txIndex: Int,
    value: Uint
) {

  def toEthTransferEvent(processedDate: Option[DateTime]) = {
    TransferEvent(0L,
                  "ETH",
                  "ERC-20",
                  from,
                  to.getOrElse(Address.default),
                  value,
                  blockNumber,
                  hash,
                  txIndex,
                  processedDate)
  }
}
