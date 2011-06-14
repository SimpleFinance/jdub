package com.codahale.jdub

import java.sql

class Transaction(conn: sql.Connection) extends Connection {
  protected def openConnection() = conn

  protected def closeConnection(conn: sql.Connection) {
    // don't actually close it!
  }

  /**
   * Roll back the transaction.
   */
  def rollback() {
    conn.rollback()
  }
}
