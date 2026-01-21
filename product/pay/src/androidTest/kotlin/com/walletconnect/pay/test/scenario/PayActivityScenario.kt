package com.walletconnect.pay.test.scenario

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.walletconnect.pay.test.activity.InstrumentedTestActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch

open class PayActivityScenario {
    private var scenario: ActivityScenario<InstrumentedTestActivity>? = null
    private var scenarioLaunched: Boolean = false
    private val latch = CountDownLatch(1)
    private val testScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    fun launch(testCodeBlock: suspend () -> Unit) {
        require(!scenarioLaunched) { "Scenario has already been launched!" }

        scenario = ActivityScenario.launch(InstrumentedTestActivity::class.java)
        scenarioLaunched = true

        scenario?.moveToState(Lifecycle.State.RESUMED)
        assert(scenario?.state?.isAtLeast(Lifecycle.State.RESUMED) == true)

        testScope.launch { testCodeBlock() }
    }

    fun closeAsSuccess() {
        latch.countDown()
    }

    fun afterAll() {
        println("afterAll")
        scenario?.close()
    }
}
