package com.clluc.stockmind.core.ethereum

import com.clluc.stockmind.core.ethereum.solidity.{Address, Function, Uint}

case class NFTTokenContract(contractAddress: Address) {

  val name = EthCall(contractAddress, Function("name"))

  val symbol = EthCall(contractAddress, Function("symbol"))

  def metadata(from: Address, id: Uint): EthCallFrom =
    EthCallFrom(from, contractAddress, Function("tokenURI", List(id)))

  def transfer(from: Address, to: Address, id: Uint): EthTransaction =
    EthTransaction(from, contractAddress, Function("transferFrom", List(from, to, id)))

  def getTransferEvents(from: Block): EthFilter = {
    EthFilter(from, List(contractAddress), List(List(TokenFactoryContract.transferNFTEvent)))
  }

}

object NFTTokenContract {

  def apply(token: Ethtoken): NFTTokenContract =
    NFTTokenContract(token.contract)

}
