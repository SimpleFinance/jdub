package com.simple.jdub

import java.sql.Connection
import scala.collection.mutable.ListBuffer

class Transaction(connection: Connection) extends Queryable {
  private[this] var rolledback = false

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
    logger.debug("Rolling back transaction")
    connection.rollback()
    rolledback = true
    onRollback.foreach(_())
  }

  private[jdub] def commit() {
    if (!rolledback) {
      logger.debug("Committing transaction")
      connection.commit()
      onCommit.foreach(_())
    }
  }

  private[jdub] def close() {
    logger.debug("Closing transaction")
    connection.close()
    onClose.foreach(_())
  }

  def transaction[A](f: Transaction => A): A = f(this)

  var onCommit: ListBuffer[() => Unit] = ListBuffer.empty[() => Unit]
  var onClose: ListBuffer[() => Unit] = ListBuffer.empty[() => Unit]
  var onRollback: ListBuffer[() => Unit] = ListBuffer.empty[() => Unit]
}
