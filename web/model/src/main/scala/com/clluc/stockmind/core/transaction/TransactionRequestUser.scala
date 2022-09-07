package com.clluc.stockmind.core.transaction

import java.util.UUID

import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.user.EmailHandle

case class TransactionRequestUser(
    sourceUserId: UUID,
    masterAccountAddress: Address,
    destination: Either[Address, EmailHandle],
    tokenSymbol: String,
    erc_type: String,
    amount: String,
    metaInf: Option[Map[String, String]] = None
)

case class TransactionRequestUser721(
    sourceUserId: UUID,
    masterAccountAddress: Address,
    destination: Either[Address, EmailHandle],
    id: BigInt,
    metaInf: Option[Map[String, String]] = None
)
