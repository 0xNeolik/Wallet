package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.user.{Balance, Balance721}
import com.clluc.stockmind.core.ethereum.Erc721Token
import scala.concurrent.Future

trait OffchainBalancePort {
  def findBalancesForAddress(ethAddress: Address): Future[List[Balance]]

  def findBalanceByAddressAndEthereumToken(ethAddress: Address,
                                           tokenSymbol: String): Future[Option[Balance]]

  def findBalance721ByAddressAndEthereumTokenId(ethAddress: Address,
                                                tokenid: BigInt): Future[Option[Balance721]]

  def findBalances721tokensForAddress(ethAddress: Address): Future[List[Erc721Token]]

}
