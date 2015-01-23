# A Tour of Jdub

## Anatomy of a Query
The various traits of the Jdub querying API all derive from the basic [`Query` trait](src/main/scala/com/simple/jdub/Query.scala). Once you understand `Query`, it is only a small leap to understand the higher-level examples in the [README](README.md).

A concrete implementation of `Query` must define a method `reduce` (declared in `Query`) and two fields: `sql` and `values` (declared in the [`RawQuery` trait](src/main/scala/com/simple/jdub/RawQuery.scala)). The `sql` field contains the SQL query to run, with optional bind parameters denoted by `?`. Under the hood, that SQL code becomes a Java [`PreparedStatement`](http://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html), with bind parameters given by the items in `values`. When your query class is executed, Jdub fetches the results and passes them to `reduce`, which produces your final result.

Minimal code to get a list of `id` column values for all rows of a table `users` would be:
```scala
case object GetIds extends Query[Seq[Long]] {

  // The trailing semicolon is optional.
  val sql = "SELECT id FROM users;"

  // If no bind parameters are used, Nil defines the empty List.
  val values = Seq()

  def reduce(results: Iterator[Row]) = {
    results.map { row =>
      row.long("id").get
    }.toSeq

}

val ids = db(GetIds) // Seq("3723", "4559", ...)
```

### The `trim` Function
If you have a more complex SQL query, you'll want to break it across lines. The `trim` function from [`SqlBase`](src/main/scala/com/simple/jdub/SqlBase.scala) is provided as a convenience for removing that excess whitespace. The above SQL string could be reproduced exactly as:
```scala
val sql = trim("""
    SELECT id
    FROM users;
    """)
```

Note that `trim` will break any query using SQL-standard comments (`-- comment`). Use C-style comments instead (`/* comment */`).

### Bind Parameters and Security
Bind parameters are used to pass values into SQL statements. The values are taken from the `values` field. If the same value is needed multiple times in the query, simply provide it multiple times when setting `values`:

    val values = Seq(userId, userName, userId)

Note that bind parameters will always be escaped to prevent [SQL injection attacks](http://en.wikipedia.org/wiki/SQL_injection). Of particular importance, this means that bind parameters cannot be used to provide table or column names. It is good security practice to make the value of `sql` as specific as possible, minimizing the chance of the queries being used in unexpected ways.

There are situations, however, where the benefits of code reuse make parameterized table or column names very attractive. One good pattern for this is to create a single file that defines a sealed query class along with case classes or case objects for all needed combinations of parameters. This prevents other code from exploiting that base class:
```scala
sealed class GetIds(table: String, city: String) extends Query[Seq[Long]] {

  val sql = trim("""
    SELECT id
    FROM %s -- table
    WHERE city = ?;
    """.format(table))

  val values = Seq(city)

  def reduce(results: Iterator[Row]) = {
    results.map { row =>
      row.long("id").get
    }.toSeq
  }

}

case object GetPortlandUserIds extends GetIds("user", "Portland")
// Okay to leave city unspecified, since it is a bind parameter.
case class GetAdminIds(city: String) extends GetIds("admin", city)
```

## Single Row Queries and Collection Queries
In practice, you will rarely use `Query`, as `SingleRowQuery` and `CollectionQuery` (and their variants) provide simpler interfaces for most use cases. Implementing `Query` directly should only be necessary when you need to return a map or perform non-trivial processing on the query results.

For counts and other single result queries, you can implement the [`SingleRowQuery` or `FlatSingleRowQuery`](src/main/scala/com/simple/jdub/SingleRowQuery.scala) traits. Instead of a `reduce` member, `SingleRowQuery` requires that you implement a `map` member which takes in a single row as input. Only the first row matching the query is returned, and an exception will be thrown in the case of no matching rows. The `FlatSingleRowQuery` trait requires that you implement `flatMap`, which wraps your result in `Option`, returning `None` in the case of no matching rows.

To return a collection, implement the [`CollectionQuery` or `FlatCollectionQuery`](src/main/scala/com/simple/jdub/SingleRowQuery.scala) traits. Like the single row queries, these define abstract `map` and `flatMap` methods, respectively, in place of `Query`'s `reduce`.

You can see examples of these query types in the [README](README.md) and also in the [database test suite](src/test/scala/com/simple/jdub/tests/DatabaseSpec.scala).

## Transactions and Transaction Lifecycle callbacks
[`Transactions`](src/main/scala/com/simple/jdub/Transaction.scala) are an integral part of successfully managing complex database code and Jdub makes using them fairly simple. By default, Jdub doesn't wrap your db calls in a transaction, so to start a transaction, you need to create or append to the transaction scope:
```scala
def doWork(db: Database) {
  db.transactionScope {
    // code you want to execute in a transaction
  }
}
```

You can nest transaction scopes as well, where all inner transactions scopes will not commit until the outermost transaction scope commits:
```scala
def doWork(db: Database) {
  db.transactionScope {
    db.transactionScope {
      db.transactionScope {
        // will not commit until outermost transaction commits
      }
    }
  }
}
```

Sometimes you need to explicitly force a new transaction to commit even when it is nested inside of another transaction scope. To do this use the `newTransactionScope` call. This will force the calls in the transaction scope to be committed at the end of the transaction scope:
```scala
def doWork(db: Database) {
  db.transactionScope {
    db.transactionScope {
      db.newTransactionScope {
        // will commit at the end of this scope
        // not added to existing transaction scope(s)
      }
    }
  }
}
```
### Rolling back a transaction
To rollback a transaction, simply call the `rollback` method on the current transaction:
```scala
def doWork(db: Database) {
  db.transactionScope {
    db.currentTransaction.rollback()
  }
}
```

This is especially helpful when you are dealing with complex error handling logic or doing 'dry runs' of your database logic.

### Lifecycle calls
Jdub provides a few lifecycle callbacks that you can hook into for different parts of the transaction lifecycle.
* onCommit - called after the transaction successfully commits
* onRollback - called after the transaction has been rolled back
* onClose - called after the transaction has been closed (will always be called and will always be called after onCommit or onRollback)

Jdub allows you to set any number of callbacks for each lifecycle. The callbacks are stored as mutable lists and can simply be appended to:
```scala
def doWork(db: Database) {
  db.transactionScope {
    db.currentTransaction.onCommit += { () =>
      // code you want to execute on commit
    }

    db.currentTransaction.onRollback += { () =>
      // code you want to execute on rollback
    }

    db.currentTransaction.onClose += { () =>
      // code you want to execute on close
    }
  }
}
```

Code flow with lifecycle calls (successful commit):
```scala
def doWork(db: Database) {
  db.transactionScope {
    db.execute(/* statement A */)

    db.currentTransaction.onCommit += { () =>
      // code you want to execute on commit
    }

    db.execute(/* statement B */)

    db.currentTransaction.onClose += { () =>
      // code you want to execute on close
    }

    db.execute(/* statement C */)
  }
}
```
* Transaction scope is started
* Statement A is executed
* onCommit handler added (but not executed)
* Statement B is executed
* onClose handler added (but not executed)
* Statement C is executed
* Transaction scope ended
* onCommit handler is executed
* onClose handler is executed

Code flow with lifecycle calls (with rollback):
```scala
def doWork(db: Database) {
  db.transactionScope {
    db.execute(/* statement A */)

    db.currentTransaction.onRollback += { () =>
      // code you want to execute on rollback
    }

    db.execute(/* statement B */)

    db.currentTransaction.onClose += { () =>
      // code you want to execute on close
    }

    db.currentTransaction.rollback()
  }
}
```
* Transaction scope is started
* Statement A is executed
* onRollback handler added (but not executed)
* Statement B is executed
* onClose handler added (but not executed)
* Transaction is rolled back
* onRollback handler is executed
* onClose handler is executed


## **TODO**
Explanations/examples of [`Statement`](src/main/scala/com/simple/jdub/Statement.scala). See examples in the [database test suite](src/test/scala/com/simple/jdub/tests/DatabaseSpec.scala).
