package com.clluc.stockmind.port.primary

import java.util.UUID

import com.clluc.stockmind.core.ethereum.CreateOrRetrieveEthAccountOutcome.CreateOrRetrieveAccountResult

import scala.concurrent.Future

trait EthereumAccountOperationsPort {
  def createOrRetrieveAccountFor(userId: UUID): Future[CreateOrRetrieveAccountResult]
}
