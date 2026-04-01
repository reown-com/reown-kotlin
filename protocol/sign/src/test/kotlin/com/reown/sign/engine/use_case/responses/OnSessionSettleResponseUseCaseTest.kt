package com.reown.sign.engine.use_case.responses

import com.reown.android.internal.common.JsonRpcResponse
import com.reown.android.internal.common.crypto.kmr.KeyManagementRepository
import com.reown.android.internal.common.model.WCResponse
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.foundation.common.model.Topic
import com.reown.foundation.util.Logger
import com.reown.sign.common.model.vo.sequence.SessionVO
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.storage.pending_session.PendingSessionTopicRepository
import com.reown.sign.storage.sequence.SessionStorageRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnSessionSettleResponseUseCaseTest {
    private val sessionStorageRepository = mockk<SessionStorageRepository>(relaxed = true)
    private val jsonRpcInteractor = mockk<RelayJsonRpcInteractorInterface>(relaxed = true)
    private val metadataStorageRepository = mockk<MetadataStorageRepositoryInterface>(relaxed = true)
    private val crypto = mockk<KeyManagementRepository>(relaxed = true)
    private val pendingSessionTopicRepository = PendingSessionTopicRepository()
    private val logger = mockk<Logger>(relaxed = true)

    private val useCase = OnSessionSettleResponseUseCase(
        sessionStorageRepository = sessionStorageRepository,
        jsonRpcInteractor = jsonRpcInteractor,
        metadataStorageRepository = metadataStorageRepository,
        crypto = crypto,
        pendingSessionTopicRepository = pendingSessionTopicRepository,
        logger = logger
    )

    private val testTopic = Topic("test_session_topic")
    private val testSession = mockk<SessionVO>(relaxed = true)

    @Before
    fun setUp() {
        every { sessionStorageRepository.isSessionValid(testTopic) } returns true
        every { sessionStorageRepository.getSessionWithoutMetadataByTopic(testTopic) } returns testSession
        every { testSession.topic } returns testTopic
        every { testSession.copy(any(), any()) } returns testSession
    }

    @Test
    fun `error path cleans up session and keys even when unsubscribe fails`() = runTest {
        val errorResponse = JsonRpcResponse.JsonRpcError(
            id = 1L,
            error = JsonRpcResponse.Error(code = -1, message = "rejected")
        )
        val wcResponse = WCResponse(
            topic = testTopic,
            method = "wc_sessionSettle",
            response = errorResponse,
            params = mockk(relaxed = true)
        )

        every { jsonRpcInteractor.unsubscribe(any(), any(), any()) } answers {
            val onFailure = thirdArg<(Throwable) -> Unit>()
            onFailure(Throwable("Network error"))
        }

        useCase(wcResponse)

        verify { sessionStorageRepository.deleteSession(testTopic) }
        verify { crypto.removeKeys(testTopic.value) }
    }

    @Test
    fun `error path emits SettledSessionResponse Error event`() = runTest {
        val errorResponse = JsonRpcResponse.JsonRpcError(
            id = 1L,
            error = JsonRpcResponse.Error(code = -1, message = "rejected")
        )
        val wcResponse = WCResponse(
            topic = testTopic,
            method = "wc_sessionSettle",
            response = errorResponse,
            params = mockk(relaxed = true)
        )

        val events = mutableListOf<com.reown.android.internal.common.model.type.EngineEvent>()
        val job = launch {
            useCase.events.collect { events.add(it) }
        }
        advanceUntilIdle()

        useCase(wcResponse)
        advanceUntilIdle()

        assertTrue(events.any { it is EngineDO.SettledSessionResponse.Error })
        job.cancel()
    }
}
