package com.codahale.jdub

import com.yammer.metrics.Instrumented

trait Statement extends Instrumented {
  private[jdub] val timer = metrics.timer("execute")

  def sql: String

  def values: Seq[Any]
}
