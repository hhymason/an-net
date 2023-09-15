/*
 * Copyright (c) 2015 - present Hive-Box.
 */

package com.mason.net

import android.annotation.SuppressLint
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

internal class SslClient private constructor() {
    var socketFactory: SSLSocketFactory? = null
    var trustManager: X509TrustManager? = null

    companion object {
        @Synchronized
        fun create(
            bksFile: InputStream?,
            password: String?
        ): SslClient? {
            val sslClient = SslClient()
            try {
                val keyManagers = prepareKeyManager(bksFile, password)
                val trustManager: X509TrustManager = UnSafeTrustManager()
                val sslContext = SSLContext.getInstance("TLS").apply {
                    init(keyManagers, arrayOf<TrustManager>(trustManager), null)
                }
                sslClient.socketFactory = sslContext.socketFactory
                sslClient.trustManager = trustManager
                return sslClient
            } catch (e: Exception) {
                LogNet.e(e)
            }
            return null
        }

        private fun prepareKeyManager(
            bksFile: InputStream?,
            password: String?
        ): Array<KeyManager>? {
            try {
                if (bksFile == null || password == null) {
                    return null
                }
                val clientKeyStore = KeyStore.getInstance("BKS")
                clientKeyStore.load(bksFile, password.toCharArray())
                val keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                keyManagerFactory.init(clientKeyStore, password.toCharArray())
                return keyManagerFactory.keyManagers
            } catch (e: Exception) {
                LogNet.e(e)
            }
            return null
        }
    }

    private class UnSafeTrustManager : X509TrustManager {
        @SuppressLint("TrustAllX509TrustManager")
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            // ignored
        }

        @SuppressLint("TrustAllX509TrustManager")
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            // ignored
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }
}
