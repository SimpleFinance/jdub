// Copyright 2013 Simple Finance Technology Corp.
// Author: Christopher Swenson (swenson@simple.com)
package com.simple.jdub

case class SslSettings(clientCertKeyStorePath: String,
                       trustKeyStoreProviderPath: String,
                       clientCertKeyStorePassword: Option[String],
                       clientCertKeyStoreProvider: Option[String] = None,
                       trustKeyStoreProvider: Option[String] = None)

