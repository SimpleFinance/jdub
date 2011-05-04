package com.codahale.jdub

import java.sql.ResultSet
import com.yammer.metrics.Instrumented

trait Query[A] extends Instrumented {
  private[jdub] val timer = metrics.timer("query")

  def sql: String

  def values: Seq[Any]

  def handle(results: ResultSet): A

  protected def trim(sql: String) = sql.replaceAll("""[\s]+""", " ").trim
}
