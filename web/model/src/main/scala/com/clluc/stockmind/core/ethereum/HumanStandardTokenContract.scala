package com.clluc.stockmind.core.ethereum

import com.clluc.stockmind.core
import com.clluc.stockmind.core.ethereum.solidity.{Address, Function, Uint}

case class HumanStandardTokenContract(contractAddress: Address) {

  val totalSupply: EthCall =
    EthCall(contractAddress, Function("totalSupply"))

  def balanceOf(owner: Address): EthCall =
    EthCall(contractAddress, Function("balanceOf", List(owner)))

  val name = EthCall(contractAddress, Function("name"))

  val symbol = EthCall(contractAddress, Function("symbol"))

  val decimals = EthCall(contractAddress, Function("decimals"))

  def transfer(from: Address, to: Address, value: Uint): EthTransaction =
    EthTransaction(from, contractAddress, Function("transfer", List(to, value)))

  def mint(from: Address, to: Address, value: Uint): EthTransaction =
    EthTransaction(from, contractAddress, Function("mint", List(to, value)))

  def burn(from: Address, to: Address, value: Uint): EthTransaction =
    EthTransaction(from, contractAddress, Function("burn", List(to, value)))

  def getTransferEvents(from: Block): EthFilter = {
    // Event signatures are created by hashing the event name and the parameter signatures.
    // In this case, it is hash("Transfer(address,address,uint256)")
    //val transferEventSignature = Event("Transfer", List(Address.apply, Address.apply, Uint(256)))
    // But let's jut use the precalculated version
    EthFilter(from, List(contractAddress), List(List(core.ethereum.erc20TransferEventSignature)))
  }

  def getMintEvents(from: Block): EthFilter = {
    EthFilter(from, List(contractAddress), List(List(core.ethereum.erc20MintEventSignature)))
  }

  def getBurnEvents(from: Block): EthFilter = {
    EthFilter(from, List(contractAddress), List(List(core.ethereum.erc20BurnEventSignature)))
  }

}

object HumanStandardTokenContract {

  def apply(token: Ethtoken): HumanStandardTokenContract =
    HumanStandardTokenContract(token.contract)

}
