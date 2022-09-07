package com.clluc.stockmind.controller

import akka.util.ByteString
import io.circe.{Json, Printer}
import play.api.http.Writeable
import play.api.mvc._
import play.api.http.MimeTypes._

private[controller] trait ErrorToResultConversions extends Results {

  def genericPlainTextWriteable[T](tDescFx: T => String): Writeable[T] = new Writeable[T](
    t => ByteString(tDescFx(t), "UTF-8"),
    Some(TEXT)
  )

  private val jsonPrinter = Printer(preserveOrder = true, dropNullKeys = true, indent = "")

  def genericJsonWriteable[T](fx: T => Json): Writeable[T] = new Writeable[T](
    t => ByteString(fx(t).pretty(jsonPrinter)),
    Some(JSON)
  )
}
