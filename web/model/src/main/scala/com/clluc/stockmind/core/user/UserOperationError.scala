package com.clluc.stockmind.core.user

import java.util.UUID

sealed trait UserOperationError {
  def message(): String
}

case class UserWithoutEthereumAccountInSystem(userId: UUID) extends UserOperationError {
  override def message(): String =
    s"User with id $userId doesn't have a known ethereum account in the system"
}

case class UserDoesNotHaveLocalDirectoryEntry(entryId: LocalDirectoryEntryId)
    extends UserOperationError {
  override def message(): String =
    s"User with directory ID $entryId does not have a local directory entry"
}

case class TokenFoundInBalanceDoesNotExist(userId: UUID) extends UserOperationError {
  override def message(): String =
    s"A token that is present in the user (with id $userId) balance is not registered as supported in Stockmind"
}

case class IOError(reason: String) extends UserOperationError {
  override def message(): String =
    s"Error in IO: $reason"
}

object UserOperationError {

  def userWithoutEthereumAccountInSystem(userId: UUID): UserOperationError =
    UserWithoutEthereumAccountInSystem(userId)

  def userDoesNotHaveLocalDirectoryEntry(entryId: LocalDirectoryEntryId): UserOperationError =
    UserDoesNotHaveLocalDirectoryEntry(entryId)

  def tokenFoundInBalanceDoesNotExist(userId: UUID): UserOperationError =
    TokenFoundInBalanceDoesNotExist(userId)

  def ioError(reason: String): UserOperationError =
    IOError(reason)
}
