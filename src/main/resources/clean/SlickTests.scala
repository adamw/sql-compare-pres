package com.softwaremill.sql

import com.softwaremill.sql.TrackType.TrackType
import slick.jdbc.JdbcBackend._
import slick.jdbc.PostgresProfile

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object SlickTests extends App with DbSetup {
  dbSetup()
  val db = Database.forURL(connectionString, driver = "org.postgresql.Driver")
  val jdbcProfile = PostgresProfile

  import jdbcProfile.api._

  // -- 1.

  // -- 2.

  // -- 3.

  // -- 4.

  // -- 5.

  // -- 6.

}