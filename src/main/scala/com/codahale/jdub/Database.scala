package com.codahale.jdub

import com.yammer.metrics.scala.Instrumented
import grizzled.slf4j.Logging
import java.util.Properties
import javax.sql.DataSource
import org.apache.tomcat.dbcp.dbcp.{PoolingDataSource, PoolableConnectionFactory, DriverManagerConnectionFactory}
import org.apache.tomcat.dbcp.pool.impl.GenericObjectPool

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
              name: String = null,
              maxWaitForConnectionInMS: Long = 8,
              maxSize: Int = 8,
              minSize: Int = 0,
              checkConnectionWhileIdle: Boolean = true,
              checkConnectionHealthWhenIdleForMS: Long = 10000,
              closeConnectionIfIdleForMS: Long = 1000L * 60L * 30L,
              healthCheckQuery: String = Utils.prependComment(PingQuery, PingQuery.sql),
              jdbcProperties: Map[String, String] = Map.empty) = {
    val properties = new Properties
    for ((k, v) <- jdbcProperties) {
      properties.setProperty(k, v)
    }
    properties.setProperty("user", username)
    properties.setProperty("password", password)

    val factory = new DriverManagerConnectionFactory(url, properties)
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
    new Database(new PoolingDataSource(pool), pool, name)
  }


}

/**
 * A set of pooled connections to a database.
 */
class Database protected(source: DataSource, pool: GenericObjectPool, name: String)
  extends Logging with Instrumented {

  metrics.gauge("active-connections", name) { pool.getNumActive }
  metrics.gauge("idle-connections", name)   { pool.getNumIdle }
  metrics.gauge("total-connections", name)  { pool.getNumIdle + pool.getNumActive }
  private val poolWait = metrics.timer("pool-wait")

  import Utils._

  /**
   * Opens a transaction which is committed after `f` is called. If `f` throws
   * an exception, the transaction is rolled back.
   */
  def transaction[A](f: Transaction => A): A = {
    val connection = poolWait.time { source.getConnection }
    connection.setAutoCommit(false)
    val txn = new Transaction(connection)
    try {
      debug("Starting transaction")
      val result = f(txn)
      debug("Committing transaction")
      connection.commit()
      result
    } catch {
      case e => {
        error("Exception thrown in transaction scope; aborting transaction", e)
        connection.rollback()
        throw e
      }
    } finally {
      connection.close()
    }
  }

  /**
   * Returns {@code true} if we can talk to the database.
   */
  def ping() = apply(PingQuery)

  /**
   * Performs a query and returns the results.
   */
  def apply[A](query: RawQuery[A]): A = {
    val connection = poolWait.time { source.getConnection }
    query.timer.time {
      try {
        if (isDebugEnabled) {
          debug("%s with %s", query.sql, query.values.mkString("(", ", ", ")"))
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
    val connection = poolWait.time { source.getConnection }
    statement.timer.time {
      try {
        if (isDebugEnabled) {
          debug("%s with %s", statement.sql, statement.values.mkString("(", ", ", ")"))
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
}
