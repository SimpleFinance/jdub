package com.codahale.jdub

import java.sql.ResultSet

private class ValueIterator(rs: ResultSet) extends Iterator[IndexedSeq[Value]] {
  private val values = (1 to rs.getMetaData.getColumnCount).map { new Value(rs, _) }.toIndexedSeq

  def hasNext = !rs.isLast

  def next() = if (hasNext) {
    rs.next()
    values
  } else Iterator.empty.next()
}
