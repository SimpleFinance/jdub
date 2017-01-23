package com.simple.jdub

import com.simple.jdub.Database.Primary
import com.simple.jdub.Database.Role

import java.sql.{Connection, Savepoint}
import scala.collection.mutable.ListBuffer

class Transaction[R <: Role](val connection: Connection) extends Queryable[R] {
  private[this] var rolledback = false

  /**
   * Performs a query and returns the results.
   */
  override def apply[A](query: RawQuery[A]): A = apply(connection, query)

  /**
   * Executes an update, insert, delete, or DDL statement.
   */
  def execute(statement: Statement)(implicit ev: R =:= Primary) = execute(connection, statement)

  /**
   * Roll back the transaction.
   */
  def rollback()(implicit ev: R =:= Primary) {
    logger.debug("Rolling back transaction")
    connection.rollback()
    rolledback = true
    onRollback.foreach(_())
  }

  /**
   * Roll back the transaction to a savepoint.
   */
  def rollback(savepoint: Savepoint)(implicit ev: R =:= Primary) {
    logger.debug("Rolling back to savepoint")
    connection.rollback(savepoint)
  }

  /**
   * Release a transaction from a savepoint.
   */
  def release(savepoint: Savepoint)(implicit ev: R =:= Primary) {
    logger.debug("Releasing savepoint")
    connection.releaseSavepoint(savepoint)
  }

  /**
   * Set an unnamed savepoint.
   */
  def savepoint()(implicit ev: R =:= Primary): Savepoint = {
    logger.debug("Setting unnamed savepoint")
    connection.setSavepoint()
  }

  /**
   * Set a named savepoint.
   */
  def savepoint(name: String)(implicit ev: R =:= Primary): Savepoint = {
    logger.debug("Setting savepoint")
    connection.setSavepoint(name)
  }

  private[jdub] def commit()(implicit ev: R =:= Primary) {
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

  def transaction[A](f: Transaction[R] => A)(implicit ev: R =:= Primary): A = f(this)

  var onCommit: ListBuffer[() => Unit] = ListBuffer.empty[() => Unit]
  var onClose: ListBuffer[() => Unit] = ListBuffer.empty[() => Unit]
  var onRollback: ListBuffer[() => Unit] = ListBuffer.empty[() => Unit]
}
