package com.codahale.jdub.tests

import com.codahale.simplespec.Spec
import java.util.concurrent.atomic.AtomicInteger
import com.codahale.jdub._
import java.io.StringWriter

class DatabaseSpec extends Spec {
  Class.forName("org.hsqldb.jdbcDriver")
  private val i = new AtomicInteger

  class `A database` {
    private val db = Database.connect("jdbc:hsqldb:mem:DbTest" + i.incrementAndGet(), "sa", "")
    db.execute(SQL("DROP TABLE people IF EXISTS"))
    db.execute(SQL("CREATE TABLE people (name varchar(100) primary key, email varchar(100), age int)"))
    db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Coda Hale", "chale@yammer-inc.com", 29)))
    db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Kris Gale", "kgale@yammer-inc.com", 30)))
    db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Old Guy", null, 402)))

    def `returns sets of results` = {
      db(AgesQuery()) must beEqualTo(Set(29, 30, 402))
    }

    def `returns sets of results with null values` = {
      db(EmailQuery()) must beEqualTo(Seq(Some("chale@yammer-inc.com"), Some("kgale@yammer-inc.com"), None))
    }

    def `returns single rows` = {
      db(AgeQuery("Coda Hale")) must beSome(29)
    }

    def `returns empty sets` = {
      db(AgeQuery("Captain Fuzzypants McFrankface")) must beNone
    }
  }
}

case class SQL(sql: String, values: Seq[Any] = Nil) extends Statement

case class AgesQuery() extends Query[Set[Int]]() {
  val sql = "SELECT age FROM people"

  val values = Nil

  def reduce(results: Vector[Vector[Any]]) = results.collect { case Vector(i: Int) => i }.toSet
}

case class AgeQuery(name: String) extends Query[Option[Int]] {
  val sql = trim("SELECT age FROM people WHERE name = ?")

  val values = name :: Nil

  def reduce(results: Vector[Vector[Any]]) = results.headOption.collect { case Vector(i: Int) => i }
}

case class EmailQuery() extends Query[Seq[Option[String]]] {
  val sql = trim("SELECT email FROM people")

  val values = Nil

  def reduce(results: Vector[Vector[Any]]) = results.map {
    case Vector(email: String) => Some(email)
    case _ => None
  }.toIndexedSeq
}
