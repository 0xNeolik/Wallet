package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.transaction.OffChainTransfer

import scala.concurrent.Future

/**
  * TODO Add Scaladoc.
  */
trait SettleTransferPort {
  def settlePendingTransfer(pendingTransferId: Long, transfer: OffChainTransfer): Future[Unit]
}
