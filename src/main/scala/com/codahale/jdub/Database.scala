package com.codahale.jdub

import java.sql.{Types, PreparedStatement}
import javax.sql.DataSource
import com.codahale.logula.Logging
import com.yammer.metrics.Instrumented
import org.apache.tomcat.dbcp.pool.impl.GenericObjectPool
import org.apache.tomcat.dbcp.dbcp.{PoolingDataSource, PoolableConnectionFactory, DriverManagerConnectionFactory}
import scala.annotation.tailrec

object Database {
  /**
   * Create a pool of connections to the given database.
   *
   * @param url the JDBC url
   * @param username the database user
   * @param password the database password
   */
  def connect(url: String,
              username: String,
              password: String,
              maxWaitForConnectionInMS: Long = 8,
              maxSize: Int = 8,
              minSize: Int = 0,
              checkConnectionWhileIdle: Boolean = true,
              checkConnectionHealthWhenIdleForMS: Long = 10000,
              closeConnectionIfIdleForMS: Long = 1000L * 60L * 30L,
              healthCheckQuery: String = prependComment(PingQuery, PingQuery.sql)) = {
    val factory = new DriverManagerConnectionFactory(url, username, password)
    val pool = new GenericObjectPool(null)
    pool.setMaxWait(maxWaitForConnectionInMS)
    pool.setMaxIdle(maxSize)
    pool.setMaxActive(maxSize)
    pool.setMinIdle(minSize)
    pool.setTestWhileIdle(checkConnectionWhileIdle)
    pool.setTimeBetweenEvictionRunsMillis(checkConnectionHealthWhenIdleForMS)
    pool.setMinEvictableIdleTimeMillis(closeConnectionIfIdleForMS)

    // this constructor sets itself as the factory of the pool
    new PoolableConnectionFactory(
      factory, pool, null, healthCheckQuery, false, true
    )
    new Database(new PoolingDataSource(pool), pool)
  }

  private def prependComment(obj: Object, sql: String) =
    "/* %s */ %s".format(obj.getClass.getSimpleName.replace("$", ""), sql)
}

/**
 * A set of pooled connections to a database.
 */
class Database protected(source: DataSource, pool: GenericObjectPool) extends Instrumented with Logging {
  import Database._

  /**
   * Returns {@code true} if we can talk to the database.
   */
  def ping() = apply(PingQuery)

  /**
   * Performs a query and returns the results.
   */
  def apply[A](query: RawQuery[A]): A = {
    query.timer.time {
      val connection = source.getConnection
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
        connection.close()
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
      val connection = source.getConnection
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
        connection.close()
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

  /**
   * Closes all connections to the database.
   */
  def close() {
    pool.close()
  }

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
