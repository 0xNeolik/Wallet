package com.clluc.stockmind.adapter.ethereum

import com.clluc.stockmind.core.ethereum.{Block, EthereumHash, LoggedEvent}
import com.clluc.stockmind.core.ethereum.solidity.Address

private[ethereum] object Utils {

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
      .decodePrefixedHexString("0x628e89e3e2bbbc88a34df624dd600f66b121c9c9c5a972d1ef6823da1a2a12c3")
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
      .decodePrefixedHexString("0x8cba3bd5974fb91398f3e6bcaa9d5fdfb7d08fdab607518b672ae8de77200b65")
      .get
  )

  val loggedEvents: List[LoggedEvent] = List(
    LoggedEvent(
      loggedEvent1.origin,
      loggedEvent1.block,
      loggedEvent1.topics,
      loggedEvent1.data,
      loggedEvent1.txHash,
      loggedEvent1.txIndex,
      loggedEvent1.blockHash
    ),
    LoggedEvent(
      loggedEvent2.origin,
      loggedEvent2.block,
      loggedEvent2.topics,
      loggedEvent2.data,
      loggedEvent2.txHash,
      loggedEvent2.txIndex,
      loggedEvent2.blockHash
    )
  )
}
