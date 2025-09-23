package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.exception.CannotFindSequenceForTopic
import com.reown.foundation.common.model.Topic
import com.reown.foundation.util.Logger
import com.reown.sign.common.exceptions.InvalidEventException
import com.reown.sign.common.exceptions.NO_SEQUENCE_FOR_TOPIC_MESSAGE
import com.reown.sign.common.exceptions.UNAUTHORIZED_EMIT_MESSAGE
import com.reown.sign.common.exceptions.UnauthorizedEventException
import com.reown.sign.common.exceptions.UnauthorizedPeerException
import com.reown.sign.common.validator.SignValidator
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.supervisorScope
import uniffi.yttrium.SignClient
import kotlinx.coroutines.async

internal class EmitEventUseCase(
    private val sessionStorageRepository: SessionStorageRepository,
    private val logger: Logger,
    private val signClient: SignClient
) : EmitEventUseCaseInterface {

    override suspend fun emit(topic: String, event: EngineDO.Event, id: Long?, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) = supervisorScope {
        runCatching { validate(topic, event) }.fold(
            onSuccess = {
                logger.log("Emitting event on topic: $topic; event: $event")

                try {
                    async { signClient.emit(topic = topic, name = event.name, data = event.data, chainId = event.chainId) }.await()

                    logger.log("Event sent successfully, on topic: $topic")
                    onSuccess()
                } catch (e: Exception) {
                    logger.error("Sending event error: $e, on topic: $topic")
                    onFailure(e)
                }
            },
            onFailure = { error ->
                logger.error("Sending event error: $error, on topic: $topic")
                onFailure(error)
            }
        )
    }

    private fun validate(topic: String, event: EngineDO.Event) {
        if (!sessionStorageRepository.isSessionValid(Topic(topic))) {
            logger.error("Emit - cannot find sequence for topic: $topic")
            throw CannotFindSequenceForTopic("$NO_SEQUENCE_FOR_TOPIC_MESSAGE$topic")
        }

        val session = sessionStorageRepository.getSessionWithoutMetadataByTopic(Topic(topic))
        if (!session.isSelfController) {
            logger.error("Emit - unauthorized peer: $topic")
            throw UnauthorizedPeerException(UNAUTHORIZED_EMIT_MESSAGE)
        }

        SignValidator.validateEvent(event) { error ->
            logger.error("Emit - invalid event: $topic")
            throw InvalidEventException(error.message)
        }

        val namespaces = session.sessionNamespaces
        SignValidator.validateChainIdWithEventAuthorisation(event.chainId, event.name, namespaces) { error ->
            logger.error("Emit - unauthorized event: $topic")
            throw UnauthorizedEventException(error.message)
        }
    }
}

internal interface EmitEventUseCaseInterface {
    suspend fun emit(topic: String, event: EngineDO.Event, id: Long? = null, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit)
}