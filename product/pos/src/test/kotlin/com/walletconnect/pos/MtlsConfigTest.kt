package com.walletconnect.pos

import android.content.Context
import android.security.KeyChain
import com.walletconnect.pos.api.MtlsConfig
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.cert.X509Certificate
import io.mockk.mockk

class MtlsConfigTest {

    private val mockContext = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(KeyChain::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(KeyChain::class)
    }

    @Test
    fun `createSslConfigFromDeviceKeyChain - returns valid SSLSocketFactory and TrustManager`() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val mockCert = mockk<X509Certificate>(relaxed = true)

        every { KeyChain.getPrivateKey(mockContext, "SSL") } returns keyPair.private
        every { KeyChain.getCertificateChain(mockContext, "SSL") } returns arrayOf(mockCert)

        val (sslSocketFactory, trustManager) = MtlsConfig.createSslConfigFromDeviceKeyChain(mockContext, "SSL")

        assertNotNull(sslSocketFactory)
        assertNotNull(trustManager)
    }

    @Test
    fun `createSslConfigFromDeviceKeyChain - throws when private key not found`() {
        every { KeyChain.getPrivateKey(mockContext, "SSL") } returns null

        val exception = assertThrows(IllegalStateException::class.java) {
            MtlsConfig.createSslConfigFromDeviceKeyChain(mockContext, "SSL")
        }
        assert(exception.message!!.contains("No private key found"))
    }

    @Test
    fun `createSslConfigFromDeviceKeyChain - throws when cert chain not found`() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

        every { KeyChain.getPrivateKey(mockContext, "SSL") } returns keyPair.private
        every { KeyChain.getCertificateChain(mockContext, "SSL") } returns null

        val exception = assertThrows(IllegalStateException::class.java) {
            MtlsConfig.createSslConfigFromDeviceKeyChain(mockContext, "SSL")
        }
        assert(exception.message!!.contains("No certificate chain found"))
    }

    @Test
    fun `createSslConfigFromDeviceKeyChain - uses custom alias`() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val mockCert = mockk<X509Certificate>(relaxed = true)

        every { KeyChain.getPrivateKey(mockContext, "CUSTOM") } returns keyPair.private
        every { KeyChain.getCertificateChain(mockContext, "CUSTOM") } returns arrayOf(mockCert)

        val (sslSocketFactory, trustManager) = MtlsConfig.createSslConfigFromDeviceKeyChain(mockContext, "CUSTOM")

        assertNotNull(sslSocketFactory)
        assertNotNull(trustManager)
    }

    @Test
    fun `createSslConfigFromDeviceKeyChain - throws with wrong alias`() {
        every { KeyChain.getPrivateKey(mockContext, "WRONG") } returns null

        val exception = assertThrows(IllegalStateException::class.java) {
            MtlsConfig.createSslConfigFromDeviceKeyChain(mockContext, "WRONG")
        }
        assert(exception.message!!.contains("WRONG"))
    }
}
