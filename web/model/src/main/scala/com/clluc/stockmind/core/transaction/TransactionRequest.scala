package com.clluc.stockmind.core.transaction

import java.util.UUID

import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.twitter.TwitterHandle

case class TransactionRequest(
    sourceUserId: UUID,
    masterAccountAddress: Address,
    destination: Either[Address, TwitterHandle],
    tokenSymbol: String,
    erc_type: String,
    amount: String,
    metaInf: Option[Map[String, String]] = None
)

case class TransactionRequest721(
    sourceUserId: UUID,
    masterAccountAddress: Address,
    destination: Either[Address, TwitterHandle],
    id: BigInt,
    metaInf: Option[Map[String, String]] = None
)
