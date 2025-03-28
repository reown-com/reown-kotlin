package com.reown.sign.engine.domain

import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.wcKoinApp
import com.reown.foundation.util.Logger
import com.reown.sign.common.model.vo.sequence.SessionVO
import com.reown.sign.engine.model.EngineDO
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.net.URL

class WalletServiceFinderTest {
    private lateinit var mockSession: SessionVO
    private var mockLogger: Logger = mockk<Logger>(relaxed = true)
    private val projectId = "test-project-id"
    private val sdkVersion = "1.0.0"
    private var walletServiceFinder: WalletServiceFinder = WalletServiceFinder(mockLogger)

    @Test
    fun `findMatchingWalletService returns null when scopedProperties is null`() {
        // Arrange
        mockSession = mockk {
            every { scopedProperties } returns null
        }

        val request = EngineDO.Request(
            topic = "test-topic",
            method = "wallet_getAssets",
            chainId = "eip155:1",
            params = ""
        )

        // Act
        val result = walletServiceFinder.findMatchingWalletService(request, mockSession)

        // Assert
        assertNull(result)
    }

    @Test
    fun `findMatchingWalletService returns correct URL for exact chain match`() {
        // Arrange
        val scopedPropertiesTest = mapOf(
            "eip155:1" to "{\"walletService\":[{\"url\":\"https://rpc.walletconnect.org/v1/wallet?projectId=$projectId&st=wkca&sv=reown-kotlin-$sdkVersion\", \"methods\":[\"wallet_getAssets\"]}]}"
        )

        mockSession = mockk {
            every { scopedProperties } returns scopedPropertiesTest
        }
        val request = EngineDO.Request(
            topic = "test-topic",
            method = "wallet_getAssets",
            chainId = "eip155:1",
            params = ""
        )

        // Act
        val result = walletServiceFinder.findMatchingWalletService(request, mockSession)

        // Assert
        assertEquals(URL("https://rpc.walletconnect.org/v1/wallet?projectId=test-project-id&st=wkca&sv=reown-kotlin-1.0.0"), result)
    }

    @Test
    fun `findMatchingWalletService returns correct URL for namespace match when exact chain not found`() {
        // Arrange
        val namespaceJson = """
            {"walletService":[{"url":"https://namespace.com","methods":["wallet_getAssets"]}]}
        """.trimIndent()

        mockSession = mockk {
            every { scopedProperties } returns mapOf(
                "eip155" to namespaceJson
            )
        }

        val request = EngineDO.Request(
            topic = "test-topic",
            method = "wallet_getAssets",
            chainId = "eip155:1",
            params = ""
        )

        // Act
        val result = walletServiceFinder.findMatchingWalletService(request, mockSession)

        // Assert
        assertEquals(URL("https://namespace.com"), result)
    }

    @Test
    fun `findMatchingWalletService returns null when method not supported`() {
        // Arrange
        val scopeJson = """
            {"walletService":[{"url":"https://service.com","methods":["other_method"]}]}
        """.trimIndent()

        mockSession = mockk {
            every { scopedProperties } returns mapOf(
                "eip155:1" to scopeJson
            )
        }

        val request = EngineDO.Request(
            topic = "test-topic",
            method = "wallet_getAssets",
            chainId = "eip155:1",
            params = ""
        )

        // Act
        val result = walletServiceFinder.findMatchingWalletService(request, mockSession)

        // Assert
        assertNull(result)
    }

    @Test
    fun `findMatchingWalletService handles invalid URL`() {
        // Arrange
        val scopeJson = """
            {"walletService":[{"url":"invalid-url","methods":["wallet_getAssets"]}]}
        """.trimIndent()

        mockSession = mockk {
            every { scopedProperties } returns mapOf(
                "eip155:1" to scopeJson
            )
        }

        val request = EngineDO.Request(
            topic = "test-topic",
            method = "wallet_getAssets",
            chainId = "eip155:1",
            params = ""
        )

        // Act
        val result = walletServiceFinder.findMatchingWalletService(request, mockSession)

        // Assert
        assertNull(result)
    }

    @Test
    fun `findMatchingWalletService handles multiple services and returns first matching one`() {
        // Arrange
        val scopeJson = """
            {"walletService":[
                {"url":"https://first.com","methods":["other_method"]},
                {"url":"https://second.com","methods":["wallet_getAssets"]},
                {"url":"https://third.com","methods":["wallet_getAssets"]}
            ]}
        """.trimIndent()

        mockSession = mockk {
            every { scopedProperties } returns mapOf(
                "eip155:1" to scopeJson
            )
        }

        val request = EngineDO.Request(
            topic = "test-topic",
            method = "wallet_getAssets",
            chainId = "eip155:1",
            params = ""
        )

        // Act
        val result = walletServiceFinder.findMatchingWalletService(request, mockSession)

        // Assert
        assertEquals(URL("https://second.com"), result)
    }

    @Test
    fun `findMatchingWalletService handles invalid JSON in scopedProperties`() {
        // Arrange
        mockSession = mockk {
            every { scopedProperties } returns mapOf(
                "eip155:1" to "invalid-json"
            )
        }

        val request = EngineDO.Request(
            topic = "test-topic",
            method = "wallet_getAssets",
            chainId = "eip155:1",
            params = ""
        )

        // Act
        val result = walletServiceFinder.findMatchingWalletService(request, mockSession)

        // Assert
        assertNull(result)
    }

    @Test
    fun `findMatchingWalletService handles missing walletService array`() {
        // Arrange
        val scopeJson = """
            {"otherField":"some value"}
        """.trimIndent()

        mockSession = mockk {
            every { scopedProperties } returns mapOf(
                "eip155:1" to scopeJson
            )
        }

        val request = EngineDO.Request(
            topic = "test-topic",
            method = "wallet_getAssets",
            chainId = "eip155:1",
            params = ""
        )

        // Act
        val result = walletServiceFinder.findMatchingWalletService(request, mockSession)

        // Assert
        assertNull(result)
    }

    @Test
    fun `findMatchingWalletService handles empty methods array`() {
        // Arrange
        val scopeJson = """
            {"walletService":[{"url":"https://service.com","methods":[]}]}
        """.trimIndent()

        mockSession = mockk {
            every { scopedProperties } returns mapOf(
                "eip155:1" to scopeJson
            )
        }

        val request = EngineDO.Request(
            topic = "test-topic",
            method = "wallet_getAssets",
            chainId = "eip155:1",
            params = ""
        )

        // Act
        val result = walletServiceFinder.findMatchingWalletService(request, mockSession)

        // Assert
        assertNull(result)
    }

    @Test
    fun `findMatchingWalletService fallbacks to namespace when exact chain not found`() {
        // Arrange
        val namespaceJson = """
            {"walletService":[{"url":"https://namespace.com","methods":["wallet_getAssets"]}]}
        """.trimIndent()

        mockSession = mockk {
            every { scopedProperties } returns mapOf(
                "eip155" to namespaceJson
            )
        }

        val request = EngineDO.Request(
            topic = "test-topic",
            method = "wallet_getAssets",
            chainId = "eip155:137", // Polygon chain ID
            params = ""
        )

        // Act
        val result = walletServiceFinder.findMatchingWalletService(request, mockSession)

        // Assert
        assertEquals(URL("https://namespace.com"), result)
    }
} 