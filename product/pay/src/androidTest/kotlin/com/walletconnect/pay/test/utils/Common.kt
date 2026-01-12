package com.walletconnect.pay.test.utils

import junit.framework.TestCase.fail

internal object Common {
    const val MERCHANT_ID = "gancho-test-collectdata"
    const val API_KEY = "wcp_merchant_1abtcETOqkiPL83kwWsZabB2HPRJCkui"
    const val TEST_ADDRESS = "0xEb52dc9cCE17f1F0Ab0606d846dce183B449033C"
    const val BASE_CHAIN = "eip155:8453"
    const val POLYGON_CHAIN = "eip155:137"
    const val PAY_API_BASE_URL = "https://api.pay.walletconnect.com/"

    val testAccounts = listOf(
        "$BASE_CHAIN:$TEST_ADDRESS",
        "$POLYGON_CHAIN:$TEST_ADDRESS"
    )
}

internal fun globalOnError(error: Throwable) {
    println("Test error: ${error.stackTraceToString()}")
    fail(error.message)
}
