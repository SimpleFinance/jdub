package com.simple.jdub

trait Statement extends SqlBase {
  def sql: String

  def values: Seq[Any]
}
