/**
 * © 2012 Simple Finance Technology Corp. All rights reserved.
 * Author: Ian Eure <ieure@simple.com>
 */

package com.simple.jdub

import java.sql.Connection

import com.yammer.metrics.scala.Instrumented
import grizzled.slf4j.Logging

trait Queryable extends Logging with Instrumented {
  import Utils._

  /**
   * Performs a query and returns the results.
   */
  def apply[A](connection: Connection, query: RawQuery[A]): A = {
    query.timer.time {
      logger.debug("%s with %s".format(query.sql,
       query.values.mkString("(", ", ", ")")))
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
    }
  }

  /**
   * Executes an update, insert, delete, or DDL statement.
   */
  def execute(connection: Connection, statement: Statement): Int = {
    statement.timer.time {
      logger.debug("%s with %s".format(statement.sql,
        statement.values.mkString("(", ", ", ")")))
      val stmt = connection.prepareStatement(prependComment(statement, statement.sql))
      try {
        prepare(stmt, statement.values)
        stmt.executeUpdate()
      } finally {
        stmt.close()
      }
    }
  }

  def execute(statement: Statement): Int
  def apply[A](query: RawQuery[A]): A
  def transaction[A](f: Transaction => A): A

  /**
   * Performs a query and returns the results.
   */
  def query[A](query: RawQuery[A]): A = apply(query)

  /**
   * Executes an update statement.
   */
  def update(statement: Statement): Int = execute(statement)

  /**
   * Executes an insert statement.
   */
  def insert(statement: Statement): Int = execute(statement)

  /**
   * Executes a delete statement.
   */
  def delete(statement: Statement): Int = execute(statement)
}
