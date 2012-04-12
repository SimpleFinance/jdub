package com.codahale.jdub

import java.sql.Connection

class Transaction(connection: Connection) extends Queryable {

  /**
   * Performs a query and returns the results.
   */
  override def apply[A](query: RawQuery[A]): A = {
    try {
      apply(connection, query)
    } finally {
      connection.close()
    }
  }

  /**
   * Executes an update, insert, delete, or DDL statement.
   */
  def execute(statement: Statement) = {
    try {
      execute(connection, statement)
    } finally {
      connection.close()
    }
  }

  /**
   * Roll back the transaction.
   */
  def rollback() {
    connection.rollback()
  }
}
