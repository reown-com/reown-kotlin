@file:JvmSynthetic

package com.walletconnect.pos.api

import android.content.Context
import android.security.KeyChain
import java.io.InputStream
import java.net.Socket
import java.security.KeyFactory
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509KeyManager
import javax.net.ssl.X509TrustManager

internal object MtlsConfig {

    private const val KEYCHAIN_ALIAS = "SSL"

    fun createSslConfigFromPem(
        certStream: InputStream,
        keyStream: InputStream
    ): Pair<SSLSocketFactory, X509TrustManager> {
        val certFactory = CertificateFactory.getInstance("X.509")
        val cert = certStream.use { certFactory.generateCertificate(it) as X509Certificate }
        val keyBytes = keyStream.use { parsePemPrivateKey(it.readBytes()) }
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(keyBytes))

        val keyStore = KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry("client", privateKey, charArrayOf(), arrayOf(cert))
        }
        return buildSslConfig(keyStore, charArrayOf())
    }

    fun createSslConfigFromDeviceKeyChain(context: Context): Pair<SSLSocketFactory, X509TrustManager> {
        val privateKey = KeyChain.getPrivateKey(context, KEYCHAIN_ALIAS)
            ?: error("No private key found for alias '$KEYCHAIN_ALIAS' in KeyChain")
        val certChain = KeyChain.getCertificateChain(context, KEYCHAIN_ALIAS)
            ?: error("No certificate chain found for alias '$KEYCHAIN_ALIAS' in KeyChain")

        val keyManager = object : X509KeyManager {
            override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?) = KEYCHAIN_ALIAS
            override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?) = arrayOf(KEYCHAIN_ALIAS)
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

    private fun buildSslConfig(
        keyStore: KeyStore,
        password: CharArray
    ): Pair<SSLSocketFactory, X509TrustManager> {
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

    private fun parsePemPrivateKey(pemBytes: ByteArray): ByteArray {
        val pem = String(pemBytes)
        val base64 = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        return java.util.Base64.getDecoder().decode(base64)
    }
}
