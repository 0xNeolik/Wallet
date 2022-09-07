package com.clluc.stockmind.core.ethereum

// TODO Remove this ADT; meant to be not used anymore
sealed trait CreateOrRetrieveEthAccountOutcome

case class AccountCreated(callResponse: Option[String] = None)
    extends CreateOrRetrieveEthAccountOutcome

case class AccountRetrieved(callResponse: Option[String] = None)
    extends CreateOrRetrieveEthAccountOutcome

object CreateOrRetrieveEthAccountOutcome {
  case class SuccessfulEthereumAccountAccomplished(
      account: EthereumAccount,
      createOrRetrieveOutcome: CreateOrRetrieveEthAccountOutcome)
  case class UnsuccessfulEthereumAccountAccomplished(callResponse: String, callStatusCode: Int)
  case class EmptyEthereumAccountAccomplished() // Only possible if after creating a new ethereum account we get an HTTP 200 code

  type CreateOrRetrieveAccountResult =
    Either[UnsuccessfulEthereumAccountAccomplished, SuccessfulEthereumAccountAccomplished]
}
