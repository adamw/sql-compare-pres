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

  // -- 2.

  // -- 3.

  // -- 4.

  // -- 5.
}
