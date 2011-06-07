package com.codahale.jdub

import java.sql.ResultSet

trait Query[A] extends RawQuery[A] {
  def handle(results: ResultSet) = reduce(new ValueIterator(results))

  def reduce(results: Iterator[IndexedSeq[Value]]): A
}
