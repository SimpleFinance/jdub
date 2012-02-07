package com.codahale.jdub

import com.yammer.metrics.scala.Instrumented
import java.sql.ResultSet

trait RawQuery[A] extends SqlBase with Instrumented {
  private[jdub] val timer = metrics.timer("query")

  def sql: String

  def values: Seq[Any]

  def handle(results: ResultSet): A

  def apply(db: Database): A = db(this)
}
