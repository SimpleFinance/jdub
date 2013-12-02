// Copyright 2013 Simple Finance Technology Corp.
// Author: Christopher Swenson (swenson@simple.com)
package com.simple.jdub

import java.net.{Socket, InetAddress}
import java.security.KeyStore
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl._

case class SslParams(identityStore: KeyStore, identityStorePassword: String, trustStore: KeyStore)

/**
 * SslSocketFactory Factory loljava.
 */
object ClientSideCertSslSocketFactoryFactory {
  val configs = new ConcurrentHashMap[String, SslParams]

  /**
   * Configure socket factory parameters.
   */
  def configure(param: String, params: SslParams) {
    configs.put(param, params)
  }

  /**
   * Create a new instance of the SSL socket factory using the stored parameters.
   */
  def factory(param: String): SSLSocketFactory = {
    val params = configs.get(param)
    if (params == null) {
      throw new IllegalArgumentException("Unknown ssl socket factory params \"%s\"".format(param))
    }

    val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    keyManagerFactory.init(params.identityStore, params.identityStorePassword.toCharArray)
    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    trustManagerFactory.init(params.trustStore)
    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, null)
    context.getSocketFactory
  }
}

/**
 * Thunk the saved SSL socket factory to the delegate using the saved parameters.
 */
class ClientSideCertSslSocketFactoryFactory(param: String) extends SSLSocketFactory {
  val delegate = ClientSideCertSslSocketFactoryFactory.factory(param)

  def getDefaultCipherSuites: Array[String] = delegate.getDefaultCipherSuites

  def getSupportedCipherSuites: Array[String] = delegate.getSupportedCipherSuites

  def createSocket(p1: Socket, p2: String, p3: Int, p4: Boolean): Socket = delegate.createSocket(p1, p2, p3, p4)

  def createSocket(p1: String, p2: Int): Socket = delegate.createSocket(p1, p2)

  def createSocket(p1: String, p2: Int, p3: InetAddress, p4: Int): Socket = delegate.createSocket(p1, p2, p3, p4)

  def createSocket(p1: InetAddress, p2: Int): Socket = delegate.createSocket(p1, p2)

  def createSocket(p1: InetAddress, p2: Int, p3: InetAddress, p4: Int): Socket = delegate.createSocket(p1, p2, p3, p4)
}
