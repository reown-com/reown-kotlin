package com.reown.sign

import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.wcKoinApp
import com.reown.android.relay.RelayClient
import com.reown.foundation.util.Logger
import com.reown.sign.common.model.vo.sequence.SessionVO
import com.reown.sign.engine.domain.WalletServiceFinder
import com.reown.sign.engine.model.EngineDO
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WalletServiceFinderTest {
    @MockK
    private var mockLogger: Logger = mockk<Logger>(relaxed = true)
    private val projectId = "test-project-id"
    private val sdkVersion = "1.0.0"

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        startKoin {
            modules(module {
                single(named(AndroidCommonDITags.LOGGER)) { mockLogger }
            })
        }
        mockkStatic(AndroidCommonDITags::class)

//        stopKoin() // Stop any existing Koin instance
//
//        // Create mock logger
//        mockLogger = mockk<Logger>(relaxed = true)
//
//        // Set up Koin with our test module
//        startKoin {
//            modules(module {
//                single(named(AndroidCommonDITags.LOGGER)) { mockLogger }
//            })
//        }

        // Make wcKoinApp reference the test Koin instance
//        mockkObject(wcKoinApp)
//        every { wcKoinApp.koin } returns org.koin.core.context.GlobalContext.get().koin
    }

    @After
    fun tearDown() {
        stopKoin()
        clearAllMocks()
    }

    @Test
    fun `findMatchingWalletService returns null when scopedProperties is null`() {
        // Given
        val mockRequest = mockk<EngineDO.Request>()
        val mockSession = mockk<SessionVO>()
        every { mockSession.scopedProperties } returns null

        // When
        val result = WalletServiceFinder.findMatchingWalletService(mockRequest, mockSession)

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
        val result = WalletServiceFinder.findMatchingWalletService(mockRequest, mockSession)

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
        val result = WalletServiceFinder.findMatchingWalletService(mockRequest, mockSession)

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
        val result = WalletServiceFinder.findMatchingWalletService(mockRequest, mockSession)

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
        val result = WalletServiceFinder.findMatchingWalletService(mockRequest, mockSession)

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
        every { mockRequest.method } returns "wallet_getAssets"

        val mockSession = mockk<SessionVO>()
        every { mockSession.scopedProperties } returns scopedProperties

        // When
        val result = WalletServiceFinder.findMatchingWalletService(mockRequest, mockSession)

        // Then
        assertNull(result)
    }
//
//    @Test
//    fun `findWalletService handles missing walletService property properly`() {
//        // Given
//        val scopedProperties = mapOf(
//            "eip155" to "{\"someOtherProperty\":{}}"
//        )
//
//        // When
//        val result = walletServiceFinder.findWalletService("wallet_getAssets", scopedProperties, "eip155")
//
//        // Then
//        assertNull(result)
//    }
//
//    @Test
//    fun `findWalletService handles invalid URL properly`() {
//        // Given
//        val scopedProperties = mapOf(
//            "eip155" to "{\"walletService\":[{\"url\":\"invalid-url\", \"methods\":[\"wallet_getAssets\"]}]}"
//        )
//
//        // When
//        val result = walletServiceFinder.findWalletService("wallet_getAssets", scopedProperties, "eip155")
//
//        // Then
//        assertNull(result)
//    }
//
//    @Test
//    fun `findWalletService handles multiple services and returns first match`() {
//        // Given
//        val scopedProperties = mapOf(
//            "eip155" to "{\"walletService\":[" +
//                    "{\"url\":\"https://example1.com\", \"methods\":[\"other_method\"]}," +
//                    "{\"url\":\"https://example2.com\", \"methods\":[\"wallet_getAssets\"]}," +
//                    "{\"url\":\"https://example3.com\", \"methods\":[\"wallet_getAssets\"]}" +
//                    "]}"
//        )
//
//        // When
//        val result = walletServiceFinder.findWalletService("wallet_getAssets", scopedProperties, "eip155")
//
//        // Then
//        val expectedUrl = URL("https://example2.com")
//        assertEquals(expectedUrl, result)
//    }
}