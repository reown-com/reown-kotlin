package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.exception.CannotFindSequenceForTopic
import com.reown.foundation.common.model.Topic
import com.reown.foundation.util.Logger
import com.reown.sign.common.exceptions.NO_SEQUENCE_FOR_TOPIC_MESSAGE
import com.reown.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import uniffi.yttrium.SignClient

internal class DisconnectSessionUseCase(
    private val sessionStorageRepository: SessionStorageRepository,
    private val logger: Logger,
    private val signClient: SignClient,
) : DisconnectSessionUseCaseInterface {
    override suspend fun disconnect(topic: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) = supervisorScope {
//        if (!sessionStorageRepository.isSessionValid(Topic(topic))) {
//            logger.error("Sending session disconnect error: invalid session $topic")
//            return@supervisorScope onFailure(CannotFindSequenceForTopic("$NO_SEQUENCE_FOR_TOPIC_MESSAGE$topic"))
//        }

        logger.log("Sending session disconnect on topic: $topic")
        
        try {
            async { signClient.disconnect(topic) }.await()
            onSuccess()
        } catch (e: Exception) {
            logger.error("Sending session disconnect error: $e on topic: $topic")
            onFailure(e)
        }
    }
}

internal interface DisconnectSessionUseCaseInterface {
    suspend fun disconnect(topic: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit)
}