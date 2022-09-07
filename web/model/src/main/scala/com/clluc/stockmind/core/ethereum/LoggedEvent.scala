package com.clluc.stockmind.core.ethereum

import com.clluc.stockmind.core.ethereum.solidity.Address

case class LoggedEvent(
    origin: Address,
    block: Block,
    topics: List[String],
    data: String,
    txHash: EthereumHash,
    txIndex: Int,
    blockHash: EthereumHash
)
