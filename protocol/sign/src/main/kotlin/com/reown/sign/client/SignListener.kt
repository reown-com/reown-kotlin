package com.reown.sign.client

import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.Validation
import com.reown.android.internal.common.model.type.EngineEvent
import com.reown.android.internal.common.scope
import com.reown.sign.engine.model.EngineDO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import uniffi.yttrium.SessionRequestJsonRpcFfi
import uniffi.yttrium.SessionRequestJsonRpcResponseFfi
import uniffi.yttrium.SettleNamespace
import uniffi.yttrium.SignListener

class SignListener() : SignListener {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    override fun onSessionConnect(id: ULong) {
        TODO("Not yet implemented")
    }

    override fun onSessionDisconnect(id: ULong, topic: String) {
        TODO("Not yet implemented")
    }

    override fun onSessionEvent(id: ULong, topic: String, params: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onSessionExtend(id: ULong, topic: String) {
        TODO("Not yet implemented")
    }

    override fun onSessionRequest(topic: String, sessionRequest: SessionRequestJsonRpcFfi) {
        val sessionRequestEvent = EngineDO.SessionRequestEvent(
            request = EngineDO.SessionRequest(
                topic = topic,
                chainId = sessionRequest.params.chainId,
                peerAppMetaData = null,
                expiry = if (sessionRequest.params.request.expiry != null) Expiry(sessionRequest.params.request.expiry!!.toLong()) else null,
                request = EngineDO.SessionRequest.JSONRPCRequest(
                    id = sessionRequest.id.toLong(),
                    method = sessionRequest.params.request.method,
                    params = sessionRequest.params.request.params
                )
            ),
            context = EngineDO.VerifyContext(1, "", Validation.UNKNOWN, "", null)
        )
        scope.launch { _events.emit(sessionRequestEvent) }
    }

    override fun onSessionRequestResponse(id: ULong, topic: String, response: SessionRequestJsonRpcResponseFfi) {
        TODO("Not yet implemented")
    }

    override fun onSessionUpdate(
        id: ULong,
        topic: String,
        namespaces: Map<String, SettleNamespace>
    ) {
        TODO("Not yet implemented")
    }
}