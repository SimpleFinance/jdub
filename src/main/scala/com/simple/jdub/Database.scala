package com.simple.jdub

import javax.sql.DataSource
import org.apache.tomcat.dbcp.pool.impl.GenericObjectPool
import org.apache.tomcat.dbcp.dbcp.{PoolingDataSource, PoolableConnectionFactory, DriverManagerConnectionFactory}
import java.util.Properties

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
              maxWaitForConnectionInMS: Long = 1000,
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
  extends Queryable {

  metrics.gauge("active-connections", name) { pool.getNumActive }
  metrics.gauge("idle-connections", name)   { pool.getNumIdle }
  metrics.gauge("total-connections", name)  { pool.getNumIdle + pool.getNumActive }
  private val poolWait = metrics.timer("pool-wait")

  import Utils._

  var transactionProvider: TransactionProvider = new TransactionManager

  /**
   * Opens a transaction which is committed after `f` is called. If `f` throws
   * an exception, the transaction is rolled back.
   */
  def transaction[A](f: Transaction => A): A = transaction(true, f)

  /**
   * Opens a transaction which is committed after `f` is called. If `f` throws
   * an exception, the transaction is rolled back, but the exception is not
   * logged (since it is rethrown).
   */
  def quietTransaction[A](f: Transaction => A): A = transaction(false, f)

  /**
   * Opens a transaction which is committed after `f` is called. If `f` throws
   * an exception, the transaction is rolled back.
   */
  def transaction[A](logError: Boolean, f: Transaction => A): A = {
    if (transactionProvider.transactionExists) {
      f(transactionProvider.currentTransaction)
    } else {
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
          if (logError) {
            error("Exception thrown in transaction scope; aborting transaction", e)
          }
          connection.rollback()
          throw e
        }
      } finally {
        connection.close()
      }
    }
  }

  /**
   * Opens a transaction that is implicitly used in all db calls on the current
   * thread within the scope of `f`. If `f` throws an exception the transaction
   * is rolled back.
   */
  def transactionScope[A](f: => A): A = {
    transaction { txn =>
      transactionProvider.begin(txn)
      try {
        f
      } finally {
        transactionProvider.end
      }
    }
  }

  /**
   * The transaction currently scoped via transactionScope.
   */
  def currentTransaction = {
    transactionProvider.currentTransaction
  }

  /**
   * Returns {@code true} if we can talk to the database.
   */
  def ping() = apply(PingQuery)

  /**
   * Performs a query and returns the results.
   */
  def apply[A](query: RawQuery[A]): A = {
    if (transactionProvider.transactionExists) {
      transactionProvider.currentTransaction(query)
    } else {
      val connection = poolWait.time { source.getConnection }
      try {
        apply(connection, query)
      } finally {
        connection.close()
      }
    }
  }

  /**
   * Executes an update, insert, delete, or DDL statement.
   */
  def execute(statement: Statement) = {
    if (transactionProvider.transactionExists) {
      transactionProvider.currentTransaction.execute(statement)
    } else {
      val connection = poolWait.time { source.getConnection }
      try {
        execute(connection, statement)
      } finally {
        connection.close()
      }
    }
  }

  /**
   * Rollback any existing ambient transaction
   */
  def rollback() {
    transactionProvider.rollback
  }

  /**
   * Closes all connections to the database.
   */
  def close() {
    pool.close()
  }
}
