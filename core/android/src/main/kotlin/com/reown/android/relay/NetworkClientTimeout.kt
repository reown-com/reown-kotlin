package com.reown.android.relay

import java.util.concurrent.TimeUnit

data class NetworkClientTimeout(
    val timeout: Long,
    val timeUnit: TimeUnit
) {
    private val timeoutInMillis: Long by lazy {
        TimeUnit.MILLISECONDS.convert(timeout, timeUnit)
    }

    init {
        require(timeoutInMillis in MIN_TIMEOUT_LIMIT..MAX_TIMEOUT_LIMIT) {
            "Timeout must be in range of $MIN_TIMEOUT_LIMIT..$MAX_TIMEOUT_LIMIT milliseconds"
        }
    }

    companion object {
        private const val MIN_TIMEOUT_LIMIT = 15_000L
        private const val MAX_TIMEOUT_LIMIT = 60_000L

        fun getDefaultNetworkTimeout() = NetworkClientTimeout(
            timeout = MIN_TIMEOUT_LIMIT,
            timeUnit = TimeUnit.MILLISECONDS
        )
    }
}
