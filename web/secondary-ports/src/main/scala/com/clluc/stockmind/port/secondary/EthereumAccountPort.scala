package com.clluc.stockmind.port.secondary

import java.util.UUID

import com.clluc.stockmind.core.ethereum.EthereumAccount
import com.clluc.stockmind.core.ethereum.solidity.Address

import scala.concurrent.{ExecutionContext, Future}

/**
  * Secondary port that defines the contract we need to retrieve information from an Ethereum account.
  */
trait EthereumAccountPort {

  /**
    * Finds an ethereum account by its user ID.
    *
    * @param user The ID of the user to find his ethereum account.
    * @return The found ethereum account or None if no ethereum account
    *         for the given user ID could be found.
    */
  def findAccountByUserId(user: UUID): Future[Option[EthereumAccount]]

  /**
    * Finds a ethereum account by its address.
    *
    * @param address The ethereum address to find.
    * @return The found ethereum account or None if no ethereum account
    *         for the given address could be found.
    */
  def findAccountByAddress(address: Address): Future[Option[EthereumAccount]]

  /**
    * Saves a ethereum account.
    *
    * @param userAccount The ethereum account to save.
    * @return The saved ethereum account.
    */
  def saveAccount(userAccount: EthereumAccount): Future[EthereumAccount]

  /**
    * Retrieves the Ethereum account associated to a user if there is one. Else,
    * create and return a new Ethereum account.
    * @param userId A user's id.
    * @return A new or existing Ethereum account, along with a boolean marking
    *         whether the account has been newly created instead of retrieved
    *         (true if created, false if retrieved).
    */
  // TODO Think about splitting this operation in several more granular ones
  // This method contains a bit of logic that would be nice to unit test mocking different database access results.
  // For that we need to separate the logic for queries from the logic from the ethereum client from the logic that
  // combines both.
  def accountFor(userId: UUID): Future[(EthereumAccount, Boolean)]

  // From here, derived operations (starting to make this trait a rich interface)
  def doesEthereumAddressBelongToAStockmindUser(address: Address)(
      implicit executionContext: ExecutionContext): Future[Boolean] =
    findAccountByAddress(address).map(_.isDefined)
}
