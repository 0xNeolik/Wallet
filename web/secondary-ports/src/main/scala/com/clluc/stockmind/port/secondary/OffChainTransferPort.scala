package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.transaction.OffChainTransfer

import scala.concurrent.Future

trait OffChainTransferPort {
  def create(tx: OffChainTransfer): Future[OffChainTransfer]

  def findTransfersInvolvingAddressAndType(address: Address,
                                           transaction_type: String): Future[List[OffChainTransfer]]

  def findTransfersInvolvingAddressPage(address: Address,
                                        limit: Int,
                                        offset: Int): Future[List[OffChainTransfer]]
  def find(id: Long): Future[Option[OffChainTransfer]]
  def linkToOnChainTxWithId(offchainTxId: Long, onchainTxId: Long): Future[OffChainTransfer]
}
