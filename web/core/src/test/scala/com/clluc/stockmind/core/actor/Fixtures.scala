package com.clluc.stockmind.core.actor

import java.util.UUID

import com.clluc.stockmind.core.Generators._
import com.clluc.stockmind.core.RawValueParser
import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.clluc.stockmind.core.transaction.OffChainTransfer
import org.joda.time.DateTime

import scala.util.Random

/**
  * Data that simulates a coherent state for the entire application.
  * It is meant to be reused by unit tests.
  */
private[actor] object Fixtures {

  val sourceUserId: UUID = UUID.randomUUID()

  def genAddressSample: Address = Address(genAddress.sample.get)

  val masterAccountAddress: Address      = genAddressSample
  val supplierAccountAddress: Address    = genAddressSample
  val destinationAccountAddress: Address = genAddressSample
  val sourceAccountAddress: Address      = genAddressSample

  val destinationEthAccountPassword: String = Random.nextString(10)

  val destinationEthAccount: EthereumAccount = {

    EthereumAccount(
      sourceUserId,
      destinationAccountAddress,
      destinationEthAccountPassword
    )
  }

  val tokenContractAdd: Address = genAddressSample

  val tokenDecimals = 1
  val tokenId: Uint = Uint(value = 1)

  val token: Erc20Token = Erc20Token(
    symbol = "SLD",
    erc_type = "ERC-20",
    name = "Solid",
    decimals = tokenDecimals,
    contract = tokenContractAdd,
    owner = None,
    birthBlock = None
  )

  val ethtoken: Ethtoken = Ethtoken(
    symbol = "SLD",
    erc_type = "ERC-20",
    name = "Solid",
    contract = tokenContractAdd,
    owner = None,
    birthBlock = None
  )

  val ethtoken721: Ethtoken = Ethtoken(
    symbol = "SLD",
    erc_type = "ERC-721",
    name = "Solid",
    contract = tokenContractAdd,
    owner = None,
    birthBlock = None
  )

  val etherAsToken: Erc20Token = Erc20Token(
    symbol = "ETH",
    erc_type = "ERC-20",
    name = "Ethereum",
    decimals = 18,
    contract = Address.default,
    owner = None,
    birthBlock = None
  )

  def fromAmountToWithdrawalValue(amount: String): BigInt =
    RawValueParser.parseIntoRawValue(amount, token.decimals).get

  def expectedSignableTx(te: TransferEvent): SignableTransaction = {
    val contract = HumanStandardTokenContract(ethtoken)
    SignableTransaction(
      contract.transfer(destinationAccountAddress, masterAccountAddress, te.value),
      destinationEthAccount.password
    )
  }

  def createOffChainTransfer(amount: Uint, created: DateTime) = OffChainTransfer(
    0L,
    token.symbol,
    token.erc_type,
    sourceAccountAddress,
    destinationAccountAddress,
    amount,
    created,
    None
  )

  val sampleTransferEvent = TransferEvent(
    42L,
    "abc",
    "ERC-20",
    Address.default,
    Address.default,
    Uint(8, 1),
    Block(1),
    EthereumHash(genEthHash.sample.get),
    1,
    None
  )

  def sampleLoggedEvent() = {
    val topics =
      List(erc20TransferEventSignature, sourceAccountAddress.value, destinationAccountAddress.value)
    genLoggedEvent.sample.get.copy(topics = topics, origin = tokenContractAdd)
  }
}
