package com.walletconnect.pay.test.utils

import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.fail

internal object Common {
    val MERCHANT_ID: String by lazy {
        InstrumentationRegistry.getArguments().getString("MERCHANT_ID")
            ?: error("MERCHANT_ID environment variable not set")
    }
    val MERCHANT_API_KEY: String by lazy {
        InstrumentationRegistry.getArguments().getString("MERCHANT_API_KEY")
            ?: error("MERCHANT_API_KEY environment variable not set")
    }
    const val BASE_CHAIN = "eip155:8453"
    const val POLYGON_CHAIN = "eip155:137"
    const val PAY_API_BASE_URL = "https://api.pay.walletconnect.com/"

    // The test wallet address is derived from TEST_WALLET_PRIVATE_KEY (via the signer),
    // so it always matches whatever wallet that key points to — single source of truth,
    // nothing to keep in sync. Accessed at test time, after TestClient has initialized.
    val testAddress: String
        get() = TestClient.signer.address

    val testAccounts: List<String>
        get() = listOf(
            "$BASE_CHAIN:$testAddress",
            "$POLYGON_CHAIN:$testAddress"
        )
}

internal fun globalOnError(error: Throwable) {
    println("Test error: ${error.stackTraceToString()}")
    fail(error.message)
}
