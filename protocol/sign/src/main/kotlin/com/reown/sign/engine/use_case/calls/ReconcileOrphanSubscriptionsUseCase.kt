@file:JvmSynthetic

package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.crypto.kmr.KeyManagementRepository
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.foundation.common.model.Topic
import com.reown.foundation.util.Logger
import com.reown.sign.storage.authenticate.AuthenticateResponseTopicRepository
import com.reown.sign.storage.pending_session.PendingSessionTopicRepository
import com.reown.sign.storage.sequence.SessionStorageRepository

internal class ReconcileOrphanSubscriptionsUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val authenticateResponseTopicRepository: AuthenticateResponseTopicRepository,
    private val pendingSessionTopicRepository: PendingSessionTopicRepository,
    private val getPairingsUseCase: GetPairingsUseCaseInterface,
    private val crypto: KeyManagementRepository,
    private val logger: Logger
) {
    suspend fun reconcile() {
        try {
            val subscribedTopics = jsonRpcInteractor.getSubscriptionTopics()
            if (subscribedTopics.isEmpty()) return

            val knownTopics = mutableSetOf<String>()

            knownTopics.addAll(
                sessionStorageRepository.getListOfSessionVOsWithoutMetadata().map { it.topic.value }
            )

            knownTopics.addAll(authenticateResponseTopicRepository.getResponseTopics())

            knownTopics.addAll(pendingSessionTopicRepository.getAllSessionTopics())

            runCatching {
                knownTopics.addAll(getPairingsUseCase.getListOfSettledPairings().map { it.topic.value })
            }.onFailure { logger.error(it) }

            val orphanTopics = subscribedTopics - knownTopics
            orphanTopics.forEach { topic ->
                logger.log("Reconciliation: unsubscribing orphan topic: $topic")
                runCatching { crypto.removeKeys(topic) }.onFailure { logger.error(it) }
                jsonRpcInteractor.unsubscribe(Topic(topic),
                    onFailure = { logger.error("Failed to unsubscribe orphan topic: $it") }
                )
            }
        } catch (e: Exception) {
            logger.error("Reconciliation error: $e")
        }
    }
}
