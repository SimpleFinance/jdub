package com.simple.jdub

trait SqlBase extends Instrumented {
  /**
   * Collapse all strings of whitespace into a single space.
   *
   * This function is incompatible with SQL standard comments (-- comment).
   * Use C-style comments instead.
   *
   * @param sql the string to trim
   */
  protected def trim(sql: String) = sql.replaceAll("""[\s]+""", " ").trim
}
