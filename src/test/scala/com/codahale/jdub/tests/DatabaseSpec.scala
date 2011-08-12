package com.codahale.jdub.tests

import java.util.concurrent.atomic.AtomicInteger
import com.codahale.simplespec.Spec
import com.codahale.simplespec.annotation.test
import com.codahale.jdub._

class DatabaseSpec extends Spec {
  Class.forName("org.hsqldb.jdbcDriver")
  val i = new AtomicInteger

  class `A database` {
    val db = Database.connect("jdbc:hsqldb:mem:DbTest" + i.incrementAndGet(), "sa", "")
    db.execute(SQL("DROP TABLE people IF EXISTS"))
    db.execute(SQL("CREATE TABLE people (name varchar(100) primary key, email varchar(100), age int)"))
    db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Coda Hale", "chale@yammer-inc.com", 29)))
    db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Kris Gale", "kgale@yammer-inc.com", 30)))
    db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Old Guy", null, 402)))

    @test def `returns sets of results` = {
      db(AgesQuery()) must beEqualTo(Set(29, 30, 402))
    }

    @test def `returns sets of results with null values` = {
      db(EmailQuery()) must beEqualTo(Seq(Some("chale@yammer-inc.com"), Some("kgale@yammer-inc.com"), None))
    }

    @test def `returns single rows` = {
      db(AgeQuery("Coda Hale")) must beSome(29)
    }

    @test def `returns empty sets` = {
      db(AgeQuery("Captain Fuzzypants McFrankface")) must beNone
    }

    class `transaction` {
      @test def `commits by default` = {
        db.transaction { txn =>
          txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))
        }

        db(AgesQuery()) must beEqualTo(Set(29, 30, 402, 5))
      }

      @test def `can rollback` = {
        db.transaction { txn =>
          txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))

          txn.rollback()
        }

        db(AgesQuery()) must beEqualTo(Set(29, 30, 402))
      }

      @test def `rolls back the transaction if an exception is thrown` = {
        def inserting() {
          db.transaction { txn =>
            txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))

            throw new IllegalArgumentException("OH NOES")
          }
        }
        
        inserting() must throwA[IllegalArgumentException]

        db(AgesQuery()) must beEqualTo(Set(29, 30, 402))
      }
    }
  }
}

case class SQL(sql: String, values: Seq[Any] = Nil) extends Statement

case class AgesQuery() extends FlatCollectionQuery[Set, Int](Set) {
  val sql = "SELECT age FROM people"

  val values = Nil
  
  def flatMap(row: Row) = row.int(0)
}

case class AgeQuery(name: String) extends FlatSingleRowQuery[Int] {
  val sql = trim("SELECT age FROM people WHERE name = ?")

  val values = name :: Nil

  def flatMap(row: Row) = row.int(0)
}

case class EmailQuery() extends CollectionQuery[Vector, Option[String]](Vector) {
  val sql = trim("SELECT email FROM people")

  val values = Nil

  def map(row: Row) = row.string("email")
}
