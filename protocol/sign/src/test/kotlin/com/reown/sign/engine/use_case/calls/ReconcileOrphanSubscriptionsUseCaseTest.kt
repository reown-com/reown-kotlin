package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.crypto.kmr.KeyManagementRepository
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.foundation.common.model.Topic
import com.reown.foundation.util.Logger
import com.reown.sign.common.model.vo.sequence.SessionVO
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.storage.authenticate.AuthenticateResponseTopicRepository
import com.reown.sign.storage.pending_session.PendingSessionTopicRepository
import com.reown.sign.storage.sequence.SessionStorageRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ReconcileOrphanSubscriptionsUseCaseTest {
    private val jsonRpcInteractor = mockk<RelayJsonRpcInteractorInterface>(relaxed = true)
    private val sessionStorageRepository = mockk<SessionStorageRepository>(relaxed = true)
    private val authenticateResponseTopicRepository = mockk<AuthenticateResponseTopicRepository>(relaxed = true)
    private val pendingSessionTopicRepository = PendingSessionTopicRepository()
    private val getPairingsUseCase = mockk<GetPairingsUseCaseInterface>(relaxed = true)
    private val crypto = mockk<KeyManagementRepository>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)

    private val useCase = ReconcileOrphanSubscriptionsUseCase(
        jsonRpcInteractor = jsonRpcInteractor,
        sessionStorageRepository = sessionStorageRepository,
        authenticateResponseTopicRepository = authenticateResponseTopicRepository,
        pendingSessionTopicRepository = pendingSessionTopicRepository,
        getPairingsUseCase = getPairingsUseCase,
        crypto = crypto,
        logger = logger
    )

    @Before
    fun setUp() {
        every { sessionStorageRepository.getListOfSessionVOsWithoutMetadata() } returns emptyList()
        coEvery { authenticateResponseTopicRepository.getResponseTopics() } returns emptyList()
        coEvery { getPairingsUseCase.getListOfSettledPairings() } returns emptyList()
    }

    @Test
    fun `should unsubscribe and remove keys for orphan topics`() = runTest {
        every { jsonRpcInteractor.getSubscriptionTopics() } returns setOf("session1", "orphan1", "orphan2")

        val sessionVO = mockk<SessionVO>(relaxed = true)
        every { sessionVO.topic } returns Topic("session1")
        every { sessionStorageRepository.getListOfSessionVOsWithoutMetadata() } returns listOf(sessionVO)

        useCase.reconcile()

        verify { crypto.removeKeys("orphan1") }
        verify { crypto.removeKeys("orphan2") }
        verify { jsonRpcInteractor.unsubscribe(Topic("orphan1"), any(), any()) }
        verify { jsonRpcInteractor.unsubscribe(Topic("orphan2"), any(), any()) }
    }

    @Test
    fun `should not unsubscribe session topics`() = runTest {
        every { jsonRpcInteractor.getSubscriptionTopics() } returns setOf("session1")

        val sessionVO = mockk<SessionVO>(relaxed = true)
        every { sessionVO.topic } returns Topic("session1")
        every { sessionStorageRepository.getListOfSessionVOsWithoutMetadata() } returns listOf(sessionVO)

        useCase.reconcile()

        verify(exactly = 0) { jsonRpcInteractor.unsubscribe(Topic("session1"), any(), any()) }
        verify(exactly = 0) { crypto.removeKeys("session1") }
    }

    @Test
    fun `should not unsubscribe pairing topics`() = runTest {
        every { jsonRpcInteractor.getSubscriptionTopics() } returns setOf("pairing1")
        coEvery { getPairingsUseCase.getListOfSettledPairings() } returns listOf(
            EngineDO.PairingSettle(Topic("pairing1"), null)
        )

        useCase.reconcile()

        verify(exactly = 0) { jsonRpcInteractor.unsubscribe(Topic("pairing1"), any(), any()) }
    }

    @Test
    fun `should not unsubscribe auth response topics`() = runTest {
        every { jsonRpcInteractor.getSubscriptionTopics() } returns setOf("authResponse1")
        coEvery { authenticateResponseTopicRepository.getResponseTopics() } returns listOf("authResponse1")

        useCase.reconcile()

        verify(exactly = 0) { jsonRpcInteractor.unsubscribe(Topic("authResponse1"), any(), any()) }
    }

    @Test
    fun `should not unsubscribe pending session topics`() = runTest {
        pendingSessionTopicRepository.insert("key1", "pendingSession1")
        every { jsonRpcInteractor.getSubscriptionTopics() } returns setOf("pendingSession1")

        useCase.reconcile()

        verify(exactly = 0) { jsonRpcInteractor.unsubscribe(Topic("pendingSession1"), any(), any()) }
    }

    @Test
    fun `should abort reconciliation when getPairings fails`() = runTest {
        every { jsonRpcInteractor.getSubscriptionTopics() } returns setOf("pairing1", "orphan1")
        coEvery { getPairingsUseCase.getListOfSettledPairings() } throws RuntimeException("DB error")

        useCase.reconcile()

        verify(exactly = 0) { jsonRpcInteractor.unsubscribe(any(), any(), any()) }
        verify(exactly = 0) { crypto.removeKeys(any()) }
    }

    @Test
    fun `should do nothing when no subscriptions exist`() = runTest {
        every { jsonRpcInteractor.getSubscriptionTopics() } returns emptySet()

        useCase.reconcile()

        verify(exactly = 0) { jsonRpcInteractor.unsubscribe(any(), any(), any()) }
        verify(exactly = 0) { crypto.removeKeys(any()) }
    }
}
