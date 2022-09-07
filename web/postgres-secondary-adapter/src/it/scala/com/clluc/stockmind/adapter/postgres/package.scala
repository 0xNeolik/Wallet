package com.clluc.stockmind.adapter

import com.typesafe.config.ConfigFactory
import doobie.imports._
import fs2.interop.cats._

package object postgres {

  private val conf = ConfigFactory.load()

  val testTransactor: Transactor[IOLite] = DriverManagerTransactor[IOLite](
    "org.postgresql.Driver",
    conf.getString("database.url"),
    conf.getString("database.user"),
    conf.getString("database.pass")
  )

}
