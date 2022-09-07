package com.clluc.stockmind.core.transaction

import com.clluc.stockmind.core.ethereum.EthereumHash

case class InboundTransfer(firstStepId: Long, secondStepHash: EthereumHash)
