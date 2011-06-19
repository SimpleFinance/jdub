package com.codahale.jdub

import scala.collection.generic.GenericCompanion

/**
 * A query class which maps each row of a result set to a value and aggregates
 * the values using a generic companion.
 */
abstract class CollectionQuery[CC[A] <: Traversable[A], A](companion: GenericCompanion[CC]) extends Query[CC[A]] {
  def map(row: Row): A
  
  final def reduce(results: Iterator[Row]): CC[A] = {
    val builder = companion.newBuilder[A]
    for (row <- results) {
      builder += map(row)
    }
    builder.result()
  }
}

/**
 * A query class which flat-maps each row of a result set to a value and
 * collects the values using a generic companion.
 */
abstract class FlatCollectionQuery[CC[A] <: Traversable[A], A](companion: GenericCompanion[CC]) extends Query[CC[A]] {
  def flatMap(row: Row): Option[A]

  final def reduce(results: Iterator[Row]) = {
    val builder = companion.newBuilder[A]
    for (row <- results;
         value <- flatMap(row)) {
      builder += value
    }
    builder.result()
  }
}
