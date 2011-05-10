package com.codahale.jdub.tests

import com.codahale.jdub._
import com.codahale.simplespec.Spec

object DatabaseSpec extends Spec {
  trait Context {
    Class.forName("org.hsqldb.jdbcDriver")
    val db = Database.connect("jdbc:hsqldb:mem:DbTest" + System.nanoTime(), "sa", "")
    db.execute(SQL("DROP TABLE people IF EXISTS"))
    db.execute(SQL("CREATE TABLE people (name varchar(100) primary key, email varchar(100), age int)"))
    db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Coda Hale", "chale@yammer-inc.com", 29)))
    db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Kris Gale", "kgale@yammer-inc.com", 30)))
  }

  class `Querying a database for a set of results` extends Context {
    def `should return the reduced set of results` = {
      db(AgesQuery()) must beEqualTo(Set(29, 30))
    }
  }

  class `Querying a database for a single row` extends Context {
    def `should return the row` = {
      db(AgeQuery("Coda Hale")) must beSome(29)
    }
  }

  class `Querying a database for an empty set` extends Context {
    def `should handle that gracefully` = {
      db(AgeQuery("Captain Fuzzypants McFrankface")) must beNone
    }
  }
}

case class SQL(sql: String, values: Seq[Any] = Nil) extends Statement

case class AgesQuery() extends Query[Set[Int]]() {
  val sql = "SELECT age FROM people"

  val values = Nil

  def reduce(results: Stream[IndexedSeq[Value]]) = results.map {_.head.toInt}.toSet
}

case class AgeQuery(name: String) extends Query[Option[Int]] {
  val sql = trim("SELECT age FROM people WHERE name = ?")

  val values = name :: Nil

  def reduce(results: Stream[IndexedSeq[Value]]) = results.headOption.map {_.head.toInt}
}
