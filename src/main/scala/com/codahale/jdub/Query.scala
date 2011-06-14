package com.codahale.jdub

import java.sql.ResultSet

trait Query[A] extends RawQuery[A] {
  def handle(results: ResultSet) = reduce(new RowIterator(results))

  def reduce(results: Iterator[Row]): A
}
