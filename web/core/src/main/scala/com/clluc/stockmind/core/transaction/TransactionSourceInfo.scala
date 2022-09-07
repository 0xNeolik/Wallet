package com.clluc.stockmind.core.transaction

import com.clluc.stockmind.core.ethereum.{EthereumAccount, Ethtoken}
import com.clluc.stockmind.core.user.Balance

/**
  * Represents the elements required from a tokens transfer issuer.
  *
  * @param token
  * @param account
  * @param balance
  */
private[core] case class TransactionSourceInfo(
    token: Ethtoken,
    account: EthereumAccount,
    balance: Balance,
    amount: BigInt
)
private[core] case class TransactionSourceInfo721(
    token: Ethtoken,
    account: EthereumAccount,
    tokenId: BigInt
)
