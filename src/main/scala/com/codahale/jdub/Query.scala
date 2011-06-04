package com.codahale.jdub

import java.sql.ResultSet

trait Query[A] extends RawQuery[A] {
  def handle(rs: ResultSet) = {
    val width = rs.getMetaData.getColumnCount
    var results = Vector.empty[Vector[Any]]
    while (rs.next()) {
      var row = Vector.empty[Any]
      for (i <- 1 to width) {
        row = row :+ rs.getObject(i)
      }
      results = results :+ row
    }
    reduce(results)
  }

  def reduce(results: Vector[Vector[Any]]): A
}
