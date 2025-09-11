package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.exception.CannotFindSequenceForTopic
import com.reown.foundation.common.model.Topic
import com.reown.foundation.util.Logger
import com.reown.sign.common.exceptions.NO_SEQUENCE_FOR_TOPIC_MESSAGE
import com.reown.sign.common.exceptions.NotSettledSessionException
import com.reown.sign.common.exceptions.SESSION_IS_NOT_ACKNOWLEDGED_MESSAGE
import com.reown.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import uniffi.yttrium.SignClient

internal class ExtendSessionUseCase(
//    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val logger: Logger,
    private val signClient: SignClient,
) : ExtendSessionUseCaseInterface {

    override suspend fun extend(topic: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) = supervisorScope {
        if (!sessionStorageRepository.isSessionValid(Topic(topic))) {
            return@supervisorScope onFailure(CannotFindSequenceForTopic("$NO_SEQUENCE_FOR_TOPIC_MESSAGE$topic"))
        }

        val session = sessionStorageRepository.getSessionWithoutMetadataByTopic(Topic(topic))
        if (!session.isAcknowledged) {
            logger.error("kobe: Sending session extend error: not acknowledged session on topic: $topic")
            return@supervisorScope onFailure(NotSettledSessionException("$SESSION_IS_NOT_ACKNOWLEDGED_MESSAGE$topic"))
        }

        logger.log("kobe: Sending session extend on topic: $topic")

        try {
            async { signClient.extend(topic) }.await()

            logger.log("kobe: Session extend sent successfully on topic: $topic")
            onSuccess()
        } catch (e: Exception) {
            logger.error("Sending session extend error: $e on topic: $topic")
            onFailure(e)
        }
    }
}

internal interface ExtendSessionUseCaseInterface {
    suspend fun extend(topic: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit)
}