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

  implicit val trackTypeMeta: Meta[TrackType] =
    Meta[Int].xmap(TrackType.byIdOrThrow, _.id)

  // -- 2.

  val selectNamesOfBig: ConnectionIO[List[City]] = {
    val bigLimit = 4000000

    sql"select id, name, population, area, link from city where population > $bigLimit"
      .query[City]
      .list
  }

  // -- 3.

  case class MetroSystemWithLineCount(metroSystemName: String, cityName: String, lineCount: Int)

  val selectMetroSystemsWithMostLines: ConnectionIO[List[MetroSystemWithLineCount]] = {
    sql"""
      SELECT ms.name, c.name, COUNT(ml.id) as line_count
        FROM metro_line as ml
        JOIN metro_system as ms on ml.system_id = ms.id
        JOIN city AS c ON ms.city_id = c.id
        GROUP BY ms.id, c.id
        ORDER BY line_count DESC
      """.query[MetroSystemWithLineCount].list
  }

  // -- 4.

  def transactions: ConnectionIO[Int] = {
    for {
      l1 <- selectNamesOfBig
      l2 <- selectMetroSystemsWithMostLines
    } yield l1.length + l2.length
  }

  // -- 5.

  def checkQuery(): Unit = {
    import xa.yolo._
    sql"select name from city".query[String].check.unsafePerformIO
  }

  // -- 6.

  selectNamesOfBig.transact(xa).unsafePerformIO.foreach(println)
}
