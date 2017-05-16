package com.softwaremill.sql

import com.softwaremill.sql.TrackType.TrackType
import io.getquill.{PostgresAsyncContext, SnakeCase}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.{ global => ec } // making tx-scoped ECs work
import scala.concurrent.duration._

object QuillTests extends App with DbSetup {
  dbSetup()

  lazy val ctx = new PostgresAsyncContext[SnakeCase]("ctx")
  import ctx._

  // -- 1.

  implicit val encodeTrackType = MappedEncoding[TrackType, Int](_.id)
  implicit val decodeTrackType = MappedEncoding[Int, TrackType](TrackType.byIdOrThrow)

  // -- 2.

  def selectNamesOfBig(): Future[Unit] = {
    val bigLimit = 4000000

    val q = quote {
      query[City].filter(_.population > lift(bigLimit)).map(_.name)
    }

    ctx.run(q).map(_.foreach(println))
  }

  // -- 3.

  def selectMetroLinesSortedByStations(): Future[Unit] = {
    case class MetroLineWithSystemCityNames(
      metroLineName: String, metroSystemName: String, cityName: String, stationCount: Int)

    // other joins (using for comprehensions cause compile errors)
    val q = quote {
      (for {
        ((ml, ms), c) <- query[MetroLine]
          .join(query[MetroSystem]).on(_.systemId == _.id)
          .join(query[City]).on(_._2.cityId == _.id)
      } yield (ml.name, ms.name, c.name, ml.stationCount)).sortBy(_._4)(Ord.desc)
    }

    ctx.run(q)
      .map(_.map(MetroLineWithSystemCityNames.tupled))
      .map(_.foreach(println))
  }

  // -- 4.

  def transactions(): Future[Int] = {
    def select1(implicit ec: ExecutionContext): Future[Seq[String]] = ctx.run {
      query[City].map(_.name)
    }

    def select2(implicit ec: ExecutionContext): Future[Seq[Int]] = ctx.run {
      query[City].map(_.population)
    }

    ctx.transaction { implicit ec =>
      for {
        l1 <- select1
        l2 <- select2
      } yield l1.length + l2.length
    }
  }

  // -- 5.

  try Await.result(selectNamesOfBig(), 1.minute)
  finally ctx.close()
}
