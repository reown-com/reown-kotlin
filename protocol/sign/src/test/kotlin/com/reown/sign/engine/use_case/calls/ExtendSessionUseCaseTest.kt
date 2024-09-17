package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.exception.CannotFindSequenceForTopic
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.foundation.common.model.PublicKey
import com.reown.foundation.common.model.Topic
import com.reown.foundation.util.Logger
import com.reown.sign.common.exceptions.NotSettledSessionException
import com.reown.sign.common.model.vo.sequence.SessionVO
import com.reown.sign.storage.sequence.SessionStorageRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ExtendSessionUseCaseTest {
    private val jsonRpcInteractor = mockk<RelayJsonRpcInteractorInterface>()
    private val sessionStorageRepository = mockk<SessionStorageRepository>()
    private val logger = mockk<Logger>()
    private val extendSessionUseCase = ExtendSessionUseCase(jsonRpcInteractor, sessionStorageRepository, logger)

    @Before
    fun setUp() {
        every { logger.error(any() as String) } answers { }
        every { logger.error(any() as Exception) } answers { }
    }

    @Test
    fun `onFailure is called when sessionStorageRepository isSessionValid is false`() = runTest {
        every { sessionStorageRepository.isSessionValid(any()) } returns false

        extendSessionUseCase.extend(
            topic = "topic",
            onSuccess = {
                fail("onSuccess should not be called since should have validation failed")
            },
            onFailure = { error ->
                assertSame(CannotFindSequenceForTopic::class, error::class)
            }
        )
    }

    @Test
    fun `onFailure is called when session isAcknowledged is false`() = runTest {
        every { sessionStorageRepository.isSessionValid(any()) } returns true
        every { sessionStorageRepository.getSessionWithoutMetadataByTopic(any()) } returns SessionVO(
            topic = Topic("topic"),
            expiry = Expiry(0),
            relayProtocol = "relayProtocol",
            relayData = "relayData",
            controllerKey = PublicKey(""),
            selfPublicKey = PublicKey(""),
            sessionNamespaces = emptyMap(),
            requiredNamespaces = emptyMap(),
            optionalNamespaces = emptyMap(),
            isAcknowledged = false,
            pairingTopic = "pairingTopic",
            transportType = null
        )

        extendSessionUseCase.extend(
            topic = "topic",
            onSuccess = {
                fail("onSuccess should not be called since should have validation failed")
            },
            onFailure = { error ->
                assertSame(NotSettledSessionException::class, error::class)
            }
        )
    }
}