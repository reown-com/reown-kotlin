@file:JvmSynthetic

package com.walletconnect.pos.api

import android.content.Context
import android.security.KeyChain
import java.net.Socket
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509KeyManager
import javax.net.ssl.X509TrustManager

internal object MtlsConfig {

    fun createSslConfigFromDeviceKeyChain(context: Context, alias: String): Pair<SSLSocketFactory, X509TrustManager> {
        val privateKey = KeyChain.getPrivateKey(context, alias)
            ?: error("No private key found for alias '$alias' in KeyChain")
        val certChain = KeyChain.getCertificateChain(context, alias)
            ?: error("No certificate chain found for alias '$alias' in KeyChain")

        val keyManager = object : X509KeyManager {
            override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?) = alias
            override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?) = arrayOf(alias)
            override fun getPrivateKey(alias: String?): PrivateKey = privateKey
            override fun getCertificateChain(alias: String?): Array<X509Certificate> = certChain
            override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?) = null
            override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?) = null
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
        }
        val trustManager = trustManagerFactory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .first()

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(arrayOf(keyManager), trustManagerFactory.trustManagers, null)
        }

        return Pair(sslContext.socketFactory, trustManager)
    }
}
