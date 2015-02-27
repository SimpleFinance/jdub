package com.simple.jdub

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import java.io.FileInputStream
import java.security.KeyStore
import java.util.{UUID, Properties}
import javax.sql.DataSource

import com.codahale.metrics.SharedMetricRegistries
import com.codahale.metrics.health.HealthCheckRegistry
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
              maxWait: Long = 1000,
              maxSize: Int = 8,
              jdbcProperties: Map[String, String] = Map.empty,
              sslSettings: Option[SslSettings] = None,
              healthCheckRegistry: Option[HealthCheckRegistry] = None): Database = {

    val properties = new Properties

    for { (k, v) <- jdbcProperties } {
      properties.setProperty(k, v)
    }

    for { settings <- sslSettings
          (k, v) <- initSsl(settings) } {
      properties.setProperty(k, v)
    }

    val poolConfig = new HikariConfig(properties) {
      setPoolName(name.getOrElse(url.replaceAll("[^A-Za-z0-9]", "")))
      setJdbcUrl(url)
      setUsername(username)
      setPassword(password)
      setConnectionTimeout(maxWait)
      setMaximumPoolSize(maxSize)
      setMetricRegistry(SharedMetricRegistries.getOrCreate("default"))
      healthCheckRegistry.map(setHealthCheckRegistry)
    }

    val poolDataSource  = new HikariDataSource(poolConfig)

    new Database(poolDataSource)
  }

  protected def initSsl(ssl: SslSettings): Map[String, String] = {
    // Load the client-side cert.
    val idStore = KeyStore.getInstance(ssl.clientCertKeyStoreProvider.getOrElse(KeyStore.getDefaultType))
    idStore.load(new FileInputStream(ssl.clientCertKeyStorePath),
                 ssl.clientCertKeyStorePassword.map { _.toCharArray }.orNull)

    // Load the CA certs.
    val trustStore = KeyStore.getInstance(ssl.trustKeyStoreProvider.getOrElse(KeyStore.getDefaultType))
    trustStore.load(new FileInputStream(ssl.trustKeyStoreProviderPath), null)

    // Set it so that the ssl socket factory knows how to find these parameters
    val params = SslParams(idStore, ssl.clientCertKeyStorePassword.orNull, trustStore)
    val identifier = UUID.randomUUID().toString

    ClientSideCertSslSocketFactoryFactory.configure(identifier, params)

    Map("ssl" -> "true",
        "sslfactory" -> "com.simple.jdub.ClientSideCertSslSocketFactoryFactory",
        "sslfactoryarg" -> identifier)
  }
}

/**
 * A set of pooled connections to a database.
 */
class Database protected(val source: DataSource)
    extends Queryable {

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
      val connection = source.getConnection
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
   * is rolled back.
   */
  def transactionScope[A](f: => A): A = {
    transaction(true, false, (txn: Transaction) => {
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
   * exception the transaction is rolled back.
   */
  def newTransactionScope[A](f: => A): A = {
    transaction(true, true, (txn: Transaction) => {
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
      val connection = source.getConnection
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
      val connection = source.getConnection
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
    if (source.isInstanceOf[HikariDataSource]) {
      source.asInstanceOf[HikariDataSource].close()
    }
  }
}
