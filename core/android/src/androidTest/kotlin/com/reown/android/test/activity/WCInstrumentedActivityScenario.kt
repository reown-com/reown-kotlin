package com.reown.android.test.activity

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.reown.android.BuildConfig
import com.reown.android.internal.common.scope
import com.reown.android.test.utils.TestClient
import com.reown.foundation.network.model.Relay
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

class WCInstrumentedActivityScenario : TestRule {
    private var scenario: ActivityScenario<InstrumentedTestActivity>? = null
    private var scenarioLaunched: Boolean = false
    private val latch = CountDownLatch(1)
    private val testScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                beforeAll()
                base.evaluate()
                afterAll()
            }
        }
    }

    private fun initLogging() {
        if (Timber.treeCount == 0) {
            Timber.plant(
                object : Timber.DebugTree() {
                    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                        super.log(priority, "WalletConnectV2", message, t)
                    }
                }
            )
        }
    }

    private fun beforeAll() {
        runBlocking(testScope.coroutineContext) {
            initLogging()
            Timber.d("init")
        }
    }

    private fun afterAll() {
        Timber.d("afterAll")
        scenario?.close()
    }

    fun launch(timeoutSeconds: Long = 1, testCodeBlock: suspend (scope: CoroutineScope) -> Unit) {
        require(!scenarioLaunched) { "Scenario has already been launched!" }

        scenario = ActivityScenario.launch(InstrumentedTestActivity::class.java)
        scenarioLaunched = true

        scenario?.moveToState(Lifecycle.State.RESUMED)
        assert(scenario?.state?.isAtLeast(Lifecycle.State.RESUMED) == true)

        testScope.launch { testCodeBlock(testScope) }

        try {
            assertTrue(latch.await(timeoutSeconds, TimeUnit.SECONDS))
        } catch (exception: Exception) {
            fail(exception.message)
        }
    }

    fun closeAsSuccess() {
        latch.countDown()
    }
}