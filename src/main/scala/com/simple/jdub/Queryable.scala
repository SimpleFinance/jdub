/**
 * © 2012, 2014 Simple Finance Technology Corp. All rights reserved.
 * Author: Ian Eure <ieure@simple.com>
 */

package com.simple.jdub

import com.simple.jdub.Database.Primary
import com.simple.jdub.Database.Role

import java.sql.Connection
import grizzled.slf4j.Logging

trait Queryable[R <: Role] extends Logging {
  import Utils._

  /**
   * Performs a query and returns the results.
   */
  def apply[A](connection: Connection, query: RawQuery[A]): A = {
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

  /**
   * Executes an update, insert, delete, or DDL statement.
   */
  def execute(connection: Connection, statement: Statement)(implicit ev: R =:= Primary): Int = {
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

  def execute(statement: Statement)(implicit ev: R =:= Primary): Int
  def apply[A](query: RawQuery[A]): A
  def transaction[A](f: Transaction[R] => A)(implicit ev: R =:= Primary): A

  /**
   * Performs a query and returns the results.
   */
  def query[A](query: RawQuery[A]): A = apply(query)

  /**
   * Executes an update statement.
   */
  def update(statement: Statement)(implicit ev: R =:= Primary): Int = execute(statement)

  /**
   * Executes an insert statement.
   */
  def insert(statement: Statement)(implicit ev: R =:= Primary): Int = execute(statement)

  /**
   * Executes a delete statement.
   */
  def delete(statement: Statement)(implicit ev: R =:= Primary): Int = execute(statement)
}
