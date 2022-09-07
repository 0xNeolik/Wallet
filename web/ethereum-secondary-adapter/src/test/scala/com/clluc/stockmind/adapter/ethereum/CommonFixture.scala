package com.clluc.stockmind.adapter.ethereum

import com.clluc.stockmind.core.ethereum.{Block, EthereumHash, LoggedEvent}
import com.clluc.stockmind.core.ethereum.solidity.Address
import io.circe.syntax._

private[ethereum] trait CommonFixture {

  val createdEvent = TransactionEvent(
    "0x7c80fe55c4b039ad1d31b6b31e04db1b5d89dc0a4e89bb999939bd74e8aac934",
    Block(113895),
    "0x00000000000000000000000000000000000000000000000000000000111111110000000000000000000000000000000000000000000000000000000000002710"
  )

  val sentEvent = TransactionEvent(
    "0xf8439e778d94fd46c719b77e9a567f64bfa5b67ffbb456b4e13d66524a0e08ad",
    Block(114565),
    "0x0000000000000000000000000000000000000000000000000000000011111111000000000000000000000000000000000000000000000000000000002222222200000000000000000000000000000000000000000000000000000000333333330000000000000000000000000000000000000000000000000000000000000400"
  )

  val loggedEvent1 = LoggedEvent(
    Address.decode("0x0d40d0f756c26cf54ea91c6f4c98e2a9858deef5"),
    Block(113895),
    List("0x7c80fe55c4b039ad1d31b6b31e04db1b5d89dc0a4e89bb999939bd74e8aac934"),
    "0x00000000000000000000000000000000000000000000000000000000111111110000000000000000000000000000000000000000000000000000000000002710",
    EthereumHash
      .decodePrefixedHexString("0x628e89e3e2bbbc88a34df624dd600f66b121c9c9c5a972d1ef6823da1a2a12c5")
      .get,
    0,
    EthereumHash
      .decodePrefixedHexString("0x628e89e3e2bbbc88a34df624dd600f66b121c9c9c5a972d1ef6823da1a2a12c4")
      .get
  )

  val loggedEvent2 = LoggedEvent(
    Address.decode("0x0d40d0f756c26cf54ea91c6f4c98e2a9858deef5"),
    Block(114565),
    List("0xf8439e778d94fd46c719b77e9a567f64bfa5b67ffbb456b4e13d66524a0e08ad"),
    "0x0000000000000000000000000000000000000000000000000000000011111111000000000000000000000000000000000000000000000000000000002222222200000000000000000000000000000000000000000000000000000000333333330000000000000000000000000000000000000000000000000000000000000400",
    EthereumHash
      .decodePrefixedHexString("0x8cba3bd5974fb91398f3e6bcaa9d5fdfb7d08fdab607518b672ae8de77200b62")
      .get,
    0,
    EthereumHash
      .decodePrefixedHexString("0x8cba3bd5974fb91398f3e6bcaa9d5fdfb7d08fdab607518b672ae8de77200b63")
      .get
  )

  val loggedEventsJson: String =
    s"""{
       |    "jsonrpc": "2.0",
       |    "result": [{
       |        "address": "${loggedEvent1.origin.toHex}",
       |        "blockHash": "0x8a87c1fee407b319f423cced1b93fddafc99a42e4cdae9ef4408c7db0cce7ef2",
       |        "blockNumber": "${loggedEvent1.block.toHex}",
       |        "data": "${loggedEvent1.data}",
       |        "logIndex": "0x0",
       |        "topics": ${loggedEvent1.topics.asJson},
       |        "transactionHash": "${loggedEvent1.txHash.toPrefixedHexString}",
       |        "transactionIndex": "0x${loggedEvent1.txIndex}",
       |        "type": "mined"
       |    }, {
       |        "address": "${loggedEvent2.origin.toHex}",
       |        "blockHash": "0x8a9a0857f0a0cc2a49e7663018a7725e0890724f982eade061911d4ea433711d",
       |        "blockNumber": "${loggedEvent2.block.toHex}",
       |        "data": "${loggedEvent2.data}",
       |        "logIndex": "0x0",
       |        "topics": ${loggedEvent2.topics.asJson},
       |        "transactionHash": "${loggedEvent2.txHash.toPrefixedHexString}",
       |        "transactionIndex": "0x${loggedEvent2.txIndex}",
       |        "type": "mined"
       |    }],
       |    "id": 42
       |}""".stripMargin
}

case class TransactionEvent(topic: String, block: Block, data: String)
