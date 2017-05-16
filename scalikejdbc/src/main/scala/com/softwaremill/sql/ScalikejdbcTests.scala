package com.softwaremill.sql

import java.sql.ResultSet

import com.softwaremill.sql.TrackType.TrackType
import scalikejdbc._

object ScalikejdbcTests extends App with DbSetup {
  dbSetup()

  ConnectionPool.add('tests, "jdbc:postgresql:sql_compare", "postgres", "")
  def db: NamedDB = NamedDB('tests)

  //--

  implicit val cityIdTypeBinder: TypeBinder[CityId] = new TypeBinder[CityId] {
    def apply(rs: ResultSet, label: String): CityId = CityId(rs.getInt(label))
    def apply(rs: ResultSet, index: Int): CityId = CityId(rs.getInt(index))
  }

  implicit val metroSystemIdTypeBinder: TypeBinder[MetroSystemId] = new TypeBinder[MetroSystemId] {
    def apply(rs: ResultSet, label: String): MetroSystemId = MetroSystemId(rs.getInt(label))
    def apply(rs: ResultSet, index: Int): MetroSystemId = MetroSystemId(rs.getInt(index))
  }

  implicit val metroLineIdTypeBinder: TypeBinder[MetroLineId] = new TypeBinder[MetroLineId] {
    def apply(rs: ResultSet, label: String): MetroLineId = MetroLineId(rs.getInt(label))
    def apply(rs: ResultSet, index: Int): MetroLineId = MetroLineId(rs.getInt(index))
  }

  implicit val trackTypeTypeBinder: TypeBinder[TrackType] = new TypeBinder[TrackType] {
    def apply(rs: ResultSet, label: String): TrackType = TrackType.byIdOrThrow(rs.getInt(label))
    def apply(rs: ResultSet, index: Int): TrackType = TrackType.byIdOrThrow(rs.getInt(index))
  }

  //--

  class CitySQL(db: NamedDB) extends SQLSyntaxSupport[City] {
    override def connectionPoolName: Any = db.name
    override def tableName: String = "city"

    def apply(rs: WrappedResultSet, rn: ResultName[City]): City = autoConstruct[City](rs, rn)
  }

  class MetroSystemSQL(db: NamedDB) extends SQLSyntaxSupport[MetroSystem] {
    override def connectionPoolName: Any = db.name
    override def tableName: String = "metro_system"

    def apply(rs: WrappedResultSet, rn: ResultName[MetroSystem]): MetroSystem = autoConstruct[MetroSystem](rs, rn)
  }

  class MetroLineSQL(db: NamedDB) extends SQLSyntaxSupport[MetroLine] {
    override def connectionPoolName: Any = db.name
    override def tableName: String = "metro_line"

    def apply(rs: WrappedResultSet, rn: ResultName[MetroLine]): MetroLine = autoConstruct[MetroLine](rs, rn)
  }

  val citySQL = new CitySQL(db)
  val metroSystemSQL = new MetroSystemSQL(db)
  val metroLineSQL = new MetroLineSQL(db)

  //--

  val selectNamesOfBig: SQLToList[City, HasExtractor] = {
    val bigLimit = 4000000

    val c = citySQL.syntax("c")
    withSQL {
      select.from(citySQL as c).where.gt(c.population, bigLimit)
    }.map(citySQL.apply(_, c.resultName)).list()
  }

  //--

  case class MetroSystemWithLineCount(metroSystemName: String, cityName: String, lineCount: Int)

  val selectMetroSystemsWithMostLines: SQLToList[MetroSystemWithLineCount, HasExtractor] = {
    val (ml, ms, c) = (metroLineSQL.syntax("ml"), metroSystemSQL.syntax("ms"), citySQL.syntax("c"))
    withSQL {
      select(ms.result.column("name"), c.result.column("name"), c.result.column("name"), sqls"count(ml.id) as line_count")
        .from(metroLineSQL as ml)
        .join(metroSystemSQL as ms).on(ml.systemId, ms.id)
        .join(citySQL as c).on(ms.cityId, c.id)
        .groupBy(ms.id, c.id)
        .orderBy(sqls"line_count").desc
    }
      .map(rs => MetroSystemWithLineCount(rs.string(ms.resultName.name), rs.string(c.resultName.name),
        rs.int("line_count")))
      .list()
  }

  //--

  def transactions(): Int = {
    db.localTx { implicit session =>
      val l1 = selectNamesOfBig.apply()
      val l2 = selectMetroSystemsWithMostLines.apply()
      l1.length + l2.length
    }
  }

  //--

  db.readOnly { implicit session =>
    selectNamesOfBig.apply().foreach(println)
  }

  println(transactions())
}