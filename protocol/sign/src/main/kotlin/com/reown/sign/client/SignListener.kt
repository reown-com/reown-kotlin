package com.reown.sign.client

import com.reown.android.internal.common.model.AppMetaDataType
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.Validation
import com.reown.android.internal.common.model.type.EngineEvent
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.foundation.common.model.Topic
import com.reown.sign.client.mapper.toMapOfEngineNamespacesSession
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.engine.model.mapper.toEngineDOSessionExtend
import com.reown.sign.engine.model.mapper.toMapOfEngineNamespacesSession
import com.reown.sign.engine.model.mapper.toSessionApproved
import com.reown.sign.engine.model.mapper.toSessionVO
import com.reown.sign.json_rpc.domain.GetSessionRequestByIdUseCase
import com.reown.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    private val metadataStorage: MetadataStorageRepositoryInterface,
    private val getSessionRequestByIdUseCase: GetSessionRequestByIdUseCase,
) : SignListener {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("kobe: SignListener coroutine error: $throwable")
    }

    private val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    override fun onSessionConnect(id: ULong, topic: String) {
        ioScope.launch {
            println("kobe: onSessionConnect: $id; topic: $topic")

            val session = sessionStorage.getSessionWithoutMetadataByTopic(Topic(topic))
                .run {
                    val peerAppMetaData = metadataStorage.getByTopicAndType(this.topic, AppMetaDataType.PEER)
                    this.copy(peerAppMetaData = peerAppMetaData)
                }

            _events.emit(session.toSessionApproved())
        }
    }

    override fun onSessionReject(id: ULong, topic: String) {
        ioScope.launch {
            println("kobe: onSessionReject: $id; topic: $topic")

            _events.emit(EngineDO.SessionRejected(topic, "User rejected"))
        }
    }

    override fun onSessionDisconnect(id: ULong, topic: String) {
        ioScope.launch {
            println("kobe: onSessionDisconnect: $id; $topic")


            _events.emit(EngineDO.SessionDelete(topic, "User disconnected"))
        }
    }

    override fun onSessionRequest(topic: String, sessionRequest: SessionRequestJsonRpcFfi) {
        ioScope.launch {
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

            _events.emit(sessionRequestEvent)
            println("kobe: session request event emitted successfully")
        }
    }

    override fun onSessionRequestResponse(id: ULong, topic: String, response: SessionRequestJsonRpcResponseFfi) {
        ioScope.launch {
            println("bary: onSessionRequestResponse: $id; $topic; $response")

            val jsonRpcHistoryEntry = getSessionRequestByIdUseCase(id.toLong())

            println("bary: onSessionRequestResponse: history entry: $jsonRpcHistoryEntry")

            val result = when (val jsonRpcResponse = response) {
                is SessionRequestJsonRpcResponseFfi.Result -> {
                    EngineDO.JsonRpcResponse.JsonRpcResult(id = jsonRpcResponse.v1.id.toLong(), result = jsonRpcResponse.v1.result)
                }

                is SessionRequestJsonRpcResponseFfi.Error -> {
                    EngineDO.JsonRpcResponse.JsonRpcError(
                        id = jsonRpcResponse.v1.id.toLong(),
                        error = EngineDO.JsonRpcResponse.Error(1000, jsonRpcResponse.v1.error)
                    )
                }

            }

            //todo: get chainId and method from json rpc history entry
            _events.emit(
                EngineDO.SessionPayloadResponse(
                    topic,
                    jsonRpcHistoryEntry?.params?.chainId,
                    jsonRpcHistoryEntry?.params?.request?.method ?: "",
                    result
                )
            )
        }
    }

    override fun onSessionEvent(topic: String, name: String, data: String, chainId: String) {
        ioScope.launch {
            println("kobe: onSessionEvent: $name; $topic; $data; $chainId")

            _events.emit(EngineDO.SessionEvent(topic, name, data, chainId))
        }
    }

    override fun onSessionExtend(id: ULong, topic: String) {
        ioScope.launch {
            println("kobe: onSessionExtend: $id; $topic")

            //Storage already update with new expiry, called from Rust
            val session = sessionStorage.getSessionWithoutMetadataByTopic(Topic(topic))

            _events.emit(session.toEngineDOSessionExtend())
        }
    }

    override fun onSessionUpdate(
        id: ULong,
        topic: String,
        namespaces: Map<String, SettleNamespace>
    ) {
        ioScope.launch {
            println("kobe: onSessionUpdate: $id; $topic; $namespaces")
            _events.emit(EngineDO.SessionUpdateNamespaces(Topic(topic), namespaces.toSessionVO().toMapOfEngineNamespacesSession()))
        }
    }
}