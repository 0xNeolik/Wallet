package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.core.ethereum.solidity.Address

import scala.concurrent.Future

trait Erc20TransferEventPort {
  def createTransferEvent(transferEvent: TransferEvent): Future[TransferEvent]

  def findTransfersInvolvingAddress(address: Address): Future[List[TransferEvent]]

  def findTransfersInvolvingAddressPage(address: Address,
                                        limit: Int,
                                        offset: Int): Future[List[TransferEvent]]
  def find(id: Long): Future[Option[TransferEvent]]
}
