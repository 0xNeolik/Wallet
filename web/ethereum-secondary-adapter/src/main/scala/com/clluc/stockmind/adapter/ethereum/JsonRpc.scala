package com.clluc.stockmind.adapter.ethereum

import com.clluc.stockmind.core.ethereum.{EthCall, EthCallFrom, EthFilter, SignableTransaction}
import shapeless._

private[ethereum] case class JsonRpc[T <: HList](jsonrpc: String,
                                                 method: String,
                                                 params: T,
                                                 id: String)

private[ethereum] object JsonRpc {

  private def apply[T <: HList](method: String, params: T): JsonRpc[T] =
    JsonRpc("2.0", method, params, "42")

  def newAccount(password: String) =
    JsonRpc("personal_newAccount", password :: HNil)

  def blockNumber() = {
    // The empty HList will be encoded as an empty object `{}`. Fortunately
    // the Ethereum API is lenient. Further info here:
    // https://stackoverflow.com/questions/41617780/how-to-encode-json-when-hlist-parameter-is-hnil/41618427
    JsonRpc("eth_blockNumber", HNil: HNil) // HNil type annotation is required to make HNil encodable as json.
  }

  def signMessage(address: String, message: String) =
    JsonRpc("eth_sign", address :: message :: HNil)

  def sendTransaction(signableTx: SignableTransaction) =
    JsonRpc("personal_sendTransaction", signableTx.tx :: signableTx.password :: HNil)

  def signAndSendTransaction(signableTx: SignableTransaction) =
    JsonRpc("personal_signAndSendTransaction", signableTx.tx :: signableTx.password :: HNil)

  def getLogs(filter: EthFilter) =
    JsonRpc("eth_getLogs", filter :: HNil)

  def callMethod(call: EthCall) =
    JsonRpc("eth_call", call :: "latest" :: HNil)

  def callMethodFrom(call: EthCallFrom) =
    JsonRpc("eth_call", call :: "latest" :: HNil)

  def getBalance(address: String) =
    JsonRpc("eth_getBalance", address :: "latest" :: HNil)

  def getBlockByNumber(hexNumber: String) =
    JsonRpc("eth_getBlockByNumber", hexNumber :: true :: HNil)
}
