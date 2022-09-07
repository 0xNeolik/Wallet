package com.clluc.stockmind.core.transaction

import com.clluc.stockmind.core.ethereum.EthereumHash

case class OutboundTransfer(transactionHash: EthereumHash, offchainTransferId: Long)
