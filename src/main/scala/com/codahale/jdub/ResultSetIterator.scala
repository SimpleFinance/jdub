package com.codahale.jdub

import java.sql.ResultSet

private class ResultSetIterator(rs: ResultSet) extends Iterator[IndexedSeq[Cell]] {
  private val cells = (1 to rs.getMetaData.getColumnCount).map { new Cell(rs, _) }.toIndexedSeq

  def hasNext = !rs.isLast

  def next() = if (hasNext) {
    rs.next()
    cells
  } else Iterator.empty.next()
}
