package com.simple.jdub.tests

import java.util.concurrent.atomic.AtomicInteger
import com.simple.simplespec.Spec
import org.junit.Test
import com.simple.jdub._

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

    @Test def `returns sets of results` = {
      db(AgesQuery()).must(be(Set(29, 30, 402)))
    }

    @Test def `returns sets of results with null values` = {
      db(EmailQuery()).must(be(Vector(Some("chale@yammer-inc.com"), Some("kgale@yammer-inc.com"), None)))
    }

    @Test def `returns single rows` = {
      db(AgeQuery("Coda Hale")).must(be(Some(29)))
    }

    @Test def `returns empty sets` = {
      db(AgeQuery("Captain Fuzzypants McFrankface")).must(be(None))
    }

    @Test def `converts None to null correctly`() {
      db(AgeNullQuery(None)).must(be(None))
      db(AgeNullQuery(null)).must(be(None))
    }

    class `transaction` {
      @Test def `commits by default` = {
        db.transaction { txn =>
          txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))
        }

        db(AgesQuery()).must(be(Set(29, 30, 402, 5)))
      }

      @Test def `can rollback` = {
        db.transaction { txn =>
          txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))

          txn.rollback()
        }

        db(AgesQuery()).must(be(Set(29, 30, 402)))
      }

      @Test def `fires onCommit and onClose after commit` = {
        var committed = false
        var rolledback = false
        var closed = false

        db.transaction { txn =>
          txn.onCommit += { () => committed = true }
          txn.onRollback += { () => rolledback = true }
          txn.onClose += { () => closed = true }

          txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))
        }

        committed.must(be(true))
        rolledback.must(be(false))
        closed.must(be(true))
      }

      @Test def `fires onRollback and onClose if an exception is thrown` = {
        var committed = false
        var rolledback = false
        var closed = false

        evaluating {
          db.transaction {txn =>
            txn.onCommit += { () => committed = true }
            txn.onRollback += { () => rolledback = true }
            txn.onClose += { () => closed = true }

            txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))

            throw new IllegalArgumentException("OH NOES")
          }
        }.must(throwAn[IllegalArgumentException])

        committed.must(be(false))
        rolledback.must(be(true))
        closed.must(be(true))
      }

      @Test def `fires onRollback and onClose if rollback` = {
        var committed = false
        var rolledback = false
        var closed = false

        db.transaction {txn =>
          txn.onCommit += { () => committed = true }
          txn.onRollback += { () => rolledback = true }
          txn.onClose += { () => closed = true }

          txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))

          txn.rollback
        }

        committed.must(be(false))
        rolledback.must(be(true))
        closed.must(be(true))
      }

      @Test def `rolls back the transaction if an exception is thrown` = {
        evaluating {
          db.transaction {txn =>
            txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))

            throw new IllegalArgumentException("OH NOES")
          }
        }.must(throwAn[IllegalArgumentException])

        db(AgesQuery()).must(be(Set(29, 30, 402)))
      }
    }

    class `transaction scope` {
      @Test def `current transaction throws when no scope` = {
        evaluating {
          db.currentTransaction
        }.must(throwAn[Exception])
      }

      @Test def `rollback throws when no scope` = {
        evaluating {
          db.rollback()
        }.must(throwAn[Exception])
      }

      @Test def `current transaction returns in scope` = {
        db.transactionScope {
          db.currentTransaction
        }
      }

      @Test def `commits by default` = {
        db.transactionScope {
          db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))
        }

        db(AgesQuery()).must(be(Set(29, 30, 402, 5)))
      }

      @Test def `can rollback` = {
        db.transactionScope {
          db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))

          db.rollback()
        }

        db(AgesQuery()).must(be(Set(29, 30, 402)))
      }

      @Test def `rolls back the transaction if an exception is thrown` = {
        evaluating {
          db.transactionScope {
            db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))

            throw new IllegalArgumentException("OH NOES")
          }
        }.must(throwAn[IllegalArgumentException])

        db(AgesQuery()).must(be(Set(29, 30, 402)))
      }

      @Test def `allows nesting` = {
        db.transactionScope {
          val transaction = db.currentTransaction
          db.transactionScope {
            db.currentTransaction.must(be(transaction))
          }
        }

        evaluating {
          db.currentTransaction
        }.must(throwAn[Exception])
      }

      @Test def `explict new transaction creates new scope` = {
        db.transactionScope {
          val transaction = db.currentTransaction
          db.newTransactionScope {
            db.currentTransaction.must(not(be(transaction)))
          }
          db.currentTransaction.must(be(transaction))
        }
      }

      @Test def `explict transaction joins scope` = {
        db.transactionScope {
          val transaction = db.currentTransaction
          db.transaction { txn =>
            txn.must(be(transaction))
          }
        }
      }

      @Test def `explict transaction joins scope and rollback` = {
        db.transactionScope {
          db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))
          db.transaction { txn =>
            txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Other Guy", null, 6)))
          }
          db.rollback()
        }

        db(AgesQuery()).must(be(Set(29, 30, 402)))
      }

      @Test def `explict transaction joins scope and causes rollback` = {
        db.transactionScope {
          db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))
          db.transaction { txn =>
            txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Other Guy", null, 6)))
            txn.rollback()
          }
        }

        db(AgesQuery()).must(be(Set(29, 30, 402)))
      }
    }
  }
}

case class SQL(sql: String, values: Seq[Any] = Nil) extends Statement

case class AgesQuery() extends FlatCollectionQuery[Set, Int] {
  val sql = "SELECT age FROM people"

  val values = Nil

  def flatMap(row: Row) = row.int(0)
}

case class AgeQuery(name: String) extends FlatSingleRowQuery[Int] {
  val sql = trim("SELECT age FROM people WHERE name = ?")

  val values = name :: Nil

  def flatMap(row: Row) = row.int(0)
}

case class AgeNullQuery(name: Option[String]) extends FlatSingleRowQuery[Int] {
  val sql = trim("SELECT age FROM people WHERE name = ?")

  val values = name :: Nil

  def flatMap(row: Row) = row.int(0)
}


case class EmailQuery() extends CollectionQuery[Vector, Option[String]] {
  val sql = trim("SELECT email FROM people")

  val values = Nil

  def map(row: Row) = row.string("email")
}
