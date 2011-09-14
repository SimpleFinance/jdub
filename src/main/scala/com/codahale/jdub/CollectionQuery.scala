package com.codahale.jdub

import collection.generic.CanBuildFrom

/**
 * A query class which maps each row of a result set to a value and aggregates
 * the values using a generic companion.
 */
abstract class CollectionQuery[CC[A] <: Traversable[A], A](implicit bf: CanBuildFrom[CC[A], A,  CC[A]]) extends Query[CC[A]] {
  def map(row: Row): A
  
  final def reduce(rows: Iterator[Row]): CC[A] = {
    val builder = bf()
    for (row <- rows) {
      builder += map(row)
    }
    builder.result()
  }
}

/**
 * A query class which flat-maps each row of a result set to a value and
 * collects the values using a generic companion.
 */
abstract class FlatCollectionQuery[CC[A] <: Traversable[A], A](implicit bf: CanBuildFrom[CC[A], A, CC[A]]) extends Query[CC[A]] {
  def flatMap(row: Row): Option[A]

  final def reduce(rows: Iterator[Row]) = {
    val builder = bf()
    for (row <- rows;
         value <- flatMap(row)) {
      builder += value
    }
    builder.result()
  }
}
