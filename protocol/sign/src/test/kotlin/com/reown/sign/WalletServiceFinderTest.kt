package com.reown.sign

import com.reown.foundation.util.Logger
import com.reown.sign.common.model.vo.sequence.SessionVO
import com.reown.sign.engine.domain.WalletServiceFinder
import com.reown.sign.engine.model.EngineDO
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Test
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WalletServiceFinderTest {
    @MockK
    private var mockLogger: Logger = mockk<Logger>(relaxed = true)
    private val projectId = "test-project-id"
    private val sdkVersion = "1.0.0"
    private var walletServiceFinder: WalletServiceFinder = WalletServiceFinder(mockLogger)

    @Test
    fun `findMatchingWalletService returns null when scopedProperties is null`() {
        // Given
        val mockRequest = mockk<EngineDO.Request>()
        val mockSession = mockk<SessionVO>()
        every { mockSession.scopedProperties } returns null

        // When
        val result = walletServiceFinder.findMatchingWalletService(mockRequest, mockSession)

        // Then
        assertNull(result)
    }

    @Test
    fun `findMatchingWalletService returns URL when exact chain match found`() {
        // Given
        val scopedProperties = mapOf(
            "eip155:1" to "{\"walletService\":[{\"url\":\"https://rpc.walletconnect.org/v1/wallet?projectId=$projectId&st=wkca&sv=reown-kotlin-$sdkVersion\", \"methods\":[\"wallet_getAssets\"]}]}"
        )

        val mockRequest = mockk<EngineDO.Request>()
        every { mockRequest.chainId } returns "eip155:1"
        every { mockRequest.method } returns "wallet_getAssets"

        val mockSession = mockk<SessionVO>()
        every { mockSession.scopedProperties } returns scopedProperties

        // When
        val result = walletServiceFinder.findMatchingWalletService(mockRequest, mockSession)

        // Then
        val expectedUrl = URL("https://rpc.walletconnect.org/v1/wallet?projectId=$projectId&st=wkca&sv=reown-kotlin-$sdkVersion")
        assertEquals(expectedUrl, result)
    }

    @Test
    fun `findMatchingWalletService returns URL when namespace match found`() {
        // Given
        val scopedProperties = mapOf(
            "eip155" to "{\"walletService\":[{\"url\":\"https://rpc.walletconnect.org/v1/wallet?projectId=$projectId&st=wkca&sv=reown-kotlin-$sdkVersion\", \"methods\":[\"wallet_getAssets\"]}]}"
        )

        val mockRequest = mockk<EngineDO.Request>()
        every { mockRequest.chainId } returns "eip155:1"
        every { mockRequest.method } returns "wallet_getAssets"

        val mockSession = mockk<SessionVO>()
        every { mockSession.scopedProperties } returns scopedProperties

        // When
        val result = walletServiceFinder.findMatchingWalletService(mockRequest, mockSession)

        // Then
        val expectedUrl = URL("https://rpc.walletconnect.org/v1/wallet?projectId=$projectId&st=wkca&sv=reown-kotlin-$sdkVersion")
        assertEquals(expectedUrl, result)
    }

    @Test
    fun `findMatchingWalletService returns null when method not supported`() {
        // Given
        val scopedProperties = mapOf(
            "eip155" to "{\"walletService\":[{\"url\":\"https://rpc.walletconnect.org/v1/wallet?projectId=$projectId&st=wkca&sv=reown-kotlin-$sdkVersion\", \"methods\":[\"wallet_getAssets\"]}]}"
        )

        val mockRequest = mockk<EngineDO.Request>()
        every { mockRequest.chainId } returns "eip155:1"
        every { mockRequest.method } returns "unsupported_method"

        val mockSession = mockk<SessionVO>()
        every { mockSession.scopedProperties } returns scopedProperties

        // When
        val result = walletServiceFinder.findMatchingWalletService(mockRequest, mockSession)

        // Then
        assertNull(result)
    }

    @Test
    fun `findMatchingWalletService returns null when no wallet service matches`() {
        // Given
        val scopedProperties = mapOf(
            "different:namespace" to "{\"walletService\":[{\"url\":\"https://example.com\", \"methods\":[\"wallet_getAssets\"]}]}"
        )

        val mockRequest = mockk<EngineDO.Request>()
        every { mockRequest.chainId } returns "eip155:1"
        every { mockRequest.method } returns "wallet_getAssets"

        val mockSession = mockk<SessionVO>()
        every { mockSession.scopedProperties } returns scopedProperties

        // When
        val result = walletServiceFinder.findMatchingWalletService(mockRequest, mockSession)

        // Then
        assertNull(result)
    }

    @Test
    fun `findWalletService handles invalid JSON properly`() {
        // Given
        val scopedProperties = mapOf(
            "eip155" to "invalid json"
        )

        val mockRequest = mockk<EngineDO.Request>()
        every { mockRequest.chainId } returns "eip155:1"
        every { mockRequest.method } returns "invalid_json"

        val mockSession = mockk<SessionVO>()
        every { mockSession.scopedProperties } returns scopedProperties

        // When
        val result = walletServiceFinder.findMatchingWalletService(mockRequest, mockSession)

        // Then
        assertNull(result)
    }
}