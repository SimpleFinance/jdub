package com.simple.jdub

import com.simple.jdub.Database.Role

import java.sql.ResultSet

trait RawQuery[A] extends SqlBase {
  def sql: String

  def values: Seq[Any]

  def handle(results: ResultSet): A

  def apply(db: Database[Role]): A = db(this)
}
