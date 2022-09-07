package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.core.ethereum.Erc20Token
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.user.Balance
import com.clluc.stockmind.port.secondary.Erc20InfoPort
import com.clluc.stockmind.core.ethereum.Ethtoken

import scala.concurrent.{ExecutionContext, Future}
import doobie.imports._

private[postgres] class PostgresErc20InfoAdapter(
    val transactor: Transactor[IOLite]
)(
    override implicit val executionContext: ExecutionContext
) extends Erc20InfoPort
    with Dao {

  override def findBalancesForAddress(ethAddress: Address): Future[List[Balance]] = {
    def query(ethAddress: Address) =
      sql"""SELECT eth_address, token, total_sent, total_received, total_withheld,
    real_balance,effective_balance
    FROM offchain_balances
    WHERE
      eth_address = $ethAddress
  """.query[Balance]

    selectMany(query(ethAddress))
  }

  override def findEthereumTokenBySymbolAndType(
      symbol_erc_type: String): Future[Option[Ethtoken]] = {
    def query(symbol: String, erc_type: String) = sql"""
      SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc_tokens.contract_address,
      erc_tokens.owner, erc_tokens.birth_block
      FROM erc_tokens
      WHERE
      erc_tokens.symbol = $symbol
      AND erc_tokens.erc_type = $erc_type
    """.query[Ethtoken]
    val es                                      = symbol_erc_type.toString().split('|')
    selectOne(query(es(0), es(1)))
  }

  override def findEthereumTokenBySymbolAndTypeAndOwner(
      symbol: String,
      erc_type: String,
      ownerAddress: Address): Future[Option[Ethtoken]] = {
    def query(symbol: String, erc_type: String, ownerAddress: Address) = sql"""
    SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc_tokens.contract_address,
    erc_tokens.owner, erc_tokens.birth_block
    FROM erc_tokens
    WHERE
      erc_tokens.symbol = $symbol
      AND
      erc_tokens.owner = $ownerAddress
      AND
      erc_tokens.erc_type=$erc_type
    """.query[Ethtoken]

    selectOne(query(symbol, erc_type, ownerAddress))
  }

  override def findEthereumTokenByNameAndSymbol(name: String,
                                                erc_type: String): Future[Option[Ethtoken]] = {
    def query(name: String, erc_type: String) = sql"""
      SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc_tokens.contract_address,
        erc_tokens.owner, erc_tokens.birth_block
      FROM erc_tokens
      WHERE
        erc_tokens.token_name = $name
      AND
        erc_tokens.erc_type=$erc_type
    """.query[Ethtoken]

    selectOne(query(name, erc_type))
  }

  override def findBalanceByAddressAndEthereumToken(ethAddress: Address,
                                                    token: String): Future[Option[Balance]] = {
    def query(ethAddress: Address, token: String) = sql"""
      SELECT eth_address, token, total_sent, total_received, total_withheld,
      real_balance,effective_balance
      FROM offchain_balances
      WHERE
        eth_address = ${ethAddress.value}
      AND
        token = $token
    """.query[Balance]

    selectOne(query(ethAddress, token))
  }

  /*override def findEthereumTokenByAddress(contractAddress: Address): Future[Option[Erc20Token]] = {
    def query(contractAddress: Address) = sql"""
      SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc20_tokens.decimals, erc_tokens.contract_address,
      erc_tokens.owner, erc_tokens.birth_block
      FROM erc_tokens LEFT JOIN erc20_tokens
       ON erc_tokens.symbol = erc20_tokens.token_symbol
       AND erc_tokens.erc_type = erc20_tokens.erc_type
       AND erc_tokens.erc_type='ERC-20'
      WHERE
        contract_address = $contractAddress
    """.query[Erc20Token]

    selectOne(query(contractAddress))
  }*/

  override def findEthereumTokenByAddress(contractAddress: Address): Future[Option[Ethtoken]] = {
    def query(contractAddress: Address) = sql"""
      SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc_tokens.contract_address,
      erc_tokens.owner, erc_tokens.birth_block
      FROM erc_tokens
      WHERE
        contract_address = $contractAddress
    """.query[Ethtoken]

    selectOne(query(contractAddress))
  }

  override def findAllErc20Tokens(): Future[List[Ethtoken]] = {
    def query = sql"""
    SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc_tokens.contract_address,
    erc_tokens.owner, erc_tokens.birth_block
    FROM erc_tokens WHERE
    erc_tokens.erc_type='ERC-20'
    """.query[Ethtoken]

    selectMany(query)
  }

  override def findValidTypes(erc_type: String): Future[Option[String]] = {
    def query(erc_type: String) = sql"""
      SELECT erc_type
      FROM erc_types
      WHERE erc_type=$erc_type
    """.query[String]

    selectOne(query(erc_type))
  }

  override def findErc20TokenBySymbolAndType(
      symbol_erc_type: String): Future[Option[Erc20Token]] = {
    def query(symbol: String, erc_type: String) = sql"""
      SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc20_tokens.decimals, erc_tokens.contract_address,
      erc_tokens.owner, erc_tokens.birth_block
      FROM erc_tokens LEFT JOIN erc20_tokens
      ON erc_tokens.symbol = erc20_tokens.token_symbol
      AND erc_tokens.erc_type = erc20_tokens.erc_type
      WHERE
      erc_tokens.symbol = $symbol
      AND erc_tokens.erc_type = $erc_type
    """.query[Erc20Token]
    val es                                      = symbol_erc_type.toString().split('|')
    selectOne(query(es(0), es(1)))
  }

  override def createEthereumToken(token: Erc20Token): Future[Erc20Token] = {
    def queryToken(erc_type: String,
                   symbol: String,
                   name: String,
                   contract: Address,
                   owner: Option[String],
                   birthBlock: Option[Int]) =
      sql"""
      INSERT INTO erc_tokens
      (erc_type, symbol, token_name, contract_address, owner, birth_block)
      VALUES
      ($erc_type, $symbol, $name, $contract, $owner, $birthBlock)""".update.run

    def queryTokenErc20(erc_type: String, symbol: String, decimals: Int) =
      sql"""
      INSERT INTO erc20_tokens
        (erc_type, token_symbol, decimals)
      VALUES
        ($erc_type, $symbol, $decimals)
      """.update.run

    def querySelect(erc_type: String, symbol: String) =
      sql""" SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc20_tokens.decimals, erc_tokens.contract_address,
                              erc_tokens.owner, erc_tokens.birth_block
                               FROM erc_tokens LEFT JOIN erc20_tokens
                               ON erc_tokens.symbol = erc20_tokens.token_symbol
                              AND erc_tokens.erc_type = erc20_tokens.erc_type
                              WHERE
                              erc_tokens.erc_type=$erc_type
                              AND erc_tokens.symbol=$symbol""".query[Erc20Token]

    for {
      _ <- insertWithFeedback(
        queryToken("ERC-20",
                   token.symbol,
                   token.name,
                   token.contract,
                   token.owner,
                   token.birthBlock))

      _ <- insertWithFeedback(queryTokenErc20("ERC-20", token.symbol, token.decimals))

      response <- selectOne(querySelect("ERC-20", token.symbol)).flatMap { token =>
        Future { token.get }
      }
    } yield response

  }

}

object PostgresErc20InfoAdapter {

  def apply(tx: Transactor[IOLite])(implicit executionContext: ExecutionContext): Erc20InfoPort =
    new PostgresErc20InfoAdapter(tx)
}
