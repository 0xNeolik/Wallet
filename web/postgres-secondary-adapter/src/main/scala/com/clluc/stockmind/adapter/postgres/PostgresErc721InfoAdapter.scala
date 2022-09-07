package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.clluc.stockmind.port.secondary.Erc721InfoPort
import com.clluc.stockmind.core.ethereum.Ethtoken
import com.clluc.stockmind.core.ethereum.Erc721Token

import scala.concurrent.{ExecutionContext, Future}
import doobie.imports._

private[postgres] class PostgresErc721InfoAdapter(
    val transactor: Transactor[IOLite]
)(
    override implicit val executionContext: ExecutionContext
) extends Erc721InfoPort
    with Dao {

  override def findAllErc721Tokens(): Future[List[Ethtoken]] = {
    def query = sql"""
    SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc_tokens.contract_address,
    erc_tokens.owner, erc_tokens.birth_block
    FROM erc_tokens WHERE
    erc_tokens.erc_type='ERC-721'
    """.query[Ethtoken]

    selectMany(query)
  }

  override def create721CollectionToken(token: Ethtoken): Future[Ethtoken] = {
    def queryToken(erc_type: String,
                   symbol: String,
                   name: String,
                   contract: Address,
                   owner: Option[String],
                   birthBlock: Option[Int]): ConnectionIO[Ethtoken] =
      sql"""
      INSERT INTO erc_tokens
      (erc_type, symbol, token_name, contract_address, owner, birth_block)
      VALUES
      ($erc_type, $symbol, $name, $contract, $owner, $birthBlock)""".update.withUniqueGeneratedKeys(
        "erc_type",
        "symbol",
        "token_name",
        "contract_address",
        "owner",
        "birth_block"
      )

    insertWithFeedback(
      queryToken("ERC-721",
                 token.symbol,
                 token.name,
                 token.contract,
                 token.owner,
                 token.birthBlock))

  }

  override def create721Token(token: Ethtoken,
                              metadata: String,
                              NFTid: Uint): Future[Erc721Token] = {
    def queryToken(erc_type: String, symbol: String, metadata: String, id: BigInt) =
      sql"""
      INSERT INTO erc721_tokens
      (erc_type, token_symbol, meta, id)
      VALUES
      ($erc_type, $symbol, $metadata, $id)""".update.run

    def querySelect(erc_type: String, symbol: String, id: BigInt) =
      sql""" SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc721_tokens.meta,erc721_tokens.id, erc_tokens.contract_address,
                              erc_tokens.owner, erc_tokens.birth_block
                               FROM erc_tokens LEFT JOIN erc721_tokens
                               ON erc_tokens.symbol = erc721_tokens.token_symbol
                              AND erc_tokens.erc_type = erc721_tokens.erc_type
                              WHERE
                              erc_tokens.erc_type=$erc_type
                              AND erc_tokens.symbol=$symbol
                              AND erc721_tokens.id=$id""".query[Erc721Token]

    for {
      _ <- insertWithFeedback(queryToken("ERC-721", token.symbol, metadata, NFTid.value))
      response <- selectOne(querySelect("ERC-721", token.symbol, NFTid.value)).flatMap { token =>
        Future { token.get }
      }
    } yield response
  }

  override def findEthereumTokenByAddress(contractAddress: Address): Future[Option[Erc721Token]] = {
    def query(contractAddress: Address) = sql"""
      SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc721_tokens.meta, erc721_tokens.id, erc_tokens.contract_address,
      erc_tokens.owner, erc_tokens.birth_block
      FROM erc_tokens LEFT JOIN erc721_tokens
       ON erc_tokens.symbol = erc721_tokens.token_symbol
       AND erc_tokens.erc_type = erc721_tokens.erc_type
      WHERE
        contract_address = $contractAddress
      AND erc_tokens.erc_type='ERC-721'
    """.query[Erc721Token]

    selectOne(query(contractAddress))
  }

  override def findEthTokenByAddress(contractAddress: Address): Future[Option[Ethtoken]] = {
    def query(contractAddress: Address) = sql"""
      SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc_tokens.contract_address,
      erc_tokens.owner, erc_tokens.birth_block
      FROM erc_tokens
      WHERE
        contract_address = $contractAddress
      AND erc_tokens.erc_type='ERC-721'
    """.query[Ethtoken]

    selectOne(query(contractAddress))
  }

  override def findEthereum721TokenByUniqueId(
      id: BigInt,
      contractAddress: Address): Future[Option[Ethtoken]] = {
    def query(id: BigInt, contractAddress: Address) = sql"""
      SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc_tokens.contract_address,
      erc_tokens.owner, erc_tokens.birth_block
      FROM erc_tokens
      LEFT JOIN erc721_tokens
      ON erc_tokens.symbol = erc721_tokens.token_symbol
      AND erc_tokens.erc_type = erc721_tokens.erc_type
      LEFT JOIN offchain_721balances bal ON
      bal.tokenid = erc721_tokens.id
      WHERE
      bal.eth_address = $contractAddress
       AND
      erc721_tokens.id=$id
      LIMIT 1
    """.query[Ethtoken]

    selectOne(query(id, contractAddress))
  }

  override def findErc721tokenByIdAndOwner(id: BigInt,
                                           tokenOwner: Address): Future[Option[Erc721Token]] = {
    def query(id: BigInt, tokenOwner: Address) = sql"""
      SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc721_tokens.meta, erc721_tokens.id, erc_tokens.contract_address,
      erc_tokens.owner, erc_tokens.birth_block
      FROM erc_tokens
      LEFT JOIN erc721_tokens
      ON erc_tokens.symbol = erc721_tokens.token_symbol
      AND erc_tokens.erc_type = erc721_tokens.erc_type
      LEFT JOIN offchain_721balances bal ON
      bal.tokenid = erc721_tokens.id
      WHERE
      bal.eth_address = $tokenOwner
       AND
      erc721_tokens.id=$id
      LIMIT 1
    """.query[Erc721Token]

    selectOne(query(id, tokenOwner))
  }

  override def findErc721tokenByTokenId(id: BigInt): Future[Option[Erc721Token]] = {
    def query(id: BigInt) = sql"""
      SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc721_tokens.meta, erc721_tokens.id, erc_tokens.contract_address,
      erc_tokens.owner, erc_tokens.birth_block
      FROM erc_tokens
      LEFT JOIN erc721_tokens
      ON erc_tokens.symbol = erc721_tokens.token_symbol
      AND erc_tokens.erc_type = erc721_tokens.erc_type
      WHERE
      erc721_tokens.id=$id
    """.query[Erc721Token]

    selectOne(query(id))
  }

  override def deleteTokenFromId(id: Uint): Future[Unit] = {
    def query(id: BigInt) = sql"""
      DELETE FROM erc721_tokens WHERE id =$id
    """.update
    update(query(id.value))
  }

  override def findERC721tokensForAddress(address: Address): Future[List[Erc721Token]] = {
    def query(address: Address) = sql"""
    SELECT erc_tokens.symbol,erc_tokens.erc_type, erc_tokens.token_name, erc721_tokens.meta, erc721_tokens.id, erc_tokens.contract_address,
       erc_tokens.owner, erc_tokens.birth_block
    FROM erc_tokens
    LEFT JOIN erc721_tokens
    ON erc_tokens.symbol = erc721_tokens.token_symbol
    AND erc_tokens.erc_type = erc721_tokens.erc_type
    LEFT JOIN offchain_transfers
    ON erc_tokens.symbol = offchain_transfers.token_symbol
    AND erc_tokens.erc_type = offchain_transfers.erc_type
    WHERE
     offchain_transfers.to_addr = ${address.value}
    AND erc_tokens.erc_type='ERC-721'
    GROUP BY erc_tokens.symbol,erc_tokens.erc_type,erc_tokens.token_name, erc721_tokens.meta,erc721_tokens.id,erc_tokens.contract_address,
     erc_tokens.owner,erc_tokens.birth_block
    """.query[Erc721Token]

    selectMany(query(address))
  }

}

object PostgresErc721InfoAdapter {

  def apply(tx: Transactor[IOLite])(implicit executionContext: ExecutionContext): Erc721InfoPort =
    new PostgresErc721InfoAdapter(tx)
}
