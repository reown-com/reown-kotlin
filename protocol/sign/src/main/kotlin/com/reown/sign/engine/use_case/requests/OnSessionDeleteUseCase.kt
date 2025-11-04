package com.reown.sign.engine.use_case.requests

import com.reown.android.internal.common.crypto.kmr.KeyManagementRepository
import com.reown.android.internal.common.exception.Uncategorized
import com.reown.android.internal.common.model.IrnParams
import com.reown.android.internal.common.model.SDKError
import com.reown.android.internal.common.model.Tags
import com.reown.android.internal.common.model.WCRequest
import com.reown.android.internal.common.model.type.EngineEvent
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.utils.dayInSeconds
import com.reown.foundation.common.model.Ttl
import com.reown.foundation.util.Logger
import com.reown.sign.common.model.type.Sequences
import com.reown.sign.common.model.vo.clientsync.session.params.SignParams
import com.reown.sign.engine.model.mapper.toEngineDO
import com.reown.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.supervisorScope

internal class OnSessionDeleteUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val crypto: KeyManagementRepository,
    private val logger: Logger
) {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    suspend operator fun invoke(request: WCRequest, params: SignParams.DeleteParams) = supervisorScope {
        logger.log("Session delete received on topic: ${request.topic}")
        val irnParams = IrnParams(Tags.SESSION_DELETE_RESPONSE, Ttl(dayInSeconds), correlationId = request.id)
        try {
            if (!sessionStorageRepository.isSessionValid(request.topic)) {
                logger.error("Session delete received failure on topic: ${request.topic} - invalid session")
                jsonRpcInteractor.respondWithError(request, Uncategorized.NoMatchingTopic(Sequences.SESSION.name, request.topic.value), irnParams)
                return@supervisorScope
            }
            jsonRpcInteractor.unsubscribe(request.topic,
                onSuccess = {
                    logger.log("Session delete received on topic: ${request.topic} - unsubscribe success")
                    try {
                        crypto.removeKeys(request.topic.value)
                    } catch (e: Exception) {
                        logger.error("Remove keys exception:$e")
                    }
                },
                onFailure = { error -> logger.error("Session delete received on topic: ${request.topic} - unsubscribe error $error") })
            sessionStorageRepository.deleteSession(request.topic)
            logger.log("Session delete received on topic: ${request.topic} - emitting")
            _events.emit(params.toEngineDO(request.topic))
        } catch (e: Exception) {
            logger.error("Session delete received failure on topic: ${request.topic} - $e")
            jsonRpcInteractor.respondWithError(request, Uncategorized.GenericError("Cannot delete a session: ${e.message}, topic: ${request.topic}"), irnParams)
            _events.emit(SDKError(e))
            return@supervisorScope
        }
    }
}