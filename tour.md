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

case object GetPortlandUserIds extends GetIds("user", "Portland")
// Okay to leave city unspecified, since it is a bind parameter.
case class GetAdminIds(city: String) extends GetIds("admin", city)
```

## Single Row Queries and Collection Queries
In most cases, you will want your queries to return either a single row or a collection whose items represent each matching row. Special traits are defined for these cases, meaning you will rarely need to use the base `Query` trait in practice.

For counts and other single result queries, you can implement the [`SingleRowQuery` or `FlatSingleRowQuery`](src/main/scala/com/simple/jdub/SingleRowQuery.scala) traits. Instead of a `reduce` member, `SingleRowQuery` requires that you implement a `map` member which takes in a single row as input. Only the first row matching the query is returned, and an exception will be thrown in the case of no matching rows. The `FlatSingleRowQuery` trait requires that you implement `flatMap`, which wraps your result in `Option`, returning `None` in the case of no matching rows.

To return a collection, implement the [`CollectionQuery` or `FlatCollectionQuery`](src/main/scala/com/simple/jdub/SingleRowQuery.scala) traits. Like the single row queries, these define abstract `map` and `flatMap` methods, respectively, in place of `Query`'s `reduce`.

You can see examples of these query types in the [README](README.md) and also in the [database test suite](src/test/scala/com/simple/jdub/tests/DatabaseSpec.scala).

## **TODO**
Explanations/examples of [`Statement`](src/main/scala/com/simple/jdub/Statement.scala), and [`Transaction`](src/main/scala/com/simple/jdub/Transaction.scala). See examples in the [database test suite](src/test/scala/com/simple/jdub/tests/DatabaseSpec.scala).
