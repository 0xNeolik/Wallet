package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.ethereum.Ethtoken
import com.clluc.stockmind.core.ethereum.Erc721Token
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}

import scala.concurrent.Future

/**
  * Port to retrieve information related to er721 token related entities.
  */
trait Erc721InfoPort {

  def create721CollectionToken(token: Ethtoken): Future[Ethtoken]

  def create721Token(token: Ethtoken, metadata: String, NFTid: Uint): Future[Erc721Token]

  def findEthereumTokenByAddress(contractAddress: Address): Future[Option[Erc721Token]]

  def findEthTokenByAddress(contractAddress: Address): Future[Option[Ethtoken]]

  def findAllErc721Tokens(): Future[List[Ethtoken]]

  def findEthereum721TokenByUniqueId(id: BigInt, contractAddress: Address): Future[Option[Ethtoken]]

  def findErc721tokenByIdAndOwner(id: BigInt, tokenOwner: Address): Future[Option[Erc721Token]]

  def findErc721tokenByTokenId(id: BigInt): Future[Option[Erc721Token]]

  def deleteTokenFromId(id: Uint): Future[Unit]

  def findERC721tokensForAddress(contractAddress: Address): Future[List[Erc721Token]]

}
