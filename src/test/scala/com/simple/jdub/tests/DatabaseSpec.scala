package com.simple.jdub.tests

import com.simple.jdub._

class DatabaseSpec extends JdubSpec {

  test("returns sets of results") {
    db(AgesQuery()).must(be(Set(29, 30, 402)))
  }

  test("returns sets of results with null values") {
    db(EmailQuery()).must(be(Vector(Some("chale@example.com"), Some("kgale@example.com"), None)))
  }

  test("returns single rows") {
    db(AgeQuery("Coda Hale")).must(be(Some(29)))
  }

  test("returns empty sets") {
    db(AgeQuery("Captain Fuzzypants McFrankface")).must(be(None))
  }

  test("converts None to null correctly") {
    db(AgeNullQuery(None)).must(be(None))
    db(AgeNullQuery(null)).must(be(None))
  }

  test("returns a collection of maps") {
    db(MapsQuery()).must(be(Seq(Map("NAME" -> "Coda Hale",
                                    "EMAIL" -> "chale@example.com",
                                    "AGE" -> 29),
                                Map("NAME" -> "Kris Gale",
                                    "EMAIL" -> "kgale@example.com",
                                    "AGE" -> 30),
                                Map("NAME" -> "Old Guy",
                                    "EMAIL" -> null,
                                    "AGE" -> 402))))
  }

  test("returns an array of strings") {
    db(NamesArrayQuery())
      .map(_.map(_.toSeq)) // arrays compare by reference, seqs by value
      .must(be(Seq(Some(Seq("Coda Hale",
                            "Kris Gale",
                            "Old Guy")))))
  }

  test("returns an array of integers") {
    db(AgesArrayQuery())
      .map(_.map(_.toSeq)) // arrays compare by reference, seqs by value
      .must(be(Seq(Some(Seq(29,
                            30,
                            402)))))
  }

  test("returns an empty array") {
    db(EmptyArrayQuery())
      .map(_.map(_.toSeq)) // arrays compare by reference, seqs by value
      .must(be(Seq(None)))
  }
}
