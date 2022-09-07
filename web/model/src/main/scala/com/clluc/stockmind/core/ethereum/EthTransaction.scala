package com.clluc.stockmind.core.ethereum

import com.clluc.stockmind.core.ethereum.solidity.{Address, Function}

/**
  * Represents a transaction sent from one address to another. The transactions
  * generated modify the blockchain by executing methods on smart contracts, or
  * move ether between accounts.
  * @param from Origin of the transaction.
  * @param to Destination of the transaction.
  * @param data Encoded transaction data.
  * @param value Amount of ether (in wei), written as a 0x-prefixed hex string.
  * @param gas Maximum amount of gas that can be spent on the transaction. If None,
  *            use Ethereum node's default value.
  */
case class EthTransaction(from: String,
                          to: Option[String],
                          data: String,
                          value: String = "0x0",
                          gas: Option[String] = None)

object EthTransaction {

  // Used to generate method calls on smart contracts
  def apply(sender: Address, destination: Address, data: Function, gas: Int): EthTransaction =
    EthTransaction(sender.toHex,
                   Some(destination.toHex),
                   data.toHex,
                   "0x0",
                   Some(s"0x${gas.toHexString}"))

  def apply(sender: Address, destination: Address, data: Function): EthTransaction =
    EthTransaction(sender.toHex, Some(destination.toHex), data.toHex)

  // Used to generate simple ether transfer transactions
  def apply(sender: Address, destination: Address, value: BigInt): EthTransaction =
    EthTransaction(sender.toHex, Some(destination.toHex), "0x", s"0x${value.toString(16)}")
}

// Utility class that groups together a transaction along with the password
// required to unlock the origin account
case class SignableTransaction(tx: EthTransaction, password: String)
