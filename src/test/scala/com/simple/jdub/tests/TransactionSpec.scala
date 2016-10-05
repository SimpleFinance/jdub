/**
  * © 2016 Simple Finance Technology Corp. All rights reserved.
  */
package com.simple.jdub.tests

class TransactionSpec extends JdubSpec {

  test("commits by default") {
    db.transaction { txn =>
      txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))
    }

    db(AgesQuery()).must(be(Set(29, 30, 402, 5)))
  }

  test("can rollback") {
    db.transaction { txn =>
      txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))

      txn.rollback()
    }

    db(AgesQuery()).must(be(Set(29, 30, 402)))
  }

  test("fires onCommit and onClose after commit") {
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

  test("fires onRollback and onClose if an exception is thrown") {
    var committed = false
    var rolledback = false
    var closed = false

    intercept[IllegalArgumentException] {
      db.transaction {txn =>
        txn.onCommit += { () => committed = true }
        txn.onRollback += { () => rolledback = true }
        txn.onClose += { () => closed = true }

        txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))

        throw new IllegalArgumentException("OH NOES")
      }
    }

    committed.must(be(false))
    rolledback.must(be(true))
    closed.must(be(true))
  }

  test("fires onRollback and onClose if rollback") {
    var committed = false
    var rolledback = false
    var closed = false

    db.transaction {txn =>
      txn.onCommit += { () => committed = true }
      txn.onRollback += { () => rolledback = true }
      txn.onClose += { () => closed = true }

      txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))

      txn.rollback()
    }

    committed.must(be(false))
    rolledback.must(be(true))
    closed.must(be(true))
  }

  test("rolls back the transaction if an exception is thrown") {
    intercept[IllegalArgumentException] {
      db.transaction {txn =>
        txn.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("New Guy", null, 5)))

        throw new IllegalArgumentException("OH NOES")
      }
    }

    db(AgesQuery()).must(be(Set(29, 30, 402)))
  }
}
