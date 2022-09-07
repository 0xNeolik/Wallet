package com.clluc.stockmind.core.ethereum

import com.clluc.stockmind.core.ethereum.solidity._

case class TokenFactoryContract(contractAddress: Address, owner: Address) {

  def createHumanStandardToken(initialAmount: Uint,
                               name: SolidityString,
                               decimals: Uint,
                               symbol: SolidityString,
                               tokenOwner: Address): EthTransaction = {
    EthTransaction(
      owner,
      contractAddress,
      Function("createHumanStandardToken", List(initialAmount, name, decimals, symbol, tokenOwner)),
      1500000)
  }

  def create721StandardToken(name: SolidityString,
                             symbol: SolidityString,
                             tokenOwner: Address): EthTransaction = {
    EthTransaction(owner,
                   contractAddress,
                   Function("createNFTCollection", List(name, symbol, tokenOwner)),
                   2500000)
  }

  def mintToken(amount: Uint, tokenAddress: Address): EthTransaction = {
    EthTransaction(owner, tokenAddress, Function("mint", List(owner, amount)), 1500000)
  }

  def mint721Token(userAddress: Address,
                   meta: SolidityString,
                   tokenAddress: Address): EthTransaction = {
    EthTransaction(owner, tokenAddress, Function("createNFT", List(userAddress, meta)), 1500000)
  }

  def burn721Token(id: Uint, tokenAddress: Address): EthTransaction = {
    EthTransaction(owner, tokenAddress, Function("deleteNFT", List(owner, id)), 1500000)
  }

  def burnToken(amount: Uint, tokenAddress: Address): EthTransaction = {
    EthTransaction(owner, tokenAddress, Function("burn", List(owner, amount)), 1500000)
  }

}

object TokenFactoryContract {

  val newTokenEvent    = "0x" + Event("NewToken", List(Address.default, Address.default)).encode
  val newToken721Event = "0x" + Event("NewTokenNFT", List(Address.default, Address.default)).encode

  val transferNFTEvent = "0x" + Event("TransferNFT",
                                      List(Address.default, Address.default, Uint(256))).encode

  val burnNFTEvent = "0x" + Event("BurnNFT", List(Address.default, Address.default, Uint(256))).encode

}
