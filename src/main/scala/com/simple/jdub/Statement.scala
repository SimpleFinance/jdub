package com.simple.jdub

trait Statement extends SqlBase {
  private[jdub] val timer = metrics.timer("execute")

  def sql: String

  def values: Seq[Any]
}
