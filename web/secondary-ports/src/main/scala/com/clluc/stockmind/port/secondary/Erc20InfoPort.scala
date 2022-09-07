package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.ethereum.Erc20Token
import com.clluc.stockmind.core.ethereum.Ethtoken
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.user.Balance

import scala.concurrent.Future

/**
  * Port to retrieve information related to er20 token related entities.
  */
trait Erc20InfoPort {
  def findBalancesForAddress(ethAddress: Address): Future[List[Balance]]
  def findEthereumTokenBySymbolAndType(symbol_erc_type: String): Future[Option[Ethtoken]]

  def findEthereumTokenBySymbolAndTypeAndOwner(symbol: String,
                                               erc_type: String,
                                               ownerAddress: Address): Future[Option[Ethtoken]]

  def findEthereumTokenByNameAndSymbol(name: String, erc_type: String): Future[Option[Ethtoken]]

  def findBalanceByAddressAndEthereumToken(ethAddress: Address,
                                           token: String): Future[Option[Balance]]
  def findEthereumTokenByAddress(contractAddress: Address): Future[Option[Ethtoken]]

  def findAllErc20Tokens(): Future[List[Ethtoken]]

  def findValidTypes(erc_type: String): Future[Option[String]]

  def createEthereumToken(token: Erc20Token): Future[Erc20Token]

  def findErc20TokenBySymbolAndType(symbol_erc_type: String): Future[Option[Erc20Token]]

}
