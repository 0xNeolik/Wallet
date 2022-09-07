package com.clluc.stockmind.core.transaction

import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import org.joda.time.DateTime

case class OffChainTransfer(
    id: Long = 0,
    tokenSymbol: String,
    erc_type: String,
    from: Address,
    to: Address,
    amount: Uint,
    created: DateTime,
    onchainTransferId: Option[Long] = None,
    token_id: Option[BigInt] = None
)
