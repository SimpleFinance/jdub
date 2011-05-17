package com.codahale.jdub

import javax.sql.DataSource
import com.codahale.logula.Logging
import com.yammer.metrics.Instrumented
import org.apache.tomcat.dbcp.pool.impl.GenericObjectPool
import org.apache.tomcat.dbcp.dbcp.{PoolingDataSource, PoolableConnectionFactory, DriverManagerConnectionFactory}
import java.sql.{Types, PreparedStatement}

object Database {
  import GenericObjectPool._

  /**
   * Create a pool of connections to the given database.
   */
  def connect(url: String,
              username: String,
              password: String,
              maxWaitForConnectionInMS: Long = DEFAULT_MAX_WAIT,
              maxSize: Int = DEFAULT_MAX_ACTIVE,
              minSize: Int = DEFAULT_MIN_IDLE,
              checkConnectionBeforeQuery: Boolean = DEFAULT_TEST_ON_BORROW,
              checkConnectionHealthWhenIdleForMS: Long = DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,
              closeConnectionIfIdleForMS: Long = DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,
              healthCheckQuery: String = prependComment(PingQuery, PingQuery.sql)) = {
    val c = new GenericObjectPool.Config
    c.maxWait = maxWaitForConnectionInMS
    c.maxIdle = maxSize
    c.maxActive = maxSize
    c.minIdle = minSize
    c.testOnBorrow = checkConnectionBeforeQuery
    c.timeBetweenEvictionRunsMillis = checkConnectionHealthWhenIdleForMS
    c.minEvictableIdleTimeMillis = closeConnectionIfIdleForMS

    val factory = new DriverManagerConnectionFactory(url, username, password)
    val pool = new GenericObjectPool(null, c)
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
 * A database.
 */
class Database(source: DataSource, pool: GenericObjectPool) extends Instrumented with Logging {
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

  private def prepare(stmt: PreparedStatement, values: Seq[Any]) {
    for ((v, i) <- values.zipWithIndex) {
      if (v == null) {
        stmt.setNull(i + 1, Types.NULL)
      } else {
        stmt.setObject(i + 1, v.asInstanceOf[AnyRef])
      }
    }
  }
}
