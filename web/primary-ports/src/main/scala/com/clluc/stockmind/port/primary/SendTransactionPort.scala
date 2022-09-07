package com.clluc.stockmind.port.primary

import com.clluc.stockmind.core.transaction.TokenTransactionError.ValidatedTransaction
import com.clluc.stockmind.core.transaction.{
  TransactionRequest,
  TransactionRequest721,
  TransactionRequestUser,
  TransactionRequestUser721,
  TransactionResult
}
import org.joda.time.DateTime

import scala.concurrent.Future

/**
  * TODO Add Scaladoc.
  */
trait SendTransactionPort {

  def sendTransaction(
      request: TransactionRequest,
      oauthKey: String,
      oauthSecret: String,
      timestampFx: => DateTime,
      masterAccountPassword: String
  ): Future[ValidatedTransaction[TransactionResult]]

  def sendTransaction721(
      request: TransactionRequest721,
      oauthKey: String,
      oauthSecret: String,
      timestampFx: => DateTime,
      masterAccountPassword: String
  ): Future[ValidatedTransaction[TransactionResult]]

  def sendTransactionUser(
      request: TransactionRequestUser,
      timestampFx: => DateTime,
      masterAccountPassword: String
  ): Future[ValidatedTransaction[TransactionResult]]

  def sendTransactionUser721(
      request: TransactionRequestUser721,
      timestampFx: => DateTime,
      masterAccountPassword: String
  ): Future[ValidatedTransaction[TransactionResult]]

  def settlePendingTransfers(oauthProvider: String,
                             destinationUserKey: String,
                             timestampFx: => DateTime): Future[List[ValidatedTransaction[Unit]]]

  def settlePendingTransfersUser(oauthProvider: String,
                                 destinationUserEmail: String,
                                 timestampFx: => DateTime): Future[List[ValidatedTransaction[Unit]]]
}
