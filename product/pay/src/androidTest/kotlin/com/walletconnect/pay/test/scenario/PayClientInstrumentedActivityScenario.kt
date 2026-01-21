package com.walletconnect.pay.test.scenario

import com.walletconnect.pay.test.utils.TestClient
import junit.framework.TestCase.fail
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.time.Duration.Companion.seconds

class PayClientInstrumentedActivityScenario : TestRule, PayActivityScenario() {
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
            val timeoutDuration = 30.seconds

            // First, check if there was an initialization error
            val initError = TestClient.initializationError
            if (initError != null) {
                fail("TestClient initialization failed:\n$initError")
                return@runBlocking
            }

            // Wait for initialization to complete
            runCatching {
                withTimeout(timeoutDuration) {
                    while (!TestClient.isInitialized.value) {
                        // Check for errors during wait
                        TestClient.initializationError?.let { error ->
                            fail("TestClient initialization failed:\n$error")
                            return@withTimeout
                        }
                        delay(100)
                    }
                }
            }.fold(
                onSuccess = { println("Pay SDK initialized successfully") },
                onFailure = {
                    val errorMsg = TestClient.initializationError ?: "Unknown error (timeout)"
                    fail("Unable to initialize Pay SDK within $timeoutDuration.\nError: $errorMsg")
                }
            )
        }
    }
}
