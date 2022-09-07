package com.clluc.stockmind.core.ethereum

import com.clluc.stockmind.core.ethereum.solidity.Address

// Represents a Ethereum node filter. Filters are used to comb through the
// blockchain logs. The logs store events written by Ethereum contracts, among
// other things.
case class EthFilter(fromBlock: String,
                     toBlock: String,
                     address: List[String],
                     topics: List[List[String]])

object EthFilter {

  def apply(fromBlock: Block, addresses: List[Address], topics: List[List[String]]): EthFilter =
    EthFilter(fromBlock.toHex, "latest", addresses.map(_.toHex), topics)

  def apply(fromBlock: Block,
            toBlock: Block,
            addresses: List[Address],
            topics: List[List[String]]): EthFilter =
    EthFilter(fromBlock.toHex, toBlock.toHex, addresses.map(_.toHex), topics)
}
