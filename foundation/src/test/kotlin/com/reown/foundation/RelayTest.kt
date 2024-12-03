package com.reown.foundation

import com.reown.foundation.crypto.data.repository.ClientIdJwtRepository
import com.reown.foundation.di.FoundationDITags
import com.reown.foundation.di.cryptoModule
import com.reown.foundation.di.foundationCommonModule
import com.reown.foundation.di.networkModule
import com.reown.foundation.network.BaseRelayClient
import com.reown.foundation.network.RelayInterface
import com.reown.foundation.network.model.Relay
import com.reown.util.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.koin.core.KoinApplication
import org.koin.core.qualifier.named
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

sealed class TestState {
    object Idle : TestState()
    object Success : TestState()
    data class Error(val message: String) : TestState()
}

@ExperimentalCoroutinesApi
class RelayTest {
    private val testProjectId: String = requireNotNull(System.getProperty("TEST_PROJECT_ID"))
    private val testProjectId2: String = requireNotNull(System.getProperty("TEST_PROJECT_ID2"))
    private val testRelayUrl: String = requireNotNull(System.getProperty("TEST_RELAY_URL"))
    private var serverUrl = "$testRelayUrl?projectId=$testProjectId"
    private val sdkVersion: String = System.getProperty("SDK_VERSION") + "-relayTest"
    private val testJob: CompletableJob = SupervisorJob()
    private val testScope: CoroutineScope = CoroutineScope(testJob + Dispatchers.IO)


    @ExperimentalTime
    @Test
    fun `Connect with empty packageName when soma packageName is configured in Cloud - successful connection`() {
        val testState = MutableStateFlow<TestState>(TestState.Idle)
        val (clientA: RelayInterface, clientB: RelayInterface) = initTwoClients(packageName = "")

        //Await connection
        val connectionTime = measureTime { awaitConnection(clientA, clientB) }.inWholeMilliseconds
        println("Connection time: $connectionTime ms")
        testState.compareAndSet(expect = TestState.Idle, update = TestState.Success)

        //Lock until is finished or timed out
        runBlocking {
            val start = System.currentTimeMillis()
            // Await test finish or check if timeout occurred
            while (testState.value is TestState.Idle && !didTimeout(start, 60000L)) {
                delay(10)
            }

            // Success or fail or idle
            when (testState.value) {
                is TestState.Success -> return@runBlocking
                is TestState.Error -> fail((testState.value as TestState.Error).message)
                is TestState.Idle -> fail("Test timeout")
            }
        }
    }

    @ExperimentalTime
    @Test
    fun `Connect with not whitelisted packageName when some packageName is already configured in Cloud - return an error`() {
        val testState = MutableStateFlow<TestState>(TestState.Idle)
        val (clientA: RelayInterface, clientB: RelayInterface) = initTwoClients(packageName = "com.test.failure")

        clientA.eventsFlow.onEach { event ->
            when (event) {
                is Relay.Model.Event.OnConnectionFailed -> {
                    if (event.throwable.message?.contains("403") == true) {
                        testState.compareAndSet(expect = TestState.Idle, update = TestState.Success)
                    }
                }

                else -> {}
            }
        }.launchIn(testScope)

        //Lock until is finished or timed out
        runBlocking {
            val start = System.currentTimeMillis()
            // Await test finish or check if timeout occurred
            while (testState.value is TestState.Idle && !didTimeout(start, 60000L)) {
                delay(10)
            }

            // Success or fail or idle
            when (testState.value) {
                is TestState.Success -> return@runBlocking
                is TestState.Error -> fail((testState.value as TestState.Error).message)
                is TestState.Idle -> fail("Test timeout")
            }
        }
    }

