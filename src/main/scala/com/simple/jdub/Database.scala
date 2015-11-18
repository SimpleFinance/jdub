package com.simple.jdub

import org.apache.tomcat.dbcp.dbcp.{PoolingDataSource, PoolableConnectionFactory, DriverManagerConnectionFactory}
import org.apache.tomcat.dbcp.pool.impl.GenericObjectPool

import java.io.FileInputStream
import java.security.KeyStore
import java.util.{UUID, Properties}
import javax.sql.DataSource

import com.codahale.metrics.SharedMetricRegistries
import nl.grons.metrics.scala.InstrumentedBuilder

trait Instrumented extends InstrumentedBuilder {
  final val metricRegistry = SharedMetricRegistries.getOrCreate("default")
}

object Database {

  /**
   * Create a pool of connections to the given database.
   *
   * @param url the JDBC url
   * @param username the database user
   * @param password the database password
   * @param sslSettings if present, uses the given SSL settings for a client-side SSL cert.
   */
  def connect(url: String,
              username: String,
              password: String,
              name: Option[String] = None,
              maxWaitForConnectionInMS: Long = 1000,
              maxSize: Int = 8,
              minSize: Int = 0,
              checkConnectionWhileIdle: Boolean = true,
              checkConnectionHealthWhenIdleForMS: Long = 10000,
              closeConnectionIfIdleForMS: Long = 1000L * 60L * 30L,
              healthCheckQuery: String = Utils.prependComment(PingQuery, PingQuery.sql),
              jdbcProperties: Map[String, String] = Map.empty,
              sslSettings: Option[SslSettings] = None): Database = {

    val properties = new Properties
    for ((k, v) <- jdbcProperties) {
      properties.setProperty(k, v)
    }
    properties.setProperty("user", username)
    properties.setProperty("password", password)

    // Configure SSL for client-side SSL.
    sslSettings match {
      case Some(ssl) =>
        // Load the client-side cert.
        val idStore = KeyStore.getInstance(
          ssl.clientCertKeyStoreProvider.getOrElse(KeyStore.getDefaultType))
        idStore.load(new FileInputStream(ssl.clientCertKeyStorePath),
          ssl.clientCertKeyStorePassword.map { _.toCharArray }.orNull)

        // Load the CA certs.
        val trustStore = KeyStore.getInstance(
          ssl.trustKeyStoreProvider.getOrElse(KeyStore.getDefaultType))
        trustStore.load(new FileInputStream(ssl.trustKeyStoreProviderPath), null)

        // Set it so that the ssl socket factory knows how to find these parameters
        val params = SslParams(idStore, ssl.clientCertKeyStorePassword.orNull, trustStore)
        val arg = UUID.randomUUID().toString
        ClientSideCertSslSocketFactoryFactory.configure(arg, params)

        // Tell JDBC we are using SSL
        // http://jdbc.postgresql.org/documentation/80/connect.html
        properties.setProperty("ssl", "true")
        // We set these parameters as required by the Postgres JDBC
        // driver. It expects the SSL factory name and a string argument.
        // http://jdbc.postgresql.org/documentation/91/ssl-factory.html
        properties.setProperty("sslfactory", "com.simple.jdub.ClientSideCertSslSocketFactoryFactory")
        properties.setProperty("sslfactoryarg", arg)

      case None =>
        // No SSL settings; just use default.
    }

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
    new Database(new PoolingDataSource(pool), pool, 
                 name.getOrElse(url.replaceAll("[^A-Za-z0-9]", "")))
  }
}

/**
 * A set of pooled connections to a database.
 */
class Database protected(val source: DataSource, pool: GenericObjectPool, name: String)
    extends Queryable {

  metrics.gauge("active-connections", name) { pool.getNumActive }
  metrics.gauge("idle-connections", name)   { pool.getNumIdle }
  metrics.gauge("total-connections", name)  { pool.getNumIdle + pool.getNumActive }
  private val poolWait = metrics.timer("pool-wait")


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

  def transaction[A](logError: Boolean, f: Transaction => A): A = transaction(false, false, f)

  /**
   * Opens a transaction which is committed after `f` is called. If `f` throws
   * an exception, the transaction is rolled back.
   */
  def transaction[A](logError: Boolean, forceNew: Boolean, f: Transaction => A): A = {
    if (!forceNew && transactionProvider.transactionExists) {
      f(transactionProvider.currentTransaction)
    } else {
      val connection = poolWait.time { source.getConnection }
      connection.setAutoCommit(false)
      val txn = new Transaction(connection)
      try {
        logger.debug("Starting transaction")
        val result = f(txn)
        txn.commit()
        result
      } catch {
        case t: Throwable => {
          if (logError) {
            logger.logger.error("Exception thrown in transaction scope; aborting transaction", t)
          }
          txn.rollback()
          throw t
        }
      } finally {
        txn.close()
      }
    }
  }

  /**
   * Opens a transaction that is implicitly used in all db calls on the current
   * thread within the scope of `f`. If `f` throws an exception the transaction
   * is rolled back. Logs exceptions thrown by `f` as errors.
   */
  def transactionScope[A](f: => A): A = {
    transaction(logError = true, forceNew = false, (txn: Transaction) => {
      transactionProvider.begin(txn)
      try {
        f
      } finally {
        transactionProvider.end
      }
    })
  }

  /**
   * Opens a new transaction that is implicitly used in all db calls
   * on the current thread within the scope of `f`. If a
   * transactionScope already exists in scope when called this method
   * will create a new separate transactionScope. If `f` throws an
   * exception the transaction is rolled back. Logs exceptions thrown by
   * `f` as errors.
   */
  def newTransactionScope[A](f: => A): A = {
    transaction(logError = true, forceNew = true, (txn: Transaction) => {
      transactionProvider.begin(txn)
      try {
        f
      } finally {
        transactionProvider.end
      }
    })
  }

  /**
   * Opens a transaction that is implicitly used in all db calls on the current
   * thread within the scope of `f`. If `f` throws an exception the transaction
   * is rolled back. Will not log exceptions thrown by `f`.
   */
  def quietTransactionScope[A](f: => A): A = {
    transaction(logError = false, forceNew = false, (txn: Transaction) => {
      transactionProvider.begin(txn)
      try {
        f
      } finally {
        transactionProvider.end
      }
    })
  }

  /**
   * Opens a new transaction that is implicitly used in all db calls
   * on the current thread within the scope of `f`. If a
   * transactionScope already exists in scope when called this method
   * will create a new separate transactionScope. If `f` throws an
   * exception the transaction is rolled back. Will not log exceptions
   * thrown by `f`.
   */
  def newQuietTransactionScope[A](f: => A): A = {
    transaction(logError = false, forceNew = true, (txn: Transaction) => {
      transactionProvider.begin(txn)
      try {
        f
      } finally {
        transactionProvider.end
      }
    })
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
