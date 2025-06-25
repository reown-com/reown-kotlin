package com.reown.sign.engine.use_case.responses

import com.reown.android.internal.common.JsonRpcResponse
import com.reown.android.internal.common.model.SDKError
import com.reown.android.internal.common.model.WCResponse
import com.reown.android.internal.common.model.type.EngineEvent
import com.reown.foundation.util.Logger
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.engine.model.mapper.toMapOfEngineNamespacesSession
import com.reown.sign.storage.sequence.SessionStorageRepository
import com.reown.utils.extractTimestamp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.supervisorScope

internal class OnSessionUpdateResponseUseCase(
    private val sessionStorageRepository: SessionStorageRepository,
    private val logger: Logger
) {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    suspend operator fun invoke(wcResponse: WCResponse) = supervisorScope {
        try {
            logger.log("Session update namespaces response received on topic: ${wcResponse.topic}")
            val sessionTopic = wcResponse.topic
            if (!sessionStorageRepository.isSessionValid(sessionTopic)) return@supervisorScope
            val session = sessionStorageRepository.getSessionWithoutMetadataByTopic(sessionTopic)
            if (!sessionStorageRepository.isUpdatedNamespaceResponseValid(session.topic.value, wcResponse.response.id.extractTimestamp())) {
                logger.error("Session update namespaces response error: invalid namespaces")
                return@supervisorScope
            }

            when (val response = wcResponse.response) {
                is JsonRpcResponse.JsonRpcResult -> {
                    logger.log("Session update namespaces response received on topic: ${wcResponse.topic}")
                    val responseId = wcResponse.response.id
                    val namespaces = sessionStorageRepository.getTempNamespaces(responseId)
                    sessionStorageRepository.deleteTempNamespacesByTopic(sessionTopic.value)
                    sessionStorageRepository.deleteNamespaceAndInsertNewNamespace(session.topic.value, namespaces, responseId)
                    sessionStorageRepository.markUnAckNamespaceAcknowledged(responseId)
                    _events.emit(EngineDO.SessionUpdateNamespacesResponse.Result(session.topic, namespaces.toMapOfEngineNamespacesSession()))
                }

                is JsonRpcResponse.JsonRpcError -> {
                    logger.error("Peer failed to update session namespaces: ${response.error} on topic: ${wcResponse.topic}")
                    _events.emit(EngineDO.SessionUpdateNamespacesResponse.Error(response.errorMessage))
                }
            }
        } catch (e: Exception) {
            logger.error("Peer failed to update session namespaces: $e")
            _events.emit(SDKError(e))
        }
    }
}