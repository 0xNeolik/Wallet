package com.clluc.stockmind.core.user
import com.clluc.stockmind.core.ethereum.Erc721Token
import io.circe.Json

case class UserInfo(directoryData: Json, balances: List[Balance], erc_721: List[Erc721Token])
