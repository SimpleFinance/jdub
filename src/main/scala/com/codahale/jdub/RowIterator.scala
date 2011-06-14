package com.codahale.jdub

import java.sql.ResultSet

class RowIterator(rs: ResultSet) extends Iterator[Row] {
  private val row = new Row(rs)
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
    row
  } else Iterator.empty.next()
}
