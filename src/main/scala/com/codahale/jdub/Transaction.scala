package com.codahale.jdub

import java.sql.Connection

class Transaction(connection: Connection) extends Queryable {

  /**
   * Performs a query and returns the results.
   */
  override def apply[A](query: RawQuery[A]): A = apply(connection, query)

  /**
   * Executes an update, insert, delete, or DDL statement.
   */
  def execute(statement: Statement) = execute(connection, statement)

  /**
   * Roll back the transaction.
   */
  def rollback() {
    connection.rollback()
  }
}
