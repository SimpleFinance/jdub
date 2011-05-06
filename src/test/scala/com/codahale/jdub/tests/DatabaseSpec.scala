package com.codahale.jdub.tests

import com.codahale.simplespec.Spec
import com.codahale.jdub.{Value, Query, Statement, Database}

case class ArbitraryStatement(sql: String, values: Seq[Any] = Nil) extends Statement

case class AgesQuery() extends Query[Set[Int]]() {
  val sql = "SELECT age FROM people"

  val values = Nil
  
  def reduce(results: Stream[IndexedSeq[Value]]) = results.map { _.head.toInt }.toSet
}

case class AgeQuery(name: String) extends Query[Option[Int]] {
  val sql = trim("SELECT age FROM people WHERE name = ?")

  val values = name :: Nil

  def reduce(results: Stream[IndexedSeq[Value]]) = results.headOption.map { _.head.toInt }
}

object DatabaseSpec extends Spec {
  class `Querying a database` {
    Class.forName("org.hsqldb.jdbcDriver")
    val db = Database.connect("jdbc:hsqldb:mem:DbTest", "sa", "")
    db.execute(ArbitraryStatement("DROP TABLE people IF EXISTS"))
    db.execute(ArbitraryStatement("CREATE TABLE people (name varchar primary key, email varchar, age int)"))
    db.execute(ArbitraryStatement("INSERT INTO people VALUES (?, ?, ?)", Seq("Coda Hale", "chale@yammer-inc.com", 29)))
    db.execute(ArbitraryStatement("INSERT INTO people VALUES (?, ?, ?)", Seq("Kris Gale", "kgale@yammer-inc.com", 30)))

    def `should return the handled result set` = {
      db(AgesQuery()) must beEqualTo(Set(29, 30))
    }

    def `should return a single row` = {
      db(AgeQuery("Coda Hale")) must beSome(29)
    }
  }
}
