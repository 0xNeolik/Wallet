package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.core.ethereum.solidity.{Address, Event, Function, Uint}
import org.scalatest.{FlatSpec, Matchers}

class HumanStandardTokenContractTest extends FlatSpec with Matchers {

  val contract      = Address("8b10301e990840cc78eab1ed2d0fcbede8ff219c")
  val creator       = Address("f06dcd243760d3abc748ae445ea876266a142b99")
  val to            = Address("e9ea50e96fb81432edd6605109257ac71930c574")
  val hundredTokens = Uint(256, 100)
  val token         = Erc20Token("BLV", "ERC-20", "Believe", 0, contract, None, None)

  val humanStandardTokenContract = HumanStandardTokenContract(
    Ethtoken(token.symbol,
             token.erc_type,
             token.name,
             token.contract,
             token.owner,
             token.birthBlock)
  )

  behavior of "HumanStandardToken"

  it should "create a total supply Ethereum call" in {
    val ethCall         = humanStandardTokenContract.totalSupply
    val expectedEthCall = EthCall(contract, Function("totalSupply"))

    ethCall shouldBe expectedEthCall
  }

  it should "create a balance of Ethereum call" in {
    val ethCall         = humanStandardTokenContract.balanceOf(creator)
    val expectedEthCall = EthCall(contract, Function("balanceOf", List(creator)))

    ethCall shouldBe expectedEthCall
  }

  it should "create a transfer Ethereum transaction" in {
    val ethTransaction = humanStandardTokenContract.transfer(creator, to, hundredTokens)
    val expectedEthCall =
      EthTransaction(creator, contract, Function("transfer", List(to, hundredTokens)))

    ethTransaction shouldBe expectedEthCall
  }

  it should "create a getTransferEvents Ethereum filter" in {
    val ethFilter = humanStandardTokenContract.getTransferEvents(Block(9999999))
    val transferEventSignature =
      Event("Transfer", List(Address.default, Address.default, Uint(256)))
    val expectedEthFilter =
      EthFilter(Block(9999999), List(contract), List(List(transferEventSignature.toHex)))

    ethFilter shouldBe expectedEthFilter
  }

}
