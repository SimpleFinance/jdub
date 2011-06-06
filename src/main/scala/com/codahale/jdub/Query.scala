package com.codahale.jdub

import java.sql.ResultSet

trait Query[A] extends RawQuery[A] {
  def handle(results: ResultSet) = reduce(new ValueIterator(results).toStream)

  def reduce(results: Stream[IndexedSeq[Value]]): A
}
