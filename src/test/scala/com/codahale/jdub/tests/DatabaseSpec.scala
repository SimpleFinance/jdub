package com.codahale.jdub.tests

import org.specs2.mutable.Specification
import java.util.concurrent.atomic.AtomicInteger
import org.specs2.specification.Scope
import com.codahale.jdub._

class DatabaseSpec extends Specification {
  "Querying a database for a set of results" should {
    "return the reduced set of results" in new context {
      db(AgesQuery()) must beEqualTo(Set(29, 30))
    }
  }

  "Querying a database for a single row" should {
    "return the row" in new context {
      db(AgeQuery("Coda Hale")) must beSome(29)
    }
  }

  "Querying a database for an empty set" should {
    "handle that gracefully" in new context {
//      db(new Poop) must beNone
      db(AgeQuery("Captain Fuzzypants McFrankface")) must beNone
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

  val counter = new AtomicInteger
  trait context extends Scope {
    Class.forName("org.hsqldb.jdbcDriver")
    val db = Database.connect("jdbc:hsqldb:mem:DbTest" + counter.incrementAndGet(), "sa", "")
    db.execute(SQL("DROP TABLE people IF EXISTS"))
    db.execute(SQL("CREATE TABLE people (name varchar primary key, email varchar, age int)"))
    db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Coda Hale", "chale@yammer-inc.com", 29)))
    db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Kris Gale", "kgale@yammer-inc.com", 30)))
  }
}
