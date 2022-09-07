package com.clluc.stockmind.core.transaction

import java.util.UUID

import com.clluc.stockmind.core.ethereum.Amount
import com.clluc.stockmind.core.ethereum.solidity.Address
//import com.clluc.stockmind.core.auth.OAuth1Info
import com.clluc.stockmind.core.transaction.CancelPendingTransaction._
import com.clluc.stockmind.core.transaction.StockmindTransaction.ValidatedTxRetrievalResult
import com.clluc.stockmind.port.primary.RetrieveTransactionsPort
import com.clluc.stockmind.port.secondary._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

import cats.instances.future._ // Bring in Monad[Future]

private[transaction] class RetrieveTransactionsAdapter(
    ethereumAccountPort: EthereumAccountPort,
    offChainTransferPort: OffChainTransferPort,
    pendingTransferPort: PendingTransferPort,
    erc20InfoPort: Erc20InfoPort,
    erc721InfoPort: Erc721InfoPort,
    erc20TransferEventPort: Erc20TransferEventPort,
    userPort: UsersRepositoryPort
)(
    implicit
    executionContext: ExecutionContext
) extends RetrieveTransactionsPort
    with RetrieveTransactionsOps[Future]
    with LazyLogging {

  override def ethereumAccountForUser(userId: UUID) =
    ethereumAccountPort.findAccountByUserId(userId)

  override def offchainTransfers(addressInvolved: Address, transaction_type: String) =
    offChainTransferPort.findTransfersInvolvingAddressAndType(addressInvolved, transaction_type)

  override def ethereumTokenBySymbolAndType(symbol_erc_type: String) =
    erc20InfoPort.findEthereumTokenBySymbolAndType(symbol_erc_type)

  override def erc20TokenBySymbolAndType(symbol_erc_type: String) =
    erc20InfoPort.findErc20TokenBySymbolAndType(symbol_erc_type)

  override def erc721TokenById(id: BigInt) =
    erc721InfoPort.findErc721tokenByTokenId(id)

  override def pendingTransfersByIssuer(sourceUserId: UUID, transaction_type: String) =
    pendingTransferPort.findPendingByOriginAndType(sourceUserId, transaction_type)

  override def ethereumAccountByAddress(add: Address) =
    ethereumAccountPort.findAccountByAddress(add)

  override def userInfoFromId(id: UUID) = userPort.find(id)

  override def onchainTxFromId(id: Long) = erc20TransferEventPort.find(id)

  override def findTransactionsPage(
      userId: UUID,
      offset: Int,
      numOfTxs: Int
  ): Future[ValidatedTxRetrievalResult[List[StockmindTransaction]]] = {
    transactionsPage(
      userId,
      offset,
      numOfTxs,
      "ERC-20"
    )
  }

  override def find721TransactionsPage(
      userId: UUID,
      offset: Int,
      numOfTxs: Int
  ): Future[ValidatedTxRetrievalResult[List[Stockmind721Transaction]]] = {
    transactionsPage721(
      userId,
      offset,
      numOfTxs,
      "ERC-721"
    )
  }

  private def transactionInfo(tx: OffChainTransfer,
                              userAddress: Address): Future[(StockmindTransaction, Long)] = {
    def _getUserId(address: Address): Future[Option[UUID]] = {
      ethereumAccountPort.findAccountByAddress(address).map(_.map(_.user))
    }

    def _getOnchainTx(id: Option[Long]) = id match {
      case Some(onchainId) =>
        erc20TransferEventPort.find(onchainId)
      case None =>
        Future.successful(None)
    }

    for {
      tokenO <- erc20InfoPort.findErc20TokenBySymbolAndType(tx.tokenSymbol + "|" + tx.erc_type)
      token         = tokenO.get
      tokenSymbol   = token.symbol
      erc_type      = token.erc_type
      tokenDecimals = tokenO.get.decimals
      amount        = Amount.fromRawIntegerValue(tx.amount.value.toString, tokenDecimals)
      userIdFrom <- _getUserId(tx.from)
      userIdTo   <- _getUserId(tx.to)
      dateString = tx.created
      onchainTxO <- _getOnchainTx(tx.onchainTransferId)
    } yield {
      val tokenAmount = TokenAmount(amount.integerPart, amount.decimalPart)

      def _buildTransaction(direction: TransactionDirection,
                            counterparty: Counterparty): StockmindTransaction = {
        StockmindTransaction(
          id = tx.id,
          direction = direction,
          pending = false,
          counterparty = counterparty,
          token = tokenSymbol,
          erc_type = erc_type,
          tokenDescription = token.name,
          decimals = tokenDecimals,
          amount = tokenAmount,
          txHash = None,
          date = dateString
        )
      }

      val transaction = (userIdFrom, userIdTo) match {
        case (None, Some(_)) => // Inbound transfer
          val onchainTxAddress = onchainTxO.map(_.from.toHex).getOrElse("0x")
          val counterparty     = Counterparty(Some(onchainTxAddress), None)
          _buildTransaction(IncomingTx, counterparty)

        case (Some(_), None) => // Outbound transfer
          val onchainTxAddress = onchainTxO.map(_.to.toHex).getOrElse("0x")
          val counterparty     = Counterparty(Some(onchainTxAddress), None)
          _buildTransaction(OutgoingTx, counterparty)

        case (Some(_), Some(_)) => // Offchain transfer
          if (tx.from.value == userAddress.value) {
            val counterparty =
              Counterparty(Some(tx.to.toHex), None)
            _buildTransaction(OutgoingTx, counterparty)
          } else {
            val counterparty =
              Counterparty(Some(tx.from.toHex), None)
            _buildTransaction(IncomingTx, counterparty)
          }

        case (None, None) => // should not happen
          logger.error(
            s"Error when processing $tx from $userAddress: neither origin nor destination have screennames")
          ???
      }

      (transaction, tx.created.getMillis)
    }

  }

  private def transactionInfo721(tx: OffChainTransfer,
                                 userAddress: Address,
                                 token_id: BigInt): Future[(Stockmind721Transaction, Long)] = {
    def _getUserId(address: Address): Future[Option[UUID]] = {
      ethereumAccountPort.findAccountByAddress(address).map(_.map(_.user))
    }

    def _getOnchainTx(id: Option[Long]) = id match {
      case Some(onchainId) =>
        erc20TransferEventPort.find(onchainId)
      case None =>
        Future.successful(None)
    }

    for {
      tokenO <- erc721InfoPort.findErc721tokenByIdAndOwner(token_id, userAddress)
      token       = tokenO.get
      tokenSymbol = token.symbol
      erc_type    = token.erc_type
      meta        = token.meta
      id          = token.id
      userIdFrom <- _getUserId(tx.from)
      userIdTo   <- _getUserId(tx.to)
      dateString = tx.created
      onchainTxO <- _getOnchainTx(tx.onchainTransferId)
    } yield {

      def _buildTransaction(direction: TransactionDirection,
                            counterparty: Counterparty): Stockmind721Transaction = {
        Stockmind721Transaction(
          id = tx.id,
          direction = direction,
          pending = false,
          counterparty = counterparty,
          token = tokenSymbol,
          erc_type = erc_type,
          tokenDescription = token.name,
          meta = token.meta,
          token_id = token.id,
          txHash = None,
          date = dateString
        )
      }

      val transaction = (userIdFrom, userIdTo) match {
        case (None, Some(_)) => // Inbound transfer
          val onchainTxAddress = onchainTxO.map(_.from.toHex).getOrElse("0x")
          val counterparty     = Counterparty(Some(onchainTxAddress), None)
          _buildTransaction(IncomingTx, counterparty)

        case (Some(_), None) => // Outbound transfer
          val onchainTxAddress = onchainTxO.map(_.to.toHex).getOrElse("0x")
          val counterparty     = Counterparty(Some(onchainTxAddress), None)
          _buildTransaction(OutgoingTx, counterparty)

        case (Some(_), Some(_)) => // Offchain transfer
          if (tx.from.value == userAddress.value) {
            val counterparty =
              Counterparty(Some(tx.to.toHex), None)
            _buildTransaction(OutgoingTx, counterparty)
          } else {
            val counterparty =
              Counterparty(Some(tx.from.toHex), None)
            _buildTransaction(IncomingTx, counterparty)
          }

        case (None, None) => // should not happen
          logger.error(
            s"Error when processing $tx from $userAddress: neither origin nor destination have screennames")
          ???
      }

      (transaction, tx.created.getMillis)
    }

  }

  override def findTransactionById(userId: UUID,
                                   txId: Long): Future[Option[StockmindTransaction]] = {

    val eventualTxs: Future[List[StockmindTransaction]] =
      for {
        ethereumAccountO <- ethereumAccountPort.findAccountByUserId(userId)
        ethereumAccount = ethereumAccountO.get
        tx <- offChainTransferPort.find(txId)
        transaction <- Future.sequence {
          tx.toList.map(transactionInfo(_, ethereumAccount.address))
        }
      } yield transaction.map(_._1)

    eventualTxs.map { l =>
      // Gross, unfriendly sanity check that in case of failing due to a lack of data integrity will give our API clients a nasty message
      // TODO Reflection: do we want to capture here a more API client friendly approach using fail fast, monadic error handling?
      require(
        Set(0, 1).contains(l.size),
        s"Expected to find none or at most 1 transaction for user $userId and txId $txId. Actually found ${l.size}")

      l.headOption
    }
  }

  override def find721TransactionById(userId: UUID,
                                      txId: Long): Future[Option[Stockmind721Transaction]] = {

    val eventualTxs: Future[List[Stockmind721Transaction]] =
      for {
        ethereumAccountO <- ethereumAccountPort.findAccountByUserId(userId)
        ethereumAccount = ethereumAccountO.get
        tx <- offChainTransferPort.find(txId)
        transaction <- Future.sequence {
          tx.toList.map(transactionInfo721(_, ethereumAccount.address, tx.get.token_id.get))
        }
      } yield transaction.map(_._1)

    eventualTxs.map { l =>
      // Gross, unfriendly sanity check that in case of failing due to a lack of data integrity will give our API clients a nasty message
      // TODO Reflection: do we want to capture here a more API client friendly approach using fail fast, monadic error handling?
      require(
        Set(0, 1).contains(l.size),
        s"Expected to find none or at most 1 transaction for user $userId and txId $txId. Actually found ${l.size}")

      l.headOption
    }
  }

  override def findPendingTransactionById(txId: Long): Future[Option[StockmindTransaction]] = {
    val eventualMaybeEventualTx: Future[Option[Future[StockmindTransaction]]] = pendingTransferPort
      .findById(txId)
      .map(_.map { pt =>
        for {
          tokenO <- erc20InfoPort.findErc20TokenBySymbolAndType(pt.tokenSymbol + "|" + pt.erc_type)
          token        = tokenO.get
          counterparty = Counterparty(None, None)
          amount       = Amount.fromRawIntegerValue(pt.amount.toString, token.decimals)
          date         = pt.created
        } yield
          StockmindTransaction(
            pt.id,
            OutgoingTx,
            pt.processed.isEmpty,
            counterparty,
            pt.tokenSymbol,
            pt.erc_type,
            token.name,
            token.decimals,
            TokenAmount(amount.integerPart, amount.decimalPart),
            None,
            date
          )
      })

    // This is a natural transformation: Option ~> List
    // For sure this logic is already implemented in some way in cats.
    // not willing to spend time on finding it at this very moment; it's a quite simple one.
    val eventualTransactions: Future[List[StockmindTransaction]] = {
      val isomorphicEventualTx: Future[List[Future[StockmindTransaction]]] =
        eventualMaybeEventualTx.map(_.toList)

      isomorphicEventualTx.map(Future.sequence(_)).flatMap(identity)
    }

    // Natural transformation back List ~> Option
    eventualTransactions.map {
      case (head :: _) => Some(head)
      case Nil         => None
    }
  }

  override def findPending721TransactionById(
      txId: Long): Future[Option[Stockmind721Transaction]] = {
    val eventualMaybeEventualTx: Future[Option[Future[Stockmind721Transaction]]] =
      pendingTransferPort
        .findById(txId)
        .map(_.map { pt =>
          for {
            tokenO <- erc721InfoPort.findErc721tokenByTokenId(pt.token_id.get)
            token        = tokenO.get
            counterparty = Counterparty(None, None)
            date         = pt.created
          } yield
            Stockmind721Transaction(
              pt.id,
              OutgoingTx,
              pt.processed.isEmpty,
              counterparty,
              pt.tokenSymbol,
              pt.erc_type,
              token.name,
              token.meta,
              token.id,
              None,
              date
            )
        })

    // This is a natural transformation: Option ~> List
    // For sure this logic is already implemented in some way in cats.
    // not willing to spend time on finding it at this very moment; it's a quite simple one.
    val eventualTransactions: Future[List[Stockmind721Transaction]] = {
      val isomorphicEventualTx: Future[List[Future[Stockmind721Transaction]]] =
        eventualMaybeEventualTx.map(_.toList)

      isomorphicEventualTx.map(Future.sequence(_)).flatMap(identity)
    }

    // Natural transformation back List ~> Option
    eventualTransactions.map {
      case (head :: _) => Some(head)
      case Nil         => None
    }
  }

  override def cancelPendingTransactionById(
      txId: Long,
      transactionRequesterUserId: UUID
  ): Future[Either[CancelPendingTransactionError, Unit]] = {

    import cats.syntax.either._

    pendingTransferPort.findById(txId).flatMap {
      case Some(pt) =>
        if ((pt.fromUser == transactionRequesterUserId) && pt.processed.isEmpty) {
          logger.info(s"Cancelling pending transaction: $pt")
          pendingTransferPort.delete(txId).map(_.asRight[CancelPendingTransactionError])
        } else {
          logger.info(
            s"User with id $transactionRequesterUserId tried to cancel pending transaction $txId without permission")
          Future.successful(
            txNotFromTheCancelActionRequester(transactionRequesterUserId, txId).asLeft[Unit])
        }

      case None => Future.successful(pendingTxNotFound(txId).asLeft[Unit])
    }
  }
}

object RetrieveTransactionsAdapter {

  def apply(
      ethereumAccountPort: EthereumAccountPort,
      offchainTransferPort: OffChainTransferPort,
      pendingTransferPort: PendingTransferPort,
      erc20InfoPort: Erc20InfoPort,
      erc721InfoPort: Erc721InfoPort,
      erc20TransferEventPort: Erc20TransferEventPort,
      userPort: UsersRepositoryPort
  )(
      implicit
      executionContext: ExecutionContext
  ): RetrieveTransactionsAdapter =
    new RetrieveTransactionsAdapter(
      ethereumAccountPort,
      offchainTransferPort,
      pendingTransferPort,
      erc20InfoPort,
      erc721InfoPort,
      erc20TransferEventPort,
      userPort
    ) with RetrieveTransactionsCachingOps // This mixing wires the actual caching behaviour
}
