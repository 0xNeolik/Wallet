package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.core.ethereum.{Erc20Token, Erc721Token}
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.user.{Balance, Balance721}
import com.clluc.stockmind.port.secondary.OffchainBalancePort
import doobie.imports._

import scala.concurrent.{ExecutionContext, Future}

class PostgresOffchainBalanceAdapter(
    val transactor: Transactor[IOLite]
)(implicit val executionContext: ExecutionContext)
    extends OffchainBalancePort
    with Dao {

  private case class BalanceDb(
      ethAddress: Address,
      erc_type: String,
      token: String,
      totalSent: BigInt,
      totalReceived: BigInt,
      totalWithheld: BigInt,
      realBalance: BigInt,
      effectiveBalance: BigInt,
      symbol: String,
      tokenName: String,
      decimals: Int,
      contractAddress: String
  )

  private case class BalanceDb721(
      ethAddress: Address,
      erc_type: String,
      token: String,
      totalSent: BigInt,
      totalReceived: BigInt,
      totalWithheld: BigInt,
      realBalance: BigInt,
      effectiveBalance: BigInt,
      symbol: String,
      tokenName: String,
      meta: String,
      id: BigInt,
      contractAddress: String
  )

  private def fromBalanceDbToBalance(db: BalanceDb): Balance =
    Balance(
      db.ethAddress,
      Erc20Token(
        db.token,
        db.erc_type,
        db.tokenName,
        db.decimals,
        Address(db.contractAddress),
        None,
        None
      ),
      db.totalSent,
      db.totalReceived,
      db.totalWithheld,
      db.realBalance,
      db.effectiveBalance
    )

  private def fromBalanceDbToBalance721(db: BalanceDb721): Balance721 =
    Balance721(
      db.ethAddress,
      Erc721Token(
        db.token,
        db.erc_type,
        db.tokenName,
        db.meta,
        db.id,
        Address(db.contractAddress),
        None,
        None
      ),
      db.totalSent,
      db.totalReceived,
      db.totalWithheld,
      db.realBalance,
      db.effectiveBalance
    )

  override def findBalancesForAddress(ethAddress: Address): Future[List[Balance]] = {
    def query(ethAddress: Address) = sql"""SELECT bal.eth_address, bal.erc_type,bal.token,
                                          bal.total_sent,bal.total_received,bal.total_withheld,bal.real_balance,
                                          bal.effective_balance,tokens20.token_symbol,tokens.token_name,
                                          tokens20.decimals,tokens.contract_address
    FROM offchain_balances bal, erc_tokens tokens, erc20_tokens tokens20
    WHERE
      bal.eth_address = $ethAddress
    AND
      bal.token = tokens.symbol
    AND
     bal.erc_type = tokens.erc_type
     AND tokens20.erc_type=tokens.erc_type AND tokens20.token_symbol=tokens.symbol
  """.query[BalanceDb]

    val queryResult: Future[List[BalanceDb]] = selectMany(query(ethAddress))

    queryResult.map(_.map(fromBalanceDbToBalance))
  }

  override def findBalanceByAddressAndEthereumToken(
      ethAddress: Address,
      tokenSymbol: String): Future[Option[Balance]] = {
    def query(ethAddress: Address, token: String) = sql"""
       SELECT bal.eth_address, bal.erc_type,bal.token,
                      bal.total_sent,bal.total_received,bal.total_withheld,bal.real_balance,
               bal.effective_balance,tokens20.token_symbol,tokens.token_name,
            tokens20.decimals,tokens.contract_address
      FROM offchain_balances bal, erc20_tokens tokens20, erc_tokens tokens
      WHERE
        eth_address = ${ethAddress.value}
      AND
        token = $token
      AND
        bal.token = tokens.symbol
      AND
        bal.erc_type = tokens.erc_type AND tokens20.token_symbol=tokens.symbol
    """.query[BalanceDb]

    val queryResult: Future[Option[BalanceDb]] = selectOne(query(ethAddress, tokenSymbol))

    queryResult.map(_.map(fromBalanceDbToBalance))
  }

  override def findBalance721ByAddressAndEthereumTokenId(
      ethAddress: Address,
      tokenid: BigInt): Future[Option[Balance721]] = {
    def query(ethAddress: Address, tokenid: BigInt) = sql"""
      SELECT bal.eth_address, bal.erc_type, bal.token, bal.total_sent, bal.total_received, bal.total_withheld,
      bal.real_balance, bal.effective_balance, tokens.token_symbol, g.token_name, tokens.meta, tokens.id, g.contract_address
      FROM offchain_721balances bal, erc721_tokens tokens, erc_tokens g
      WHERE
        eth_address = ${ethAddress.value}
      AND
        tokens.id = $tokenid
      AND
        bal.tokenid = tokens.id
      AND
        bal.erc_type = tokens.erc_type
      AND g.erc_type = tokens.erc_type AND g.symbol = tokens.token_symbol
    """.query[BalanceDb721]

    val queryResult: Future[Option[BalanceDb721]] = selectOne(query(ethAddress, tokenid))

    queryResult.map(_.map(fromBalanceDbToBalance721))
  }

  override def findBalances721tokensForAddress(ethAddress: Address): Future[List[Erc721Token]] = {
    def query(ethAddress: Address) = sql"""
      SELECT g.symbol, g.erc_type, g.token_name, tokens.meta, tokens.id, g.contract_address,
       g.owner, g.birth_block
      FROM offchain_721balances bal, erc721_tokens tokens, erc_tokens g
      WHERE
        eth_address = ${ethAddress.value}
      AND
        bal.tokenid = tokens.id
      AND
        bal.erc_type = tokens.erc_type
      AND g.erc_type = tokens.erc_type AND g.symbol = tokens.token_symbol
      AND bal.real_balance = 1
    """.query[Erc721Token]

    selectMany(query(ethAddress))

  }

}
