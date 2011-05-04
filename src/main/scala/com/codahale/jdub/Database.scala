package com.codahale.jdub

import javax.sql.DataSource
import com.codahale.logula.Logging
import com.yammer.metrics.Instrumented
import java.sql.{Connection, PreparedStatement}
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
              healthCheckQuery: String = "/* Jdub Healthcheck */ SELECT 1") = {
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
}

class Database(implicit source: DataSource) extends Instrumented with Logging {
  def apply[A](query: RawQuery[A]): A = {
    query.timer.time {
      withConnection {connection =>
        if (log.isDebugEnabled) {
          log.debug("%s with %s", query.sql, query.values.mkString("(", ", ", ")"))
        }

        val stmt = connection.prepareStatement(query.sql)
        prepare(stmt, query.values)

        val results = stmt.executeQuery()
        try {
          query.handle(results)
        } finally {
          results.close()
        }
      }
    }
  }

  def execute(statement: Statement) = {
    statement.timer.time {
      withConnection {connection =>
        if (log.isDebugEnabled) {
          log.debug("%s with %s", statement.sql, statement.values.mkString("(", ", ", ")"))
        }

        val stmt = connection.prepareStatement(statement.sql)
        prepare(stmt, statement.values)

        stmt.execute()
      }
    }
  }

  def update(statement: Statement) = {
    statement.timer.time {
      withConnection {connection =>
        if (log.isDebugEnabled) {
          log.debug("%s with %s", statement.sql, statement.values.mkString("(", ", ", ")"))
        }

        val stmt = connection.prepareStatement(statement.sql)
        prepare(stmt, statement.values)

        stmt.executeUpdate()
      }
    }
  }

  private def withConnection[A](f: Connection => A) = {
    val connection = source.getConnection
    try {
      f(connection)
    } finally {
      connection.close()
    }
  }

  private def prepare(stmt: PreparedStatement, values: Seq[Any]) {
    for ((v, i) <- values.zipWithIndex) {
      stmt.setObject(i + 1, v.asInstanceOf[AnyRef])
    }
  }
}