    @ExperimentalTime
    @Test
    fun `Connect with packageName when no packageName is configured in Cloud - successful connection`() {
        serverUrl = "$testRelayUrl?projectId=$testProjectId2"
        val testState = MutableStateFlow<TestState>(TestState.Idle)
        val (clientA: RelayInterface, clientB: RelayInterface) = initTwoClients(packageName = "com.test")

        //Await connection
        val connectionTime = measureTime { awaitConnection(clientA, clientB) }.inWholeMilliseconds
        println("Connection time: $connectionTime ms")
        testState.compareAndSet(expect = TestState.Idle, update = TestState.Success)

        //Lock until is finished or timed out
        runBlocking {
            val start = System.currentTimeMillis()
            // Await test finish or check if timeout occurred
            while (testState.value is TestState.Idle && !didTimeout(start, 60000L)) {
                delay(10)
            }

            // Success or fail or idle
            when (testState.value) {
                is TestState.Success -> return@runBlocking
                is TestState.Error -> fail((testState.value as TestState.Error).message)
                is TestState.Idle -> fail("Test timeout")
            }
        }
    }

    @ExperimentalTime
    @Test
    fun `One client sends unencrypted message, second one receives it`() {
        val testState = MutableStateFlow<TestState>(TestState.Idle)
        val testTopic = Random.nextBytes(32).bytesToHex()
        val testMessage = "testMessage"
        val (clientA: RelayInterface, clientB: RelayInterface) = initTwoClients()

        // Listen to incoming messages/requests
        clientB.subscriptionRequest.onEach {
            println("ClientB subscriptionRequest: $it")
            assertEquals(testMessage, it.params.subscriptionData.message)
            testState.compareAndSet(expect = TestState.Idle, update = TestState.Success)
        }.launchIn(testScope)

        //Await connection
        measureAwaitingForConnection(clientA, clientB)

        //Subscribe to topic
        clientB.subscribe(testTopic) { result ->
            result.fold(
                onSuccess = {
                    println("ClientB subscribe on topic: $testTopic")
                },
                onFailure = { error ->
                    println("ClientB failed to subscribe on topic: $testTopic. Message: ${error.message}")
                }
            )
        }

        clientA.publish(testTopic, testMessage, Relay.Model.IrnParams(1114, 300)) { result ->
            result.fold(
                onSuccess = { println("ClientA publish on topic: $testTopic; message: $testMessage") },
                onFailure = { error ->
                    println("ClientA failed to publish on topic: $testTopic. Message: ${error.message}")
                }
            )
        }

        //Lock until is finished or timed out
        runBlocking {
            val start = System.currentTimeMillis()
            // Await test finish or check if timeout occurred
            while (testState.value is TestState.Idle && !didTimeout(start, 60000L)) {
                delay(10)
            }

            // Success or fail or idle
            when (testState.value) {
                is TestState.Success -> return@runBlocking
                is TestState.Error -> fail((testState.value as TestState.Error).message)
                is TestState.Idle -> fail("Test timeout")
            }
        }
    }

    @ExperimentalTime
    @Test
    fun `One client sends unencrypted message with too small ttl and receives error from relay`() {
        val testState = MutableStateFlow<TestState>(TestState.Idle)
        val testTopic = Random.nextBytes(32).bytesToHex()
        val testMessage = "testMessage"
        val ttl: Long = 1
        val (clientA: RelayInterface, clientB: RelayInterface) = initTwoClients()

        //Await connection
        measureAwaitingForConnection(clientA, clientB)

        //Publish message
        clientA.publish(testTopic, testMessage, Relay.Model.IrnParams(1114, ttl)) { result ->
            result.fold(
                onSuccess = {
                    testState.compareAndSet(expect = TestState.Idle, update = TestState.Error("ClientA publish on topic: $testTopic with ttl: $ttl"))
                },
                onFailure = {
                    testState.compareAndSet(expect = TestState.Idle, update = TestState.Success)
                }
            )
        }

        //Lock until is finished or timed out
        runBlocking {
            val start = System.currentTimeMillis()
            // Await test finish or check if timeout occurred
            while (testState.value is TestState.Idle && !didTimeout(start, 60000L)) {
                delay(10)
            }

            // Success or fail or idle
            when (testState.value) {
                is TestState.Success -> return@runBlocking
                is TestState.Error -> fail((testState.value as TestState.Error).message)
                is TestState.Idle -> fail("Test timeout")
            }
        }
    }

