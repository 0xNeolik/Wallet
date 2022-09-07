package com.clluc.stockmind.core.transaction

import java.util.UUID

import com.clluc.stockmind.core.Caching._
import com.clluc.stockmind.core.ethereum.{EthereumAccount, Ethtoken, TransferEvent}
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.auth.OAuth1Info
import com.clluc.stockmind.core.user.User

import scala.concurrent.Future

trait RetrieveTransactionsCachingOps extends RetrieveTransactionsOps[Future] {

  private val ethereumAccountForUserCache =
    stdCacheInstance[UUID, Future[Option[EthereumAccount]]]()

  private val ethereumTokenBySymbolAndTypeCache =
    stdCacheInstance[String, Future[Option[Ethtoken]]]()

  private val ethereumAccountByAddressCache =
    stdCacheInstance[Address, Future[Option[EthereumAccount]]]()

  private val userInfoFromIdCache = stdCacheInstance[UUID, Future[Option[User]]]()

  private val onchainTxFromIdCache =
    stdCacheInstance[java.lang.Long, Future[Option[TransferEvent]]]()

  case class TransactionsPageCacheKey(userId: UUID,
                                      offset: Int,
                                      limit: Int,
                                      oAuth1Info: Option[OAuth1Info])

  abstract override def ethereumAccountForUser(userId: UUID): Future[Option[EthereumAccount]] =
    memoize(super.ethereumAccountForUser)(ethereumAccountForUserCache)(userId)

  abstract override def ethereumTokenBySymbolAndType(
      symbol_erc_type: String): Future[Option[Ethtoken]] =
    memoize(super.ethereumTokenBySymbolAndType)(ethereumTokenBySymbolAndTypeCache)(symbol_erc_type)

  abstract override def ethereumAccountByAddress(add: Address): Future[Option[EthereumAccount]] =
    memoize(super.ethereumAccountByAddress)(ethereumAccountByAddressCache)(add)

  abstract override def userInfoFromId(id: UUID): Future[Option[User]] =
    memoize(super.userInfoFromId)(userInfoFromIdCache)(id)

  // "Primitives" conversion required due to Scala / Java interoperability issues
  abstract override def onchainTxFromId(id: Long): Future[Option[TransferEvent]] =
    memoize[java.lang.Long, Future[Option[TransferEvent]]](l => super.onchainTxFromId(l.toLong))(
      onchainTxFromIdCache)(id)
}
