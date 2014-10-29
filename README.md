jdub
====

*A damn simple JDBC wrapper. Y'know. For databases.*


Requirements
------------

* Java SE 6
* Scala 2.9.2

How To Use
----------

**First**, specify Jdub as a dependency:

```xml
<dependencies>
  <dependency>
    <groupId>com.simple</groupId>
    <artifactId>jdub_${scala.version}</artifactId>
    <version>0.1.0</version>
  </dependency>
</dependencies>
```

(Don't forget to include your JDBC driver!)

**Second**, connect to a database:

```scala
val db = Database.connect("jdbc:postgresql://localhost/wait_what", "myaccount", "mypassword")
```

**Third**, run some queries:

```scala
case class GetUsers() extends Query[List[User]] {
  val sql = trim("""
SELECT id, email, name
  FROM users
""")

  val values = Nil

  def reduce(results: Iterator[Row]) = {
    for (row <- results;
         id <- row.long("id");
         email <- row.string("email");
         name <- row.string("name"))
      yield User(id, email, name)
  }.toList
}
// users = List(User("id1", "user@example.com", "Example"), ...)
val users = db(GetUsers())

case class GetUser(userId: Long) extends FlatSingleRowQuery[User] {
  val sql = trim("""
SELECT id, email, name
  FROM users
 WHERE id = ?
""")

  val values = userId :: Nil

  def flatMap(row: Row) = {
    val id = row.long("id").get
    val email = row.string("email").get
    val name = row.string("name")).get
    User(id, email, name)
  }
}

// this'll print the user record for user #4002
db(GetUser(4002)) match {
  case Some(user) => println(user)
  case None => println("User 4002 not found!")
}
```

**Fourth**, execute some statements:

```scala
case class UpdateUserEmail(userId: Long, oldEmail: String, newEmail: String) extends Statement {
  val sql = trim("""
UPDATE users
   SET email = ?
 WHERE userId = ? AND email = ?
""")

  val values = userId :: oldEmail :: newEmail :: Nil
}

// execute the statement
db.execute(UpdateUserEmail(4002, "old@example.com", "new@example.com"))

// or return the number of rows updated
db.update(UpdateUserEmail(4002, "old@example.com", "new@example.com"))
```

**Fifth**, read up on all the details in [the Jdub tour](tour.md).

License
-------

Copyright (c) 2011-2012 Coda Hale
Copyright (c) 2012-2013 Simple Finance Technology Corp. All rights reserved.

Published under The MIT License, see LICENSE.md
