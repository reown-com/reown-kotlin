package com.reown.foundation

import com.tinder.scarlet.WebSocket
import com.reown.foundation.common.model.SubscriptionId
import com.reown.foundation.network.BaseRelayClient
import com.reown.foundation.network.ConnectionLifecycle
import com.reown.foundation.network.ConnectionState
import com.reown.foundation.network.data.service.RelayService
import com.reown.foundation.network.model.Relay
import com.reown.foundation.network.model.RelayDTO
import com.reown.foundation.util.Logger
import com.reown.foundation.util.scope
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class BaseRelayClientTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var client: BaseRelayClient
    private val relayServiceMock = mockk<RelayService>(relaxed = true)
    private val connectionLifecycleMock = mockk<ConnectionLifecycle>(relaxed = true)
    private val loggerMock = mockk<Logger>(relaxed = true)
    private val mockConnectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        client = spyk(object : BaseRelayClient() {
            init {
                this.relayService = relayServiceMock
                this.logger = loggerMock
                scope = testScope
                this.connectionLifecycle = connectionLifecycleMock
            }
        }, recordPrivateCalls = true)
        client.connectionState = mockConnectionState
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `publish invokes relayService publishRequest successfully`() = testScope.runTest {
        val id = 123L

        val topic = "testTopic"
        val message = "testMessage"
        val params = Relay.Model.IrnParams(1, 60, correlationId = 1234L, prompt = true)
        val ack = RelayDTO.Publish.Result.Acknowledgement(123L, result = true)

        val publishRequestSlot = slot<RelayDTO.Publish.Request>()
        coEvery { relayServiceMock.publishRequest(capture(publishRequestSlot)) } just Runs
        coEvery { relayServiceMock.observePublishAcknowledgement() } returns flowOf(ack)
        every { connectionLifecycleMock.reconnect() } just Runs
        coEvery { relayServiceMock.observeWebSocketEvent() } returns flowOf(WebSocket.Event.OnConnectionOpened("Open"))

        client.connectionState.value = ConnectionState.Open
        client.publish(topic, message, params, id)

        coVerify { relayServiceMock.publishRequest(any()) }
        assertEquals(id, publishRequestSlot.captured.id)
    }

    @Test
    fun `test publish success`() = testScope.runTest {
        val topic = "testTopic"
        val message = "testMessage"
        val params = Relay.Model.IrnParams(1, 60, correlationId = 1234L, prompt = true)
        val ack = RelayDTO.Publish.Result.Acknowledgement(123L, result = true)

        coEvery { relayServiceMock.publishRequest(any()) } returns Unit
        coEvery { relayServiceMock.observePublishAcknowledgement() } returns flowOf(ack)
        coEvery { relayServiceMock.observeWebSocketEvent() } returns flowOf(WebSocket.Event.OnConnectionOpened("Open"))

        client.observeResults()
        client.connectionState.value = ConnectionState.Open
        client.publish(topic, message, params, 123L) { result ->
            result.fold(
                onSuccess = {
                    assertEquals(123L, it.id)
                },
                onFailure = { fail(it.message) }
            )
        }

        coVerify { relayServiceMock.publishRequest(any()) }
    }


    @Test
    fun `test publish error due to time out`() = testScope.runTest {
        val topic = "testTopic"
        val message = "testMessage"
        val params = Relay.Model.IrnParams(1, 60, correlationId = 1234L, prompt = true)

        coEvery { relayServiceMock.publishRequest(any()) } returns Unit
        coEvery { relayServiceMock.observePublishAcknowledgement() } returns flow { delay(15000L) }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            client.connectionState.value = ConnectionState.Open
            client.publish(topic, message, params) { result ->
                result.fold(
                    onSuccess = {
                        fail("Should not be successful")
                    },
                    onFailure = {
                        assertTrue(result.exceptionOrNull() is TimeoutCancellationException)
                    }
                )
            }
        }

        advanceUntilIdle()

        coVerify { relayServiceMock.publishRequest(any()) }
    }

    @Test
    fun `test subscribe success`() = testScope.runTest {
        val topic = "testTopic"
        val expectedId = 123L
        val relayDto = RelayDTO.Subscribe.Result.Acknowledgement(id = expectedId, result = SubscriptionId("testId"))

        coEvery { relayServiceMock.subscribeRequest(any()) } returns Unit
        coEvery { relayServiceMock.observeSubscribeAcknowledgement() } returns flowOf(relayDto)

        client.observeResults()
        client.connectionState.value = ConnectionState.Open
        client.subscribe(topic, expectedId) { result ->
            result.fold(
                onSuccess = {
                    assertEquals(expectedId, result.getOrNull()?.id)
                },
                onFailure = { fail(it.message) }
            )
        }

        coVerify { relayServiceMock.subscribeRequest(any()) }
    }

    @Test
    fun `test subscribe failure due to timeout`() = testScope.runTest() {
        val topic = "testTopic"

        coEvery { relayServiceMock.subscribeRequest(any()) } returns Unit
        coEvery { relayServiceMock.observeSubscribeAcknowledgement() } returns flow { delay(10000L) }

        client.connectionState.value = ConnectionState.Open
        client.subscribe(topic) { result ->
            result.fold(
                onSuccess = {
                    fail("Should not be successful")
                },
                onFailure = {
                    assertTrue(result.exceptionOrNull() is TimeoutCancellationException)
                }
            )
        }

        testScheduler.apply { advanceTimeBy(5000); runCurrent() }

        coVerify { relayServiceMock.subscribeRequest(any()) }
    }

    @Test
    fun `test batch subscribe success`() = testScope.runTest {
        val topics = listOf("testTopic")
        val expectedId = 123L
        val relayDto = RelayDTO.BatchSubscribe.Result.Acknowledgement(id = expectedId, result = listOf("testId"))

        coEvery { relayServiceMock.batchSubscribeRequest(any()) } returns Unit
        coEvery { relayServiceMock.observeBatchSubscribeAcknowledgement() } returns flowOf(relayDto)
        coEvery { relayServiceMock.observeWebSocketEvent() } returns flowOf(WebSocket.Event.OnConnectionOpened("Open"))

        // Ensure topics are not already in ackedTopics
        client.ackedTopics.clear()
        client.observeResults()
        client.connectionState.value = ConnectionState.Open
        client.batchSubscribe(topics, expectedId) { result ->
            result.fold(
                onSuccess = {
                    assertEquals(expectedId, result.getOrNull()?.id)
                },
                onFailure = { fail(it.message) }
            )
        }

        coVerify { relayServiceMock.batchSubscribeRequest(any()) }
    }

    @Test
    fun `test batch subscribe failure due to timeout`() = testScope.runTest {
        val topics = listOf("testTopic")

        coEvery { relayServiceMock.batchSubscribeRequest(any()) } returns Unit
        coEvery { relayServiceMock.observeBatchSubscribeAcknowledgement() } returns flow { delay(10000L) }
        coEvery { relayServiceMock.observeWebSocketEvent() } returns flowOf(WebSocket.Event.OnConnectionOpened("Open"))

        // Ensure topics are not already in ackedTopics
        client.ackedTopics.clear()
        client.connectionState.value = ConnectionState.Open

        client.batchSubscribe(topics) { result ->
            result.fold(
                onSuccess = {
                    fail("Should not be successful")
                },
                onFailure = {
                    assertTrue(result.exceptionOrNull() is TimeoutCancellationException)
                }
            )
        }

        testScheduler.apply { advanceTimeBy(5000); runCurrent() }

        coVerify { relayServiceMock.batchSubscribeRequest(any()) }
    }

    @Test
    fun `test unsubscribe success`() = testScope.runTest {
        val topic = "testTopic"
        val expectedId = 123L
        val relayDto = RelayDTO.Unsubscribe.Result.Acknowledgement(id = expectedId, result = true)

        coEvery { relayServiceMock.unsubscribeRequest(any()) } returns Unit
        coEvery { relayServiceMock.observeUnsubscribeAcknowledgement() } returns flowOf(relayDto)

        client.observeResults()
        client.connectionState.value = ConnectionState.Open
        client.unsubscribe(topic, "subsId", expectedId) { result ->
            result.fold(
                onSuccess = {
                    assertEquals(expectedId, result.getOrNull()?.id)
                },
                onFailure = { fail(it.message) }
            )
        }

        coVerify { relayServiceMock.unsubscribeRequest(any()) }
    }

    @Test
    fun `test unsubscribe failure`() = testScope.runTest {
        val topic = "testTopic"

        coEvery { relayServiceMock.subscribeRequest(any()) } returns Unit
        coEvery { relayServiceMock.observeSubscribeAcknowledgement() } returns flow { delay(10000L) }

        client.connectionState.value = ConnectionState.Open
        client.subscribe(topic) { result ->
            result.fold(
                onSuccess = {
                    fail("Should not be successful")
                },
                onFailure = {
                    assertTrue(result.exceptionOrNull() is TimeoutCancellationException)
                }
            )
        }

        testScheduler.apply { advanceTimeBy(5000); runCurrent() }

        coVerify { relayServiceMock.subscribeRequest(any()) }
    }

    @Test
    fun `test proposeSession success`() = testScope.runTest {
        val pairingTopic = com.reown.foundation.common.model.Topic("testPairingTopic")
        val sessionProposal = "testSessionProposal"
        val correlationId = 1234L
        val expectedId = 123L
        val relayDto = RelayDTO.ProposeSession.Result.Acknowledgement(id = expectedId, result = true)

        coEvery { relayServiceMock.proposeSessionRequest(any()) } returns Unit
        coEvery { relayServiceMock.observeProposeSessionAcknowledgement() } returns flowOf(relayDto)
        coEvery { relayServiceMock.observeWebSocketEvent() } returns flowOf(WebSocket.Event.OnConnectionOpened("Open"))

        client.observeResults()
        client.connectionState.value = ConnectionState.Open
        client.proposeSession(pairingTopic, sessionProposal, correlationId, expectedId) { result ->
            result.fold(
                onSuccess = {
                    assertEquals(expectedId, result.getOrNull()?.id)
                },
                onFailure = { fail(it.message) }
            )
        }

        coVerify { relayServiceMock.proposeSessionRequest(any()) }
    }

    @Test
    fun `test proposeSession failure due to timeout`() = testScope.runTest {
        val pairingTopic = com.reown.foundation.common.model.Topic("testPairingTopic")
        val sessionProposal = "testSessionProposal"
        val correlationId = 1234L

        coEvery { relayServiceMock.proposeSessionRequest(any()) } returns Unit
        coEvery { relayServiceMock.observeProposeSessionAcknowledgement() } returns flow { delay(10000L) }

        client.connectionState.value = ConnectionState.Open
        client.proposeSession(pairingTopic, sessionProposal, correlationId) { result ->
            result.fold(
                onSuccess = {
                    fail("Should not be successful")
                },
                onFailure = {
                    assertTrue(result.exceptionOrNull() is TimeoutCancellationException)
                }
            )
        }

        testScheduler.apply { advanceTimeBy(5000); runCurrent() }

        coVerify { relayServiceMock.proposeSessionRequest(any()) }
    }

    @Test
    fun `test proposeSession error response`() = testScope.runTest {
        val pairingTopic = com.reown.foundation.common.model.Topic("testPairingTopic")
        val sessionProposal = "testSessionProposal"
        val correlationId = 1234L
        val expectedId = 123L
        val errorMessage = "Session proposal error"
        val relayDto = RelayDTO.ProposeSession.Result.JsonRpcError(
            jsonrpc = "2.0",
            error = RelayDTO.Error(code = -1, message = errorMessage),
            id = expectedId
        )

        coEvery { relayServiceMock.proposeSessionRequest(any()) } returns Unit
        coEvery { relayServiceMock.observeProposeSessionError() } returns flowOf(relayDto)
        coEvery { relayServiceMock.observeWebSocketEvent() } returns flowOf(WebSocket.Event.OnConnectionOpened("Open"))

        client.observeResults()
        client.connectionState.value = ConnectionState.Open
        client.proposeSession(pairingTopic, sessionProposal, correlationId, expectedId) { result ->
            result.fold(
                onSuccess = {
                    fail("Should not be successful")
                },
                onFailure = {
                    assertTrue(it.message?.contains(errorMessage) == true)
                }
            )
        }

        coVerify { relayServiceMock.proposeSessionRequest(any()) }
    }

    @Test
    fun `test approveSession success`() = testScope.runTest {
        val pairingTopic = com.reown.foundation.common.model.Topic("testPairingTopic")
        val sessionTopic = com.reown.foundation.common.model.Topic("testSessionTopic")
        val sessionProposalResponse = "testSessionProposalResponse"
        val sessionSettlementRequest = "testSessionSettlementRequest"
        val correlationId = 1234L
        val expectedId = 123L
        val relayDto = RelayDTO.ApproveSession.Result.Acknowledgement(id = expectedId, result = true)

        coEvery { relayServiceMock.approveSessionRequest(any()) } returns Unit
        coEvery { relayServiceMock.observeApproveSessionAcknowledgement() } returns flowOf(relayDto)
        coEvery { relayServiceMock.observeWebSocketEvent() } returns flowOf(WebSocket.Event.OnConnectionOpened("Open"))

        client.observeResults()
        client.connectionState.value = ConnectionState.Open
        client.approveSession(pairingTopic, sessionTopic, sessionProposalResponse, sessionSettlementRequest, correlationId, expectedId) { result ->
            result.fold(
                onSuccess = {
                    assertEquals(expectedId, result.getOrNull()?.id)
                },
                onFailure = { fail(it.message) }
            )
        }

        coVerify { relayServiceMock.approveSessionRequest(any()) }
    }

    @Test
    fun `test approveSession failure due to timeout`() = testScope.runTest {
        val pairingTopic = com.reown.foundation.common.model.Topic("testPairingTopic")
        val sessionTopic = com.reown.foundation.common.model.Topic("testSessionTopic")
        val sessionProposalResponse = "testSessionProposalResponse"
        val sessionSettlementRequest = "testSessionSettlementRequest"
        val correlationId = 1234L

        coEvery { relayServiceMock.approveSessionRequest(any()) } returns Unit
        coEvery { relayServiceMock.observeApproveSessionAcknowledgement() } returns flow { delay(10000L) }

        client.connectionState.value = ConnectionState.Open
        client.approveSession(pairingTopic, sessionTopic, sessionProposalResponse, sessionSettlementRequest, correlationId) { result ->
            result.fold(
                onSuccess = {
                    fail("Should not be successful")
                },
                onFailure = {
                    assertTrue(result.exceptionOrNull() is TimeoutCancellationException)
                }
            )
        }

        testScheduler.apply { advanceTimeBy(5000); runCurrent() }

        coVerify { relayServiceMock.approveSessionRequest(any()) }
    }

    @Test
    fun `test approveSession error response`() = testScope.runTest {
        val pairingTopic = com.reown.foundation.common.model.Topic("testPairingTopic")
        val sessionTopic = com.reown.foundation.common.model.Topic("testSessionTopic")
        val sessionProposalResponse = "testSessionProposalResponse"
        val sessionSettlementRequest = "testSessionSettlementRequest"
        val correlationId = 1234L
        val expectedId = 123L
        val errorMessage = "Session approve error"
        val relayDto = RelayDTO.ApproveSession.Result.JsonRpcError(
            jsonrpc = "2.0",
            error = RelayDTO.Error(code = -1, message = errorMessage),
            id = expectedId
        )

        coEvery { relayServiceMock.approveSessionRequest(any()) } returns Unit
        coEvery { relayServiceMock.observeApproveSessionError() } returns flowOf(relayDto)
        coEvery { relayServiceMock.observeWebSocketEvent() } returns flowOf(WebSocket.Event.OnConnectionOpened("Open"))

        client.observeResults()
        client.connectionState.value = ConnectionState.Open
        client.approveSession(pairingTopic, sessionTopic, sessionProposalResponse, sessionSettlementRequest, correlationId, expectedId) { result ->
            result.fold(
                onSuccess = {
                    fail("Should not be successful")
                },
                onFailure = {
                    assertTrue(it.message?.contains(errorMessage) == true)
                }
            )
        }

        coVerify { relayServiceMock.approveSessionRequest(any()) }
    }
}