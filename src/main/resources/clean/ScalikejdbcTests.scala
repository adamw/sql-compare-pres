package com.softwaremill.sql

import java.sql.ResultSet

import com.softwaremill.sql.TrackType.TrackType
import scalikejdbc._

object ScalikejdbcTests extends App with DbSetup {
  dbSetup()

  ConnectionPool.add('tests, "jdbc:postgresql:sql_compare", "postgres", "")
  def db: NamedDB = NamedDB('tests)

  // -- 1.


  // -- 2.


  // -- 3.


  // -- 4.


  // -- 5.


  // -- 6.

}