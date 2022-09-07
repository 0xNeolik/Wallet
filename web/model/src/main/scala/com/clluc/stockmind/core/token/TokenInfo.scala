package com.clluc.stockmind.core.token

import com.clluc.stockmind.core.ethereum.Ethtoken

case class AllTokensInfo(erc20Tokens: List[Ethtoken], erc721Tokens: List[Ethtoken])
