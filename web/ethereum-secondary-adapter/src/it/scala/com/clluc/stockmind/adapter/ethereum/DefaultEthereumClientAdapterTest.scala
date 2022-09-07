package com.clluc.stockmind.adapter.ethereum

import com.clluc.stockmind.core.ethereum
import com.clluc.stockmind.core.ethereum.{Block, Erc20Token, HumanStandardTokenContract, SignableTransaction}
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.typesafe.config.ConfigFactory
import org.scalatest.{AsyncFlatSpec, Matchers}

class DefaultEthereumClientAdapterTest extends AsyncFlatSpec with Matchers {

  /*
  Test for ERC20 Token HumanStandardToken.sol. Contract code here:
  https://github.com/ConsenSys/Tokens/tree/4788a8600a0d27586a56e158db98c58164ce2ca1/Token_Contracts/contracts
   */

  val conf = ConfigFactory.load

  private val testContractAdd = conf.getString("eth.test.contract")

  // TODO This solves an error running tests, but I don't know if it's wrong (likely)
  // Please review and either fix or give some tips to allow me to fix it
  val contract = Address.decode(
    Option(testContractAdd).getOrElse("0x731a10897d267e19b34503ad902d0a29173ba4b1")
  )

  val totalAmountTokens = Uint(256, 10000)
  val token = Erc20Token("BLV", "Believe", 0, contract, None, None)
  val humanStandardTokenContract = HumanStandardTokenContract(
    token
  )

  val creator = Address.decode(conf.getString("eth.test.creator"))
  val password = ConfigFactory.load().getString("eth.test.password")
  val to = Address("e9ea50e96fb81432edd6605109257ac71930c574")
  val hundredTokens = Uint(256, 100)
  val erc20Transfer = ethereum.erc20TransferEventSignature

  val url = ConfigFactory.load().getString("eth.node")
  val ethClient = new DefaultEthereumClientAdapter(url)

  behavior of "DefaultEthereumClientAdapter"

  it should "get the last block number" in {
    ethClient.findEthereumBlockNumber().map { response =>
      Block.fromHexString(response.right.get)
    }.map(_ => succeed)
  }

  "Creator of the contract" should "have all tokens" in {
    for {
      balanceR <- ethClient.callMethod(humanStandardTokenContract.balanceOf(creator))
      balance = Uint.decode(256, balanceR.right.get.replace("0x", ""))
    } yield balance shouldBe totalAmountTokens
  }

  behavior of "HumanStandardTokenContract - integration tests"

  it should "get the total supply tokens" in {
    for {
      supplyR <- ethClient.callMethod(humanStandardTokenContract.totalSupply)
      supply = Uint.decode(256, supplyR.right.get.replace("0x", ""))
    } yield supply shouldBe totalAmountTokens
  }

  it should "be able to transfer token to another account" in {
    for {
      _ <- ethClient.sendTransaction(
        SignableTransaction(
          humanStandardTokenContract.transfer(creator, to, hundredTokens), password
        )
      )
      creatorBalanceR <- ethClient.callMethod(humanStandardTokenContract.balanceOf(creator))
      creatorBalance = Uint.decode(256, creatorBalanceR.right.get.replace("0x", ""))
      toBalanceR <- ethClient.callMethod(humanStandardTokenContract.balanceOf(to))
      toBalance = Uint.decode(256, toBalanceR.right.get.replace("0x", ""))
      _ <- ethClient.sendTransaction(
        SignableTransaction(
          humanStandardTokenContract.transfer(creator, to, hundredTokens), password
        )
      )
    } yield {
      creatorBalance shouldBe Uint(256, 9900)
      toBalance shouldBe Uint(256, 100)
    }
  }

  it should "get all logs about transfer from specific block" in {
    for {
      txHashR <- ethClient.sendTransaction(
        SignableTransaction(
          humanStandardTokenContract.transfer(creator, to, hundredTokens), password
        )
      )
      txHash = txHashR.right.get
      logsR <- ethClient.getLoggedEvents(humanStandardTokenContract.getTransferEvents(Block(0)))
      txHashes = logsR.right.get.map(_.txHash.toPrefixedHexString)
    } yield txHashes should contain(txHash)
  }

  it should "not get any logs about transfer when there are none" in {
    val notMinedYetBlock = Block(99)
    for {
      eventsR <- ethClient.getLoggedEvents(humanStandardTokenContract.getTransferEvents(notMinedYetBlock))
      events = eventsR.right.get
    } yield events shouldBe empty
  }

}
