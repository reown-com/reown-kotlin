package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.exception.CannotFindSequenceForTopic
import com.reown.foundation.common.model.Topic
import com.reown.foundation.util.Logger
import com.reown.sign.common.exceptions.InvalidNamespaceException
import com.reown.sign.common.exceptions.NO_SEQUENCE_FOR_TOPIC_MESSAGE
import com.reown.sign.common.exceptions.NotSettledSessionException
import com.reown.sign.common.exceptions.SESSION_IS_NOT_ACKNOWLEDGED_MESSAGE
import com.reown.sign.common.exceptions.UNAUTHORIZED_UPDATE_MESSAGE
import com.reown.sign.common.exceptions.UnauthorizedPeerException
import com.reown.sign.common.validator.SignValidator
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.engine.model.mapper.toMapOfNamespacesVOSession
import com.reown.sign.engine.model.mapper.toSettleYttrium
import com.reown.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import uniffi.yttrium.SignClient

internal class SessionUpdateUseCase(
//    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val logger: Logger,
    private val signClient: SignClient,
) : SessionUpdateUseCaseInterface {

    override suspend fun sessionUpdate(
        topic: String,
        namespaces: Map<String, EngineDO.Namespace.Session>,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) = supervisorScope {
        runCatching { validate(topic, namespaces) }.fold(
            onSuccess = {
                logger.log("kobe: Sending session update on topic: $topic; $namespaces")

                try {
                    async { signClient.update(topic = topic, namespaces = namespaces.toSettleYttrium()) }.await()

                    logger.log("kobe: Update sent successfully, topic: $topic")
                    onSuccess()
                } catch (e: Exception) {
                    logger.error("kobe: Sending session update error: $e, topic: $topic")
                    onFailure(e)
                }
            },
            onFailure = {
                logger.error("kobe: Error updating namespaces: $it")
                onFailure(it)
            }
        )
    }

    private fun validate(topic: String, namespaces: Map<String, EngineDO.Namespace.Session>) {
        if (!sessionStorageRepository.isSessionValid(Topic(topic))) {
            logger.error("Sending session update error: cannot find sequence for topic: $topic")
            throw CannotFindSequenceForTopic("$NO_SEQUENCE_FOR_TOPIC_MESSAGE$topic")
        }

        val session = sessionStorageRepository.getSessionWithoutMetadataByTopic(Topic(topic))

        if (!session.isSelfController) {
            logger.error("Sending session update error: unauthorized peer")
            throw UnauthorizedPeerException(UNAUTHORIZED_UPDATE_MESSAGE)
        }

        if (!session.isAcknowledged) {
            logger.error("Sending session update error: session is not acknowledged")
            throw NotSettledSessionException("$SESSION_IS_NOT_ACKNOWLEDGED_MESSAGE$topic")
        }

        SignValidator.validateSessionNamespace(namespaces.toMapOfNamespacesVOSession(), session.requiredNamespaces) { error ->
            logger.error("Sending session update error: invalid namespaces $error")
            throw InvalidNamespaceException(error.message)
        }
    }
}

internal interface SessionUpdateUseCaseInterface {
    suspend fun sessionUpdate(
        topic: String,
        namespaces: Map<String, EngineDO.Namespace.Session>,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    )
}