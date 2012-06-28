package com.simple.jdub

import com.yammer.metrics.scala.Instrumented

trait Statement extends SqlBase with Instrumented {
  private[jdub] val timer = metrics.timer("execute")

  def sql: String

  def values: Seq[Any]
}
