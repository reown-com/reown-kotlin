package com.reown.sign.engine.use_case.responses

import com.reown.android.internal.common.JsonRpcResponse
import com.reown.android.internal.common.model.SDKError
import com.reown.android.internal.common.model.Tags
import com.reown.android.internal.common.model.TransportType
import com.reown.android.internal.common.model.WCResponse
import com.reown.android.internal.common.model.type.EngineEvent
import com.reown.android.pulse.domain.InsertEventUseCase
import com.reown.android.pulse.model.Direction
import com.reown.android.pulse.model.EventType
import com.reown.android.pulse.model.properties.Properties
import com.reown.android.pulse.model.properties.Props
import com.reown.foundation.util.Logger
import com.reown.sign.common.model.vo.clientsync.session.params.SignParams
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.engine.model.mapper.toEngineDO
import com.reown.sign.json_rpc.domain.GetSessionRequestByIdUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.supervisorScope

internal class OnSessionRequestResponseUseCase(
    private val logger: Logger,
    private val insertEventUseCase: InsertEventUseCase,
    private val getSessionRequestByIdUseCase: GetSessionRequestByIdUseCase,
    private val clientId: String
) {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    suspend operator fun invoke(wcResponse: WCResponse, params: SignParams.SessionRequestParams) = supervisorScope {
        try {
            val jsonRpcHistoryEntry = getSessionRequestByIdUseCase(wcResponse.response.id)
            logger.log("Session request response received on topic: ${wcResponse.topic}")
            val result = when (wcResponse.response) {
                is JsonRpcResponse.JsonRpcResult -> (wcResponse.response as JsonRpcResponse.JsonRpcResult).toEngineDO()
                is JsonRpcResponse.JsonRpcError -> (wcResponse.response as JsonRpcResponse.JsonRpcError).toEngineDO()
            }

            if (jsonRpcHistoryEntry?.transportType == TransportType.LINK_MODE) {
                insertEventUseCase(
                    Props(
                        EventType.SUCCESS,
                        Tags.SESSION_REQUEST_LINK_MODE_RESPONSE.id.toString(),
                        Properties(correlationId = wcResponse.response.id, clientId = clientId, direction = Direction.RECEIVED.state)
                    )
                )
            }
            val method = params.request.method
            logger.log("Session request response received on topic: ${wcResponse.topic} - emitting: $result")
            _events.emit(EngineDO.SessionPayloadResponse(wcResponse.topic.value, params.chainId, method, result))
        } catch (e: Exception) {
            logger.error("Session request response received failure: $e")
            _events.emit(SDKError(e))
        }
    }
}