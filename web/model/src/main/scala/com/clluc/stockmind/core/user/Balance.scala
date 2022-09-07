package com.clluc.stockmind.core.user

import com.clluc.stockmind.core.ethereum.{Erc20Token, Erc721Token}
import com.clluc.stockmind.core.ethereum.solidity.Address

case class Balance(
    ethAddress: Address,
    token: Erc20Token,
    totalSent: BigInt,
    totalReceived: BigInt,
    totalWithheld: BigInt,
    realBalance: BigInt,
    effectiveBalance: BigInt
)

case class Balance721(
    ethAddress: Address,
    token: Erc721Token,
    totalSent: BigInt,
    totalReceived: BigInt,
    totalWithheld: BigInt,
    realBalance: BigInt,
    effectiveBalance: BigInt
)
