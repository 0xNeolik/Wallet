package com.clluc.stockmind.core

package object ethereum {

  // Event signatures are created by hashing the event name and the parameter signatures.
  // In this case, it is keccak("Transfer(address,address,uint256)")
  //
  // Our Event case class can encode signatures on demand, like shown below:
  // val transferEventSignature = Event("Transfer", List(Address.apply, Address.apply, Uint(256))).encode
  //
  // In this case though it is better to just precalculate the signature

  val erc20TransferEventSignature =
    "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"

  val erc20MintEventSignature =
    "0x0f6798a560793a54c3bcfe86a93cde1e73087d944c0ea20544137d4121396885"

  val erc20BurnEventSignature =
    "0xcc16f5dbb4873280815c1ee09dbd06736cffcc184412cf7a71a0fdb75d397ca5"

}
