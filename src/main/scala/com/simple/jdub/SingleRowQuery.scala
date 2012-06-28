package com.simple.jdub

/**
 * A query class which maps the first row of a result set to a value and throws
 * an exception if the row doesn't exit.
 */
trait SingleRowQuery[A] extends Query[A] {
  def map(row: Row): A

  final def reduce(rows: Iterator[Row]) = rows.map(map).next()
}

/**
 * A query class which maps the first row of a result set to an optional value,
 * returning None if the result set has no rows.
 */
trait FlatSingleRowQuery[A] extends Query[Option[A]] {
  def flatMap(row: Row): Option[A]

  final def reduce(rows: Iterator[Row]) = if (rows.hasNext) {
    flatMap(rows.next())
  } else None
}
