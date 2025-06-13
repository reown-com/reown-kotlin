package com.reown.sign.test.scenario

import com.reown.android.internal.common.scope
import com.reown.foundation.network.model.Relay
import com.reown.sign.test.BuildConfig
import com.reown.sign.test.utils.TestClient
import junit.framework.TestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class HybridAppInstrumentedActivityScenario : TestRule, SignActivityScenario() {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                beforeAll()
                base.evaluate()
                afterAll()
            }
        }
    }

    private fun beforeAll() {
        runBlocking {
            initLogging()
            val timeoutDuration = BuildConfig.TEST_TIMEOUT_SECONDS.seconds
            fun isEverythingReady() =
                TestClient.Wallet.isInitialized.value && TestClient.Dapp.isInitialized.value && TestClient.Hybrid.isInitialized.value

            runCatching {
                withTimeout(timeoutDuration) {
                    while (!isEverythingReady()) {
                        delay(100)
                    }
                }
            }.fold(
                onSuccess = { Timber.d("Connection established and peers initialized successfully") },
                onFailure = { TestCase.fail("Unable to establish connection OR initialize peers within $timeoutDuration") }
            )
        }
    }
}