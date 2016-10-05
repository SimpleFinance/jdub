/**
  * © 2016 Simple Finance Technology Corp. All rights reserved.
  */
package com.simple.jdub.tests

import com.simple.jdub.CollectionQuery
import com.simple.jdub.FlatSingleRowQuery
import com.simple.jdub.Row
import com.simple.jdub.Statement

class DocumentationExampleSpec extends JdubSpec {

  // The code examples in README, etc. should be tested here.
  // Copy and paste the snippets between "```scala" and "```" comments.

  test("example of SingleRowQuery") {
    //```scala
    // Query returning an optional single result.
    case class GetAge(name: String) extends FlatSingleRowQuery[Int] {

      val sql = trim(
        """
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
    //```
    db(GetAge("Old Guy")).must(be(Some(402)))
  }

  test("example of CollectionQuery") {
    //```scala
    // Query returning a Person object for each row.
    case object GetPeople extends CollectionQuery[Seq, Person] {

      val sql = trim(
        """
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
    //```
    db(GetPeople).must(contain(Person("Coda Hale", "chale@example.com", 29)))
  }

  test("example of Statement") {
    //```scala
    case class UpdateEmail(name: String,
                           newEmail: String) extends Statement {
      val sql = trim(
        """
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
    //```
    count.must(be(1))
  }

}
