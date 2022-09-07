package com.clluc.stockmind.adapter.postgres

import java.util.UUID

import com.clluc.stockmind.core.ethereum.EthereumAccount

import scala.concurrent.{ExecutionContext, Future}
import doobie.imports._
import doobie.postgres.pgtypes.UuidType
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.port.secondary.EthereumAccountPort

private class PostgresEthereumAccountAdapter(val transactor: Transactor[IOLite])(
    override implicit val executionContext: ExecutionContext
) extends EthereumAccountPort
    with Dao {

  override def findAccountByUserId(user: UUID): Future[Option[EthereumAccount]] = {
    def query(user: UUID) = sql"""
      SELECT *
      FROM ethereum_accounts
      WHERE
        user_id = $user
     """.query[EthereumAccount]

    selectOne(query(user))
  }

  override def findAccountByAddress(address: Address): Future[Option[EthereumAccount]] = {
    def query(address: Address) = sql"""
      SELECT *
      FROM ethereum_accounts
      WHERE
        address = ${address.value}
     """.query[EthereumAccount]

    selectOne(query(address))
  }

  override def saveAccount(ethereumAccount: EthereumAccount): Future[EthereumAccount] = {
    def query(user: UUID, address: String, password: String): ConnectionIO[EthereumAccount] =
      sql"""
      INSERT INTO ethereum_accounts
        (user_id, address, password)
      VALUES
        ($user, $address, $password)
      ON CONFLICT (user_id) DO UPDATE
      SET
        user_id = $user,
        address = $address,
        password = $password
      WHERE
        ethereum_accounts.user_id = $user
    """.update
        .withUniqueGeneratedKeys("user_id", "address", "password")

    val (user, address, password) = EthereumAccount.unapply(ethereumAccount).get
    insertWithFeedback(query(user, address.value, password))
  }

  // TODO This method contains logic that eventually will have to be included in the core module
  // It uses conceptually two different adapters: postgres and ethereum. The piece in charge of
  // composing both in a single piece of business logic should be core.
  // For now and in order to make commits more atomic and simpler we leave it not implemented.
  override def accountFor(userId: UUID): Future[(EthereumAccount, Boolean)] = ???
}

object PostgresEthereumAccountAdapter {

  def apply(tx: Transactor[IOLite])(
      implicit executionContext: ExecutionContext): EthereumAccountPort =
    new PostgresEthereumAccountAdapter(tx)
}
