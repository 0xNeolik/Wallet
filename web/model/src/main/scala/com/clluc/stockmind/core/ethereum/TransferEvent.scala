package com.clluc.stockmind.core.ethereum

import com.clluc.stockmind.core.ethereum
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import org.joda.time.DateTime

import scala.util.Try

case class TransferEvent(
    id: Long,
    tokenSymbol: String,
    erc_type: String,
    from: Address,
    to: Address,
    value: Uint,
    block: Block,
    txHash: EthereumHash,
    txIndex: Int,
    processedDate: Option[DateTime],
    token_id: Option[BigInt] = None
)

object TransferEvent {

  // TODO Give more info on the return type about the underlying error causing the return of a None value
  // TODO Research how to decouple it
  def fromLoggedEvent(event: LoggedEvent,
                      token: Ethtoken,
                      timestamp: DateTime): Option[TransferEvent] =
    if (event.topics.head == ethereum.erc20TransferEventSignature && event.origin == token.contract) {
      Try {
        TransferEvent(
          0L,
          token.symbol,
          "ERC-20",
          Address.decode(event.topics(1)),
          Address.decode(event.topics(2)),
          Uint.decode(256, event.data.drop(2)),
          event.block,
          event.txHash,
          event.txIndex,
          Some(timestamp)
        )
      }.toOption
    } else if (event.topics.head == TokenFactoryContract.transferNFTEvent && event.origin == token.contract) {
      Try {
        TransferEvent(
          0L,
          token.symbol,
          "ERC-721",
          Address.decode(event.topics(1)),
          Address.decode(event.topics(2)),
          Uint(256, 1),
          event.block,
          event.txHash,
          event.txIndex,
          Some(timestamp),
          Some(Uint.decode(256, event.topics(3).drop(2)).value)
        )
      }.toOption

    } else {
      None
    }
}
