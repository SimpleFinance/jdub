package com.codahale.jdub

import java.sql.PreparedStatement
import javax.sql.DataSource
import com.codahale.logula.Logging
import com.yammer.metrics.Instrumented
import org.apache.tomcat.dbcp.pool.impl.GenericObjectPool
import org.apache.tomcat.dbcp.dbcp.{PoolingDataSource, PoolableConnectionFactory, DriverManagerConnectionFactory}

object Database {
  import GenericObjectPool._

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
    println(healthCheckQuery)

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
    val poolableConnectionFactory = new PoolableConnectionFactory(
      factory, pool, null, healthCheckQuery, false, true
    )
    new Database()(new PoolingDataSource(pool))
  }

  private def prependComment(obj: Object, sql: String) =
    "/* %s */ %s".format(obj.getClass.getSimpleName.replace("$", ""), sql)

  def poop = prependComment(PingQuery, PingQuery.sql)
}

class Database(implicit source: DataSource) extends Instrumented with Logging {
  import Database._

  def ping() = apply(PingQuery)

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
          stmt.execute()
        } finally {
          stmt.close()
        }
      } finally {
        connection.close()
      }
    }
  }

  def update(statement: Statement) = {
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

  private def prepare(stmt: PreparedStatement, values: Seq[Any]) {
    for ((v, i) <- values.zipWithIndex) {
      stmt.setObject(i + 1, v.asInstanceOf[AnyRef])
    }
  }
}
