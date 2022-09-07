package com.clluc.stockmind.core.signup

import java.util.UUID

import com.clluc.stockmind.core.ethereum.CreateOrRetrieveEthAccountOutcome.{
  SuccessfulEthereumAccountAccomplished,
  UnsuccessfulEthereumAccountAccomplished
}
import com.clluc.stockmind.core.ethereum.{AccountCreated, AccountRetrieved, EthereumAccount}
import com.clluc.stockmind.port.primary.EthereumAccountOperationsPort
import com.clluc.stockmind.port.secondary.{EthereumAccountPort, EthereumClientPort}

import scala.concurrent.{ExecutionContext, Future}

private[core] class EthereumAccountOperationsAdapter(
    ethereumAccountPort: EthereumAccountPort,
    ethereumClientPort: EthereumClientPort
)(implicit ec: ExecutionContext)
    extends EthereumAccountOperationsPort {

  override def createOrRetrieveAccountFor(
      userId: UUID
  ): Future[
    Either[UnsuccessfulEthereumAccountAccomplished, SuccessfulEthereumAccountAccomplished]] = {
    ethereumAccountPort.findAccountByUserId(userId).flatMap {
      case Some(account) =>
        Future.successful(
          Right(SuccessfulEthereumAccountAccomplished(account, AccountRetrieved()))
        )

      case None =>
        val eventualPassword = ethereumClientPort.createAccountGeneratingPassword()
        eventualPassword.flatMap {
          case Right(justCreatedEthAccount) =>
            ethereumAccountPort
              .saveAccount(
                EthereumAccount(
                  userId,
                  justCreatedEthAccount.address,
                  justCreatedEthAccount.password
                )
              )
              .map { ethAcc =>
                Right(SuccessfulEthereumAccountAccomplished(ethAcc, AccountCreated()))
              }

          case Left(unexpectedResponse) =>
            Future.successful {
              Left(
                UnsuccessfulEthereumAccountAccomplished(unexpectedResponse.networkResponseBody,
                                                        unexpectedResponse.statusCode)
              )
            }
        }
    }
  }
}

object EthereumAccountOperationsAdapter {

  def apply(
      ethereumAccountPort: EthereumAccountPort,
      ethereumClientPort: EthereumClientPort
  )(implicit ec: ExecutionContext): EthereumAccountOperationsAdapter =
    new EthereumAccountOperationsAdapter(ethereumAccountPort, ethereumClientPort)
}
