package com.reown.sign.engine.domain

import com.reown.foundation.util.Logger
import com.reown.sign.common.model.vo.sequence.SessionVO
import com.reown.sign.engine.model.EngineDO
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.net.URL

class WalletServiceFinderTest {

    private lateinit var mockSession: SessionVO
    
    @Before
    fun setup() {
        // Mock the wcKoinApp for the logger
        mockkStatic("com.reown.android.internal.common.wcKoinAppKt")
        val mockLogger = mockk<Logger>(relaxed = true)
        val mockKoin = mockk<org.koin.core.Koin>()
        val mockKoinApp = mockk<org.koin.core.KoinApplication>()
        
        every { com.reown.android.internal.common.wcKoinApp } returns mockKoinApp
        every { mockKoinApp.koin } returns mockKoin
//        every { mockKoin.get<Logger>(any()) } returns mockLogger
    }
    
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
        val result = WalletServiceFinder.findMatchingWalletService(request, mockSession)
        
        // Assert
        assertNull(result)
    }
    
    @Test
    fun `findMatchingWalletService returns correct URL for exact chain match`() {
        // Arrange
        val exactChainJson = """
            {"walletService":[{"url":"https://exact-chain.com","methods":["wallet_getAssets"]}]}
        """.trimIndent()
        
        mockSession = mockk {
            every { scopedProperties } returns mapOf(
                "eip155:1" to exactChainJson
            )
        }
        
        val request = EngineDO.Request(
            topic = "test-topic",
            method = "wallet_getAssets",
            chainId = "eip155:1",
            params = ""
        )
        
        // Act
        val result = WalletServiceFinder.findMatchingWalletService(request, mockSession)
        
        // Assert
        assertEquals(URL("https://exact-chain.com"), result)
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
        val result = WalletServiceFinder.findMatchingWalletService(request, mockSession)
        
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
        val result = WalletServiceFinder.findMatchingWalletService(request, mockSession)
        
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
        val result = WalletServiceFinder.findMatchingWalletService(request, mockSession)
        
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
        val result = WalletServiceFinder.findMatchingWalletService(request, mockSession)
        
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
        val result = WalletServiceFinder.findMatchingWalletService(request, mockSession)
        
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
        val result = WalletServiceFinder.findMatchingWalletService(request, mockSession)
        
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
        val result = WalletServiceFinder.findMatchingWalletService(request, mockSession)
        
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
        val result = WalletServiceFinder.findMatchingWalletService(request, mockSession)
        
        // Assert
        assertEquals(URL("https://namespace.com"), result)
    }
} 