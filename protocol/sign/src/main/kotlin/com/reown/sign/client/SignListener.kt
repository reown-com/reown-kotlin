package com.reown.sign.client

import com.reown.android.internal.common.model.AppMetaDataType
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.Validation
import com.reown.android.internal.common.model.type.EngineEvent
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.foundation.common.model.Topic
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.engine.model.mapper.toEngineDOSessionExtend
import com.reown.sign.engine.model.mapper.toSessionApproved
import com.reown.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import uniffi.yttrium.SessionRequestJsonRpcFfi
import uniffi.yttrium.SessionRequestJsonRpcResponseFfi
import uniffi.yttrium.SettleNamespace
import uniffi.yttrium.SignListener

internal class SignListener(
    private val sessionStorage: SessionStorageRepository,
    private val metadataStorage: MetadataStorageRepositoryInterface
) : SignListener {
    //todo: add local coroutines scope
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    override fun onSessionConnect(id: ULong, topic: String) {
        println("kobe: onSessionConnect: $id; topic: $topic")
        scope.launch {
            val session = sessionStorage.getSessionWithoutMetadataByTopic(Topic(topic))
                .run {
                    val peerAppMetaData = metadataStorage.getByTopicAndType(this.topic, AppMetaDataType.PEER)
                    this.copy(peerAppMetaData = peerAppMetaData)
                }

            _events.emit(session.toSessionApproved())
        }
    }

    override fun onSessionReject(id: ULong, topic: String) {
        println("kobe: onSessionReject: $id; topic: $topic")

        scope.launch {
            _events.emit(EngineDO.SessionRejected(topic, "User rejected"))
        }
    }

    override fun onSessionDisconnect(id: ULong, topic: String) {
        println("kobe: onSessionDisconnect: $id; $topic")

        scope.launch {
            _events.emit(EngineDO.SessionDelete(topic, "User disconnected"))
        }
    }

    override fun onSessionEvent(id: ULong, topic: String, params: Boolean) {
        println("kobe: onSessionEvent: $id; $topic")
    }

    override fun onSessionExtend(id: ULong, topic: String) {
        println("kobe: onSessionExtend: $id; $topic")

        //Storage already update with new expiry, called from Rust
        val session = sessionStorage.getSessionWithoutMetadataByTopic(Topic(topic))

        scope.launch {
            _events.emit(session.toEngineDOSessionExtend())
        }
    }

    override fun onSessionRequest(topic: String, sessionRequest: SessionRequestJsonRpcFfi) {
        println("kobe: onSessionRequest: $sessionRequest")

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

        scope.launch {
            _events.emit(sessionRequestEvent)
            println("kobe: session request event emitted successfully")
        }
    }

    override fun onSessionRequestResponse(id: ULong, topic: String, response: SessionRequestJsonRpcResponseFfi) {
        println("kobe: onSessionRequestResponse: $id; $topic")
    }

    override fun onSessionUpdate(
        id: ULong,
        topic: String,
        namespaces: Map<String, SettleNamespace>
    ) {
        println("kobe: onSessionUpdate: $id; $topic; $namespaces")
    }
}