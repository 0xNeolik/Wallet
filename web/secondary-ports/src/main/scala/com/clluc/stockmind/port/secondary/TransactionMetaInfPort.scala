package com.clluc.stockmind.port.secondary

import scala.concurrent.Future

/**
  * Defines operations to store and retrieve metadata related to an specific (offchain) transaction id.
  */
trait TransactionMetaInfPort {

  /**
    * Saves the given key / value pairs as metadata for the transaction with the given id
    * @param txId The id of the off chain transaction to which this metadata will be bounded.
    *             As every transaction in the system (regardless of it being off or on chain)
    *             has an off chain representation we take the off chain id as the value of this
    *             attribute.
    * @param metaInf A set of key / value pairs. Each key represents the name of a meta info
    *                attribute, and the value is (obviously) it's value.
    *                This is a way to generalize any data that a user of the platform might
    *                want to associate and store within a transaction. Some users (or customers,
    *                whatever you want to call them) might want to store different things than
    *                others. This representation allows not only an arbitrary number of attributes,
    *                but also to give an arbitrary name to each one.
    * @return An empty future representing an eventual success or failure in the operation.
    */
  def saveMetaInf(txId: Long, metaInf: Map[String, String]): Future[Unit]

  /**
    * Reads the metadata associated with a given transaction
    * @param txId The offchain transaction id for which we want the metadata to be retrieved.
    *             Apply also to on chain transactions, that have also an offchain counterpart.
    * @return A future representing the transaction metadata (key / value pairs).
    *         For full details about how transaction metadata is modeled, see Scaladoc for the
    *         saveMetaInf method.
    */
  def readMetaInf(txId: Long): Future[Map[String, String]]
}
