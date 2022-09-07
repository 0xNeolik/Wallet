package com.clluc.stockmind.core.ethereum

import com.clluc.stockmind.core.ethereum.solidity.Address

/*case class Ethtoken(symbol: String,
                    erc_type: String,
                    name: String,
                    contract: Address,
                    owner: Option[String],
                    birthBlock: Option[Int])*/

case class Erc721Token(symbol: String,
                       erc_type: String,
                       name: String,
                       meta: String,
                       id: BigInt,
                       contract: Address,
                       owner: Option[String],
                       birthBlock: Option[Int])

case class Erc20Token(symbol: String,
                      erc_type: String,
                      name: String,
                      decimals: Int,
                      contract: Address,
                      owner: Option[String],
                      birthBlock: Option[Int])

sealed trait EthereumToken
case object Ether extends EthereumToken
case class Ethtoken(symbol: String,
                    erc_type: String,
                    name: String,
                    contract: Address,
                    owner: Option[String],
                    birthBlock: Option[Int])
    extends EthereumToken
