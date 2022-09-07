package com.clluc.stockmind.core.user

case class EmailHandle(value: String) {
  private val pattern = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,10}$".r

  require(
    pattern.findFirstMatchIn(value).isDefined,
    s"Not valid email handle $value"
  )
}
