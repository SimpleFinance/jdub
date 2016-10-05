/**
  * © 2016 Simple Finance Technology Corp. All rights reserved.
  */
package com.simple.jdub.tests

import com.simple.jdub.CollectionQuery
import com.simple.jdub.FlatCollectionQuery
import com.simple.jdub.FlatSingleRowQuery
import com.simple.jdub.Row
import com.simple.jdub.Statement

case class Person(name: String, email: String, age: Int)

case class SQL(sql: String, values: Seq[Any] = Seq()) extends Statement

case class AgesQuery() extends FlatCollectionQuery[Set, Int] {
  val sql = "SELECT age FROM people"

  val values = Seq()

  def flatMap(row: Row) = row.int(0)
}

case class AgeQuery(name: String) extends FlatSingleRowQuery[Int] {
  val sql = trim("SELECT age FROM people WHERE name = ?")

  val values = Seq(name)

  def flatMap(row: Row) = row.int(0)
}

case class AgeNullQuery(name: Option[String]) extends FlatSingleRowQuery[Int] {
  val sql = trim("SELECT age FROM people WHERE name = ?")

  val values = Seq(name)

  def flatMap(row: Row) = row.int(0)
}


case class EmailQuery() extends CollectionQuery[Vector, Option[String]] {
  val sql = trim("SELECT email FROM people")

  val values = Seq()

  def map(row: Row) = row.string("email")
}

case class MapsQuery() extends CollectionQuery[Seq, Map[String, Any]] {
  val sql = trim("SELECT name, email, age FROM people")

  val values = Seq()

  def map(row: Row) = row.toMap()
}

case class NamesArrayQuery() extends CollectionQuery[Seq, Option[Array[String]]] {
  val sql = trim("SELECT ARRAY_AGG(name) AS names FROM people")

  val values = Seq()

  def map(row: Row) = row.array[String]("names")
}

case class AgesArrayQuery() extends CollectionQuery[Seq, Option[Array[Int]]] {
  val sql = trim("SELECT ARRAY_AGG(age) AS ages FROM people")

  val values = Seq()

  def map(row: Row) = row.array[Int]("ages")
}

case class EmptyArrayQuery() extends CollectionQuery[Seq, Option[Array[Int]]] {
  val sql = trim("SELECT ARRAY_AGG(age) AS ages FROM people WHERE name = 'not a name'")

  val values = Seq()

  def map(row: Row) = row.array[Int]("ages")
}

