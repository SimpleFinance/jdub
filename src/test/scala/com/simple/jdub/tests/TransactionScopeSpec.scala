/**
  * © 2016 Simple Finance Technology Corp. All rights reserved.
  */
package com.simple.jdub.tests

class TransactionScopeSpec extends JdubSpec {
  test("current transaction throws when no scope") {
    intercept[Exception] {
      db.currentTransaction
    }
  }

  test("rollback throws when no scope") {
    intercept[Exception] {
      db.rollback()
    }
  }

  test("current transaction returns in scope") {
    db.transactionScope {
      db.currentTransaction
    }
  }

  test("commits by default") {
    db.transactionScope {
      db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))
    }

    db(AgesQuery()).must(be(Set(29, 30, 402, 5)))
  }

  test("can rollback") {
    db.transactionScope {
      db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))

      db.rollback()
    }

    db(AgesQuery()).must(be(Set(29, 30, 402)))
  }

  test("rolls back the transaction if an exception is thrown") {
    intercept[IllegalArgumentException] {
      db.transactionScope {
        db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))

        throw new IllegalArgumentException("OH NOES")
      }
    }

    db(AgesQuery()).must(be(Set(29, 30, 402)))
  }

  test("allows nesting") {
    db.transactionScope {
      val transaction = db.currentTransaction
      db.transactionScope {
        db.currentTransaction.must(be(transaction))
      }
    }

    intercept[Exception] {
      db.currentTransaction
    }
  }

  test("explicit new transaction creates new scope") {
    db.transactionScope {
      val transaction = db.currentTransaction
      db.newTransactionScope {
        db.currentTransaction.must(not(be(transaction)))
      }
      db.currentTransaction.must(be(transaction))
    }
  }

  test("explicit transaction joins scope") {
    db.transactionScope {
      val transaction = db.currentTransaction
      db.transaction { txn =>
        txn.must(be(transaction))
      }
    }
  }

  test("explicit transaction joins scope and rollback") {
    db.transactionScope {
      db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))
      db.transaction { txn =>
        txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Other Guy", null, 6)))
      }
      db.rollback()
    }

    db(AgesQuery()).must(be(Set(29, 30, 402)))
  }

  test("explicit transaction joins scope and causes rollback") {
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
