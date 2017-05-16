package com.softwaremill.sql

import doobie.imports._
import cats._
import cats.data._
import cats.implicits._
import com.softwaremill.sql.TrackType.TrackType
import fs2.interop.cats._

object DoobieTests extends App with DbSetup {
  dbSetup()
  val xa = DriverManagerTransactor[IOLite]("org.postgresql.Driver", "jdbc:postgresql:sql_compare", null, null)

  // -- 1.

  // -- 2.

  // -- 3.

  // -- 4.

  // -- 5.

  // -- 6.
}
