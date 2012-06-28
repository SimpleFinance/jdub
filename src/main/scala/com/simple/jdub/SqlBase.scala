package com.simple.jdub

trait SqlBase {
  protected def trim(sql: String) = sql.replaceAll("""[\s]+""", " ").trim
}
