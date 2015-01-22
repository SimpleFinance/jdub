jdub
====

*A damn simple JDBC wrapper. Y'know. For databases.*


Requirements
------------

* Java SE 6 or above
* Scala 2.11.x

How To Use
----------

**First**, specify Jdub as a dependency:

```xml
<dependencies>
  <dependency>
    <groupId>com.simple</groupId>
    <artifactId>jdub_${scala.major.version}</artifactId>
    <version>0.9.0</version>
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
// Query returning an optional single result.
case class GetAge(name: String) extends FlatSingleRowQuery[Int] {

  val sql = trim("""
      SELECT age /* Use C-style comments in trimmed queries */
      FROM people
      WHERE name = ?
      """)

  val values = Seq(name)

  def flatMap(row: Row) = {
    // Returns Option[Int]
    row.int(0) // 0 gets the first column
  }

}

val age = db(GetAge("Old Guy")).getOrElse(-1) // 402
```

```scala
// Query returning a Person object for each row.
case object GetPeople extends CollectionQuery[Seq, Person] {

  val sql = trim("""
      SELECT name, email, age
      FROM people
      """)

  val values = Seq()

  def map(row: Row) = {
    val name = row.string("name").get
    val email = row.string("email").getOrElse("")
    val age = row.int("age").getOrElse(0)
    Person(name, email, age)
  }

}

val person = db(GetPeople).head // Person(Coda Hale,chale@example.com,29)
```


**Fourth**, execute some statements:

```scala
case class UpdateEmail(name: String, newEmail: String) extends Statement {
  val sql = trim("""
      UPDATE people
      SET email = ?
      WHERE name = ?
      """)
  val values = Seq(newEmail, name)
}

// Execute the statement.
db.execute(UpdateEmail("Old Guy", "oldguy@example.com"))

// Or return the number of rows updated.
val count = db.update(UpdateEmail("Old Guy", "oldguy@example.com")) // 1
```

**Fifth**, read up on all the details in the [Jdub tour](tour.md).

License
-------

Copyright (c) 2011-2012 Coda Hale
Copyright (c) 2012-2013 Simple Finance Technology Corp. All rights reserved.

Published under The MIT License, see [LICENSE.md](LICENSE.md)
