package com.codahale.jdub

import com.yammer.metrics.Instrumented
import java.sql.ResultSet

trait RawQuery[A] extends Instrumented {
  private[jdub] val timer = metrics.timer("query")

  def sql: String

  def values: Seq[Any]

  def handle(results: ResultSet): A

  def apply(db: Database): A = db(this)

  protected def trim(sql: String) = sql.replaceAll("""[\s]+""", " ").trim
}
