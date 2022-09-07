package com.clluc.stockmind.core.transaction

import java.util.UUID

import com.clluc.stockmind.core.auth.LoginInfo
import org.joda.time.DateTime

case class PendingTransfer(
    id: Long = 0L,
    fromUser: UUID,
    toFutureUser: LoginInfo,
    tokenSymbol: String,
    erc_type: String,
    amount: BigInt,
    created: DateTime,
    processed: Option[DateTime],
    token_id: Option[BigInt] = None
)
