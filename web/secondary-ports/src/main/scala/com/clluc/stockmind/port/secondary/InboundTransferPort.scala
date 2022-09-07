package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.transaction.InboundTransfer

import scala.concurrent.Future

trait InboundTransferPort {

  def create(it: InboundTransfer): Future[InboundTransfer]
  def findByFirstStep(stepId: Long): Future[Option[InboundTransfer]]
  def findBySecondStep(stepTxHash: String): Future[Option[InboundTransfer]]

}
