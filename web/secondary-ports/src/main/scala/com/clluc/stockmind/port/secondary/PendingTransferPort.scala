package com.clluc.stockmind.port.secondary

import java.util.UUID

import com.clluc.stockmind.core.auth.LoginInfo
import com.clluc.stockmind.core.transaction.PendingTransfer
import org.joda.time.DateTime

import scala.concurrent.Future

trait PendingTransferPort {
  def create(transfer: PendingTransfer): Future[PendingTransfer]

  def findById(id: Long): Future[Option[PendingTransfer]]

  def findPendingByOriginAndType(userId: UUID, pending_type: String): Future[List[PendingTransfer]]

  def findPendingByOrigin(userId: UUID): Future[List[PendingTransfer]]

  def findPendingByDestination(loginInfo: LoginInfo): Future[List[PendingTransfer]]

  def markAsProcessed(id: Long, timestamp: DateTime): Future[PendingTransfer]

  def delete(id: Long): Future[Unit]
}