    @ExperimentalTime
    private fun measureAwaitingForConnection(clientA: RelayInterface, clientB: RelayInterface) {
        println("Connection established after ${measureTime { awaitConnection(clientA, clientB) }.inWholeMilliseconds} ms with: $testRelayUrl")
    }

    private fun startLoggingClientEventsFlow(client: RelayInterface, tag: String) =
        client.eventsFlow.onEach { println("$tag eventsFlow: $it") }.launchIn(testScope)

    private fun initTwoClients(packageName: String = "com.reown.sample.wallet"): Pair<RelayInterface, RelayInterface> {
        val koinAppA: KoinApplication = KoinApplication.init()
            .apply { modules(foundationCommonModule(), cryptoModule()) }.also { koinApp ->
                val jwt = koinApp.koin.get<ClientIdJwtRepository>().generateJWT(testRelayUrl) { clientId ->
                    println("ClientA id: $clientId")
                }
                koinApp.modules(networkModule(serverUrl.addUserAgent(sdkVersion), sdkVersion, jwt, packageName))
            }

        val koinAppB: KoinApplication = KoinApplication.init()
            .apply { modules(foundationCommonModule(), cryptoModule()) }.also { koinApp ->
                val jwt = koinApp.koin.get<ClientIdJwtRepository>().generateJWT(testRelayUrl) { clientId ->
                    println("ClientB id: $clientId")
                }
                koinApp.modules(networkModule(serverUrl.addUserAgent(sdkVersion), sdkVersion, jwt, packageName))
            }

        val clientA: BaseRelayClient = koinAppA.koin.get()
        val clientB: BaseRelayClient = koinAppB.koin.get()

        clientA.relayService = koinAppA.koin.get(named(FoundationDITags.RELAY_SERVICE))
        clientB.relayService = koinAppB.koin.get(named(FoundationDITags.RELAY_SERVICE))

        clientA.isLoggingEnabled = true
        clientB.isLoggingEnabled = true

        clientA.observeResults()
        clientB.observeResults()

        startLoggingClientEventsFlow(clientA, "ClientA")
        startLoggingClientEventsFlow(clientB, "ClientB")

        return (clientA to clientB)
    }

    private fun didTimeout(start: Long, timeout: Long): Boolean = System.currentTimeMillis() - start > timeout

    private fun awaitConnection(clientA: RelayInterface, clientB: RelayInterface) = runBlocking {
        val isClientAReady = MutableStateFlow(false)
        val isClientBReady = MutableStateFlow(false)
        val areBothReady: StateFlow<Boolean> =
            combine(isClientAReady, isClientBReady) { clientA: Boolean, clientB: Boolean -> clientA && clientB }
                .stateIn(testScope, SharingStarted.Eagerly, false)

        val clientAJob = clientA.eventsFlow.onEach { event ->
            when (event) {
                is Relay.Model.Event.OnConnectionOpened<*> -> isClientAReady.compareAndSet(expect = false, update = true)
                else -> {}
            }
        }.launchIn(testScope)

        val clientBJob = clientB.eventsFlow.onEach { event ->
            when (event) {
                is Relay.Model.Event.OnConnectionOpened<*> -> isClientBReady.compareAndSet(expect = false, update = true)
                else -> {}
            }
        }.launchIn(testScope)

        //Lock until is finished or timed out
        val start = System.currentTimeMillis()
        while (!areBothReady.value && !didTimeout(start, 10000L)) {
            delay(10)
        }

        if (didTimeout(start, 50000L)) {
            throw Exception("Unable to establish socket connection")
        }

        clientAJob.cancel()
        clientBJob.cancel()
    }
}