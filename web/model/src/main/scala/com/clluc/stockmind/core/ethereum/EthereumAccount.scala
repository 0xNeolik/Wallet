package com.clluc.stockmind.core.ethereum

import java.util.UUID

import com.clluc.stockmind.core.ethereum.solidity.Address

case class EthereumAccount(user: UUID, address: Address, password: String)
