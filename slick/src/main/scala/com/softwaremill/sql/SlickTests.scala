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

  implicit lazy val cityIdColumnType = MappedColumnType.base[CityId, Int](_.id, CityId)
  implicit lazy val metroSystemIdColumnType = MappedColumnType.base[MetroSystemId, Int](_.id, MetroSystemId)
  implicit lazy val metroLineIdColumnType = MappedColumnType.base[MetroLineId, Int](_.id, MetroLineId)
  implicit lazy val trackTypeColumnType = MappedColumnType.base[TrackType, Int](_.id, TrackType.byIdOrThrow)

  class Cities(tag: Tag) extends Table[City](tag, "city") {
    def id = column[CityId]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def population = column[Int]("population")
    def area = column[Float]("area")
    def link = column[Option[String]]("link")
    def * = (id, name, population, area, link) <> (City.tupled, City.unapply)
  }
  lazy val cities = TableQuery[Cities]

  class MetroSystems(tag: Tag) extends Table[MetroSystem](tag, "metro_system") {
    def id = column[MetroSystemId]("id", O.PrimaryKey)
    def cityId = column[CityId]("city_id")
    def name = column[String]("name")
    def dailyRidership = column[Int]("daily_ridership")
    def * = (id, cityId, name, dailyRidership) <> (MetroSystem.tupled, MetroSystem.unapply)
  }
  lazy val metroSystems = TableQuery[MetroSystems]

  class MetroLines(tag: Tag) extends Table[MetroLine](tag, "metro_line") {
    def id = column[MetroLineId]("id", O.PrimaryKey)
    def systemId = column[MetroSystemId]("system_id")
    def name = column[String]("name")
    def stationCount = column[Int]("station_count")
    def trackType = column[TrackType]("track_type")
    def * = (id, systemId, name, stationCount, trackType) <> (MetroLine.tupled, MetroLine.unapply)
  }
  lazy val metroLines = TableQuery[MetroLines]

  //--

  val selectNamesOfBig: DBIO[Seq[(String, Int)]] = {
    cities.filter(_.population > 4000000).map(c => (c.name, c.population)).result
  }

  case class MetroSystemWithLineCount(metroSystemName: String, cityName: String, lineCount: Int)

  //--

  val selectMetroSystemsWithMostLines: DBIO[Seq[MetroSystemWithLineCount]] = {
    /*
    SELECT ms.name, c.name, COUNT(ml.id) as line_count
    FROM metro_line as ml
    JOIN metro_system as ms on ml.system_id = ms.id
    JOIN city AS c ON ms.city_id = c.id
    GROUP BY ms.id, c.id
    ORDER BY line_count DESC;
     */

    metroLines
      .join(metroSystems).on(_.systemId === _.id)
      .join(cities).on(_._2.cityId === _.id)
      .groupBy { case ((_, ms), c) => (ms.id, c.id, ms.name, c.name) }
      .map { case ((msId, cId, msName, cName), lines) => (msName, cName, lines.length) }
      .sortBy(_._3.desc)
      .result
      .map(_.map(MetroSystemWithLineCount.tupled))
  }

  //--

  val transactions: DBIO[Int] = {
    val combined = for {
      l1 <- selectNamesOfBig
      l2 <- selectMetroSystemsWithMostLines
    } yield l1.length + l2.length

    combined.transactionally
  }

  //--

  val future = db.run(selectMetroSystemsWithMostLines).map { r =>
    r.foreach(println)
  }

  try println(Await.result(future, 1.minute))
  finally db.close()
}