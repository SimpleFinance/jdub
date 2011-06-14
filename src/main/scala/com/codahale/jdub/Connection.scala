package com.codahale.jdub

import java.sql
import com.codahale.logula.Logging
import scala.annotation.tailrec
import java.sql.{Types, PreparedStatement}

object Connection {
  private[jdub] def prependComment(obj: Object, sql: String) =
    "/* %s */ %s".format(obj.getClass.getSimpleName.replace("$", ""), sql)
}

trait Connection extends Logging {
  import Connection._

  protected def openConnection(): sql.Connection
  protected def closeConnection(conn: sql.Connection)

  /**
   * Performs a query and returns the results.
   */
  def apply[A](query: RawQuery[A]): A = {
    query.timer.time {
      val connection = openConnection()
      try {
        if (log.isDebugEnabled) {
          log.debug("%s with %s", query.sql, query.values.mkString("(", ", ", ")"))
        }
        val stmt = connection.prepareStatement(prependComment(query, query.sql))
        try {
          prepare(stmt, query.values)
          val results = stmt.executeQuery()
          try {
            query.handle(results)
          } finally {
            results.close()
          }
        } finally {
          stmt.close()
        }
      } finally {
        closeConnection(connection)
      }
    }
  }

  /**
   * Performs a query and returns the results.
   */
  def query[A](query: RawQuery[A]): A = apply(query)

  /**
   * Executes an update, insert, delete, or DDL statement.
   */
  def execute(statement: Statement) = {
    statement.timer.time {
      val connection = openConnection()
      try {
        if (log.isDebugEnabled) {
          log.debug("%s with %s", statement.sql, statement.values.mkString("(", ", ", ")"))
        }
        val stmt = connection.prepareStatement(prependComment(statement, statement.sql))
        try {
          prepare(stmt, statement.values)
          stmt.executeUpdate()
        } finally {
          stmt.close()
        }
      } finally {
        closeConnection(connection)
      }
    }
  }

  /**
   * Executes an update statement.
   */
  def update(statement: Statement) = execute(statement)

  /**
   * Executes an insert statement.
   */
  def insert(statement: Statement) = execute(statement)

  /**
   * Executes a delete statement.
   */
  def delete(statement: Statement) = execute(statement)

  @tailrec
  private def prepare(stmt: PreparedStatement, values: Seq[Any], index: Int = 1) {
    if (!values.isEmpty) {
      val v = values.head
      if (v == null) {
        stmt.setNull(index, Types.NULL)
      } else {
        stmt.setObject(index, v.asInstanceOf[AnyRef])
      }
      prepare(stmt, values.tail, index + 1)
    }
  }
}
