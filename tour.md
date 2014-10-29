# A Tour of Jdub

The basic [`Query` class](src/main/scala/com/simple/jdub/Query.scala) defines an abstract method `reduce` and inherits abstract fields `sql` and `values` from the [`RawQuery` trait]. The concrete classes you create based on `Query`, then, must fill out those three items. The `sql` field contains the actual SQL code, with optional input parameters denoted by '?'. That SQL code becomes a Java [`PreparedStatement`](http://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html) under the hood, with the content of `values` passed in for the input parameters. The resulting rows are passed to the `reduce` method which defines the actual output of your query.

Minimal code to get a list of `id` column values for all rows of a table `users` would be:
```scala
case class GetIds() extends Query[List[Long]] {

  // The trailing slash is optional.
  val sql = "SELECT id FROM users;"

  // If no input parameters are used, Nil defines the empty List.
  val values = Nil

  def reduce(results: Iterator[Row]) = {
    results.map { row =>
      row.long("id").get
    }.toList
}
val ids = db(GetIds())
```

If you have a more complex SQL query, you'll want to break it across lines. The `trim` function from [`SqlBase`](src/main/scala/com/simple/jdub/SqlBase.scala) is provided as a convenience for removing that excess whitespace. The above SQL string could be reproduced exactly as:
```scala
val sql = trim("""
    SELECT id
    FROM users;
    """)
```

Input parameters are used to pass values into SQL statements. The values are taken from the `values` field. If the same value is needed multiple times in the query, simply provide it multiple times when setting `values`:

    val values = userId :: userName :: userId :: Nil

Note that input parameters cannot be used to provide table or column names. If you'd like to reuse the same code for multiple tables or multiple columns, consider carefully the security implications. One pattern is to define the base query in a sealed class, then define case classes in the same file for all needed combinations of input parameters. This prevents other code from using that base class:
```scala
sealed class GetIds(table: String, city: String) extends Query[List[Long]] {

  val sql = trim("""
    SELECT id
    FROM %s
    WHERE city = ?;
    """.format(column))

  val values = city :: Nil

  def reduce(results: Iterator[Row]) = {
    results.map { row =>
      row.long("id").get
    }.toList
}
case class GetPortlandUserIds() extends GetIds("user", "Portland")
case class GetNewYorkAdminIds() extends GetIds("admin", "New York")
```

If you want to count items or make some other query that's expected to return only a single row, you'll want to implement the [`SingleRowQuery` or `FlatSingleRowQuery`](src/main/scala/com/simple/jdub/SingleRowQuery.scala) traits instead of `Query`. These define abstract `map` and `flatMap` methods, respectively, instead of `reduce`. See [README.md](README.md) for an example of `FlatSingleRowQuery` in use.

**TODO:** Explanations/examples of [`CollectionQuery`](src/main/scala/com/simple/jdub/CollectionQuery.scala), [`Statement`](src/main/scala/com/simple/jdub/Statement.scala), and [`Transaction`](src/main/scala/com/simple/jdub/Transaction.scala).
