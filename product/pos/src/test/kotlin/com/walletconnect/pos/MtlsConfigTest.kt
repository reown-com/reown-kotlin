package com.walletconnect.pos

import com.walletconnect.pos.api.MtlsConfig
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

class MtlsConfigTest {

    private fun loadTestCert(): ByteArray =
        javaClass.classLoader!!.getResourceAsStream("test-client.crt")!!.readBytes()

    private fun loadTestKey(): ByteArray =
        javaClass.classLoader!!.getResourceAsStream("test-client.key")!!.readBytes()

    @Test
    fun `createSslConfigFromPem - returns valid SSLSocketFactory and TrustManager`() {
        val (sslSocketFactory, trustManager) = MtlsConfig.createSslConfigFromPem(
            ByteArrayInputStream(loadTestCert()),
            ByteArrayInputStream(loadTestKey())
        )
        assertNotNull(sslSocketFactory)
        assertNotNull(trustManager)
        assert(sslSocketFactory is SSLSocketFactory)
        assert(trustManager is X509TrustManager)
    }

    @Test
    fun `createSslConfigFromPem - throws on invalid cert`() {
        assertThrows(Exception::class.java) {
            MtlsConfig.createSslConfigFromPem(
                ByteArrayInputStream("not a cert".toByteArray()),
                ByteArrayInputStream(loadTestKey())
            )
        }
    }

    @Test
    fun `createSslConfigFromPem - throws on invalid key`() {
        assertThrows(Exception::class.java) {
            MtlsConfig.createSslConfigFromPem(
                ByteArrayInputStream(loadTestCert()),
                ByteArrayInputStream("not a key".toByteArray())
            )
        }
    }

    @Test
    fun `createSslConfigFromPem - throws on empty streams`() {
        assertThrows(Exception::class.java) {
            MtlsConfig.createSslConfigFromPem(
                ByteArrayInputStream(ByteArray(0)),
                ByteArrayInputStream(ByteArray(0))
            )
        }
    }
}
