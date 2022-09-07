package com.clluc.stockmind.core.ethereum

import com.clluc.stockmind.core.ethereum.solidity.Address

case class NewEthereumAccount(address: Address, password: String)

object JsonRpcResponse {
  case class UnexpectedEthereumResponse(networkResponseBody: String, statusCode: Int)

  type Password = String

  type EthereumResponse[T] = Either[UnexpectedEthereumResponse, T]

  type JsonRpcAddress = EthereumResponse[Address]

  type JsonRpcLoggedEvents = EthereumResponse[List[LoggedEvent]]

  type JsonRpcBalance = EthereumResponse[BigInt]

  type JsonRpcNewEthereumAccount = EthereumResponse[NewEthereumAccount]

  type JsonRpcPlainResult = EthereumResponse[String]
}
