package com.walletconnect.pos

import com.walletconnect.pos.api.MtlsConfig
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

class MtlsConfigTest {

    private fun loadTestP12(): ByteArray =
        javaClass.classLoader!!.getResourceAsStream("test-client.p12")!!.readBytes()

    @Test
    fun `createSslConfig - returns valid SSLSocketFactory and TrustManager`() {
        val p12Bytes = loadTestP12()
        val (sslSocketFactory, trustManager) = MtlsConfig.createSslConfig(
            ByteArrayInputStream(p12Bytes),
            "changeit".toCharArray()
        )
        assertNotNull(sslSocketFactory)
        assertNotNull(trustManager)
        assert(sslSocketFactory is SSLSocketFactory)
        assert(trustManager is X509TrustManager)
    }

    @Test
    fun `createSslConfig - throws on wrong password`() {
        val p12Bytes = loadTestP12()
        assertThrows(IOException::class.java) {
            MtlsConfig.createSslConfig(
                ByteArrayInputStream(p12Bytes),
                "wrong-password".toCharArray()
            )
        }
    }

    @Test
    fun `createSslConfig - throws on empty stream`() {
        assertThrows(IOException::class.java) {
            MtlsConfig.createSslConfig(
                ByteArrayInputStream(ByteArray(0)),
                "changeit".toCharArray()
            )
        }
    }

    @Test
    fun `createSslConfig - throws on invalid data`() {
        assertThrows(IOException::class.java) {
            MtlsConfig.createSslConfig(
                ByteArrayInputStream("not a pkcs12 file".toByteArray()),
                "changeit".toCharArray()
            )
        }
    }
}
