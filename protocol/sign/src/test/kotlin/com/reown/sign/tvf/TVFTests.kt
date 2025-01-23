package com.reown.sign.tvf

import com.reown.sign.engine.model.tvf.TVF
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.Test

class TVFTests {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val tvf = TVF(moshi)

    @Test
    fun `collect should return null when rpcMethod is not in the allowed list`() {
        // Arrange
        val rpcMethod = "unsupported_method"
        val rpcParams = "{}"
        val chainId = "1"

        // Act
        val result = tvf.collect(rpcMethod, rpcParams, chainId)

        // Assert
        assertNull(result)
    }

    @Test
    fun `collect should parse eth_sendTransaction correctly`() {
        // Arrange
        val rpcMethod = "eth_sendTransaction"
        val rpcParams = "[{\"to\": \"0x1234567890abcdef\", \"from\": \"0x1234567890abcdef\"}]"
        val chainId = "1"

        // Act
        val result = tvf.collect(rpcMethod, rpcParams, chainId)

        // Assert
        assertNotNull(result)
        assertEquals(listOf("eth_sendTransaction"), result?.first)
        assertEquals(listOf("0x1234567890abcdef"), result?.second)
        assertEquals("1", result?.third)
    }

    @Test
    fun `collect should return default value when parsing eth_sendTransaction fails`() {
        // Arrange
        val rpcMethod = "eth_sendTransaction"
        val rpcParams = "{malformed_json}"
        val chainId = "1"

        // Act
        val result = tvf.collect(rpcMethod, rpcParams, chainId)

        // Assert
        assertNotNull(result)
        assertEquals(listOf("eth_sendTransaction"), result?.first)
        assertEquals(listOf(""), result?.second)
        assertEquals("1", result?.third)
    }

    @Test
    fun `collectTxHashes should return the rpcResult for evm and wallet methods`() {
        // Arrange
        val rpcMethod = "eth_sendTransaction"
        val rpcResult = "0x123abc"

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assertEquals(listOf("0x123abc"), result)
    }

    @Test
    fun `collectTxHashes should parse solana_signTransaction and return the signature`() {
        // Arrange
        val rpcMethod = "solana_signTransaction"
        val rpcResult = "{\"signature\": \"0xsignature123\"}"

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assertEquals(listOf("0xsignature123"), result)
    }

    @Test
    fun `collectTxHashes should return null when parsing solana_signTransaction fails`() {
        // Arrange
        val rpcMethod = "solana_signTransaction"
        val rpcResult = "{malformed_json}"

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNull(result)
    }

    @Test
    fun `collectTxHashes should parse solana_signAndSendTransaction and return the signature`() {
        // Arrange
        val rpcMethod = "solana_signAndSendTransaction"
        val rpcResult = "{\"signature\": \"0xsendAndSignSignature\"}"

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assertEquals(listOf("0xsendAndSignSignature"), result)
    }

    @Test
    fun `collectTxHashes should parse solana_signAllTransactions and return all transactions`() {
        // Arrange
        val rpcMethod = "solana_signAllTransactions"
        val rpcResult = "{\"transactions\": [\"tx1\", \"tx2\"]}"

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assertEquals(listOf("tx1", "tx2"), result)
    }

    @Test
    fun `collectTxHashes should return null for unsupported methods`() {
        // Arrange
        val rpcMethod = "unsupported_method"
        val rpcResult = "some_result"

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNull(result)
    }
}