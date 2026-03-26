@file:JvmSynthetic

package com.walletconnect.pos.api

import java.io.InputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

internal object MtlsConfig {

    fun createSslConfig(
        clientP12: InputStream,
        password: CharArray
    ): Pair<SSLSocketFactory, X509TrustManager> {
        val keyStore = KeyStore.getInstance("PKCS12").apply {
            clientP12.use { stream -> load(stream, password) }
        }

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, password)
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
        }

        val trustManager = trustManagerFactory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .firstOrNull()
            ?: throw IllegalStateException("No X509TrustManager found in default trust manager factory")

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
        }

        return Pair(sslContext.socketFactory, trustManager)
    }
}
