package com.simple.jdub

trait SqlBase extends Instrumented {
  protected def trim(sql: String) = sql.replaceAll("""[\s]+""", " ").trim
}
