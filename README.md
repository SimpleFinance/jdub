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
    <version>${jdub.version}</version>
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

You can also use the sql string interpolator:

```scala
class PersonQueries(val database: Database) {
  // Query returning an optional single result.
  def getAge(name: String): Option[Int] = database {
    sql"""
      SELECT age
      FROM people
      WHERE name = ${name}
    """.map { row =>
      row.int("age")
    }
  }.headOption
}

val personQueries = new PersonQueries(database)
val age = personQueries.getAge("Old Guy").getOrElse(-1) // 402
```

```scala
object PersonQueries {
  def mapper(row: Row): String = {
    Person(
      name = row.string("name").get
      email = row.string("email").getOrElse("")
      age = row.int("age").getOrElse(0)
    )
  }
}

class PersonQueries(val database: Database) {
  // Query returning Person objects for each row.
  def getPeople(): Seq[Person] = database {
    sql"""
      SELECT name, email, age
      FROM people
    """.map(PersonQueries.mapper)
  }
}

val personQueries = new PersonQueries(database)
val people = personQueries.getPeople.head // Person(Coda Hale,chale@example.com,29)
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

You can also use the sql string interpolator:

```scala
object PersonQueries {
  def mapper(row: Row): String = {
    Person(
      name = row.string("name").get
      email = row.string("email").getOrElse("")
      age = row.int("age").getOrElse(0)
    )
  }
}

class PersonQueries(val database: Database) {
  // Update a Person's email
  def updateEmail(name: String, newEmail: String): Option[Person] = database {
    sql"""
      UPDATE people
      SET email = ${newEmail}
      WHERE name = ${name}
      RETURNING people.*
    """.map(PersonQueries.mapper)
  }.headOption
}


val personQueries = new PersonQueries(database)
val updatedPerson = personQueries.updateEmail("Old Guy", "oldguy@example.com")
```

**Fifth**, read up on all the details in the [Jdub tour](tour.md).

License
-------

Copyright (c) 2011-2012 Coda Hale
Copyright (c) 2012-2016 Simple Finance Technology Corp. All rights reserved.

Published under The MIT License, see [LICENSE.md](LICENSE.md)
