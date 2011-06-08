package com.codahale.jdub

import java.sql.ResultSet

private class ValueIterator(rs: ResultSet) extends Iterator[IndexedSeq[Value]] {
  private val values = (1 to rs.getMetaData.getColumnCount).map { new Value(rs, _) }.toIndexedSeq
  private var advanced, canAdvance = false

  def hasNext = {
    if (!advanced) {
      advanced = true
      canAdvance = rs.next()
    }
    canAdvance
  }

  def next() = if (hasNext) {
    advanced = false
    values
  } else Iterator.empty.next()
}
