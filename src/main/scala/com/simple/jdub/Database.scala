package com.simple.jdub

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import java.io.FileInputStream
import java.security.KeyStore
import java.util.{Properties, UUID}
import javax.sql.DataSource
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheckRegistry
import com.simple.jdub.Database.Primary
import com.simple.jdub.Database.Replica

import scala.annotation.implicitNotFound

object Database {

  sealed trait Role
  final class Primary extends Role
  final class Replica extends Role

  /**
   * Create a pool of connections to the given database.
   *
   * @param url the JDBC url
   * @param username the database user
   * @param password the database password
   * @param sslSettings if present, uses the given SSL settings for a client-side SSL cert.
   */
  def connect[R <: Role](url: String,
              username: String,
              password: String,
              name: Option[String] = None,
              maxWait: Long = 1000,
              maxSize: Int = 8,
              jdbcProperties: Map[String, String] = Map.empty,
              sslSettings: Option[SslSettings] = None,
              healthCheckRegistry: Option[HealthCheckRegistry] = None,
              metricRegistry: Option[MetricRegistry] = None,
              connectionInitSql: Option[String] = None): Database[R] = {

    val properties = new Properties

    for { (k, v) <- jdbcProperties } {
      properties.setProperty(k, v)
    }

    val poolConfig = new HikariConfig(properties) {
      setPoolName(name.getOrElse(url.replaceAll("[^A-Za-z0-9]", "")))
      setJdbcUrl(url)
      setUsername(username)
      setPassword(password)
      setConnectionTimeout(maxWait)
      setMaximumPoolSize(maxSize)
      healthCheckRegistry.map(setHealthCheckRegistry)
      metricRegistry.map(setMetricRegistry)
      connectionInitSql.map(setConnectionInitSql)

      for { settings <- sslSettings
            (k, v) <- initSsl(settings) } {
        addDataSourceProperty(k, v)
      }
    }

    val poolDataSource  = new HikariDataSource(poolConfig)

    new Database(poolDataSource, metricRegistry)
  }

  protected def initSsl(ssl: SslSettings): Map[String, String] = {
    // ready the client-side certs
    val clientCertKeyStoreProvider = ssl.clientCertKeyStoreProvider.getOrElse(KeyStore.getDefaultType)
    val clientCertKeyStorePassword = ssl.clientCertKeyStorePassword.map(_.toCharArray).orNull
    val clientCertKeyStoreStream = new FileInputStream(ssl.clientCertKeyStorePath)
    val clientCertKeyStore = KeyStore.getInstance(clientCertKeyStoreProvider)

    // ready the ca certs
    val trustKeyStoreProvider = ssl.trustKeyStoreProvider.getOrElse(KeyStore.getDefaultType)
    val trustKeyStoreStream = new FileInputStream(ssl.trustKeyStoreProviderPath)
    val trustKeyStore = KeyStore.getInstance(trustKeyStoreProvider)

    // load everything up
    clientCertKeyStore.load(clientCertKeyStoreStream, clientCertKeyStorePassword)
    trustKeyStore.load(trustKeyStoreStream, null)

    // get some parameters ready for the ssl socket factory
    val identifier = UUID.randomUUID().toString
    val sslParams = SslParams(clientCertKeyStore,
                              ssl.clientCertKeyStorePassword.orNull,
                              trustKeyStore)

    // let the factory know about our ssl params
    ClientSideCertSslSocketFactoryFactory.configure(identifier, sslParams)

    // return a set of jdbc properties that need to be set
    Map("ssl" -> "true",
        "sslfactory" -> "com.simple.jdub.ClientSideCertSslSocketFactoryFactory",
        "sslfactoryarg" -> identifier)
  }
}

/**
 * A set of pooled connections to a database.
 */
class Database[R <: Database.Role] protected(val source: DataSource, metrics: Option[MetricRegistry])
    extends Queryable[R] {

  private[jdub] def time[A](klass: java.lang.Class[_])(f: => A) = {
    metrics.fold(f) { registry =>
      val timer = registry.timer(MetricRegistry.name(klass))
      val ctx = timer.time()
      try {
        f
      } finally {
        ctx.stop
      }
    }
  }

  val transactionProvider: TransactionProvider[R] = new TransactionManager

  def replica: Database[Replica] = new Database[Replica](source, metrics)

  def primary: Database[Primary] = new Database[Primary](source, metrics)

  /**
   * Opens a transaction which is committed after `f` is called. If `f` throws
   * an exception, the transaction is rolled back.
   */
  def transaction[A](f: Transaction[R] => A)(implicit ev: R =:= Primary): A = transaction(true, f)

  /**
   * Opens a transaction which is committed after `f` is called. If `f` throws
   * an exception, the transaction is rolled back, but the exception is not
   * logged (since it is rethrown).
   */
  def quietTransaction[A](f: Transaction[R] => A)(implicit ev: R =:= Primary): A = transaction(false, f)

  def transaction[A](logError: Boolean, f: Transaction[R] => A)(implicit ev: R =:= Primary): A = transaction(false, false, f)

  /**
   * Opens a transaction which is committed after `f` is called. If `f` throws
   * an exception, the transaction is rolled back.
   */
  def transaction[A](logError: Boolean, forceNew: Boolean, f: Transaction[R] => A)(implicit ev: R =:= Primary): A = {
    if (!forceNew && transactionProvider.transactionExists) {
      f(transactionProvider.currentTransaction)
    } else {
      val connection = source.getConnection
      connection.setAutoCommit(false)
      val txn = new Transaction[R](connection)
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
  def transactionScope[A](f: => A)(implicit ev: R =:= Primary): A = {
    transaction(logError = true, forceNew = false, (txn: Transaction[R]) => {
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
  def newTransactionScope[A](f: => A)(implicit ev: R =:= Primary): A = {
    transaction(logError = true, forceNew = true, (txn: Transaction[R]) => {
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
  def quietTransactionScope[A](f: => A)(implicit ev: R =:= Primary): A = {
    transaction(logError = false, forceNew = false, (txn: Transaction[R]) => {
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
  def newQuietTransactionScope[A](f: => A)(implicit ev: R =:= Primary): A = {
    transaction(logError = false, forceNew = true, (txn: Transaction[R]) => {
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
  def currentTransaction(implicit ev: R =:= Primary) = {
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
        time(query.getClass()) {
          apply(connection, query)
        }
      } finally {
        connection.close()
      }
    }
  }

  /**
   * Executes an update, insert, delete, or DDL statement.
   */
  def execute(statement: Statement)(implicit ev: R =:= Primary): Int = {
    if (transactionProvider.transactionExists) {
      transactionProvider.currentTransaction.execute(statement)
    } else {
      val connection = source.getConnection
      try {
        time(statement.getClass()) {
          execute(connection, statement)
        }
      } finally {
        connection.close()
      }
    }
  }

  /**
   * Rollback any existing ambient transaction
   */
  def rollback()(implicit ev: R =:= Primary) {
    transactionProvider.rollback
  }

  /**
   * Closes all connections to the database.
   */
  def close() {
    if (source.isWrapperFor(classOf[HikariDataSource])) {
      source.unwrap(classOf[HikariDataSource]).close()
    }
  }
}
