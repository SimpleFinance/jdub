package com.codahale.jdub

trait SqlBase {
  protected def trim(sql: String) = sql.replaceAll("""[\s]+""", " ").trim
}
