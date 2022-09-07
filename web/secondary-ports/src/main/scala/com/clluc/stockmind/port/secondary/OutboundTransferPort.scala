package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.ethereum.EthereumHash
import com.clluc.stockmind.core.transaction.OutboundTransfer

import scala.concurrent.Future

trait OutboundTransferPort {

  def create(ot: OutboundTransfer): Future[OutboundTransfer]
  def findByTxHash(txHash: EthereumHash): Future[Option[OutboundTransfer]]

}
