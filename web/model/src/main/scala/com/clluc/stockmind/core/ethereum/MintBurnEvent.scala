package com.clluc.stockmind.core.ethereum

import com.clluc.stockmind.core.ethereum
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import org.joda.time.DateTime

import scala.util.Try

case class MintBurnEvent(
    id: Long,
    tokenSymbol: String,
    erc_type: String,
    owner: Address,
    value: Uint,
    block: Block,
    txHash: EthereumHash,
    txIndex: Int,
    processedDate: Option[DateTime]
)

object MintBurnEvent {

  // TODO Give more info on the return type about the underlying error causing the return of a None value
  // TODO Research how to decouple it
  def fromLoggedEvent(event: LoggedEvent,
                      token: Ethtoken,
                      timestamp: DateTime): Option[MintBurnEvent] =
    if ((event.topics.head == ethereum.erc20MintEventSignature || event.topics.head == ethereum.erc20BurnEventSignature) && event.origin == token.contract) {
      Try {
        MintBurnEvent(
          0L,
          token.symbol,
          "ERC-20",
          Address.decode(event.topics(1)),
          Uint.decode(256, event.data.drop(2)),
          event.block,
          event.txHash,
          event.txIndex,
          Some(timestamp)
        )
      }.toOption
    } else if (event.topics.head == TokenFactoryContract.burnNFTEvent && event.origin == token.contract) {
      Try {
        MintBurnEvent(
          0L,
          token.symbol,
          "ERC-721",
          Address.decode(event.topics(1)),
          Uint(256, 1),
          event.block,
          event.txHash,
          event.txIndex,
          Some(timestamp)
        )
      }.toOption

    } else {
      None
    }
}
