package com.clluc.stockmind.core.twitter

case class TwitterHandle(value: String) {
  private val pattern = "^[a-zA-Z0-9_]{1,15}$".r

  require(
    pattern.findFirstMatchIn(value).isDefined &&
      !value.toLowerCase.contains("twitter") &&
      !value.toLowerCase.contains("admin"),
    s"Not valid twitter handle $value"
  )
}
