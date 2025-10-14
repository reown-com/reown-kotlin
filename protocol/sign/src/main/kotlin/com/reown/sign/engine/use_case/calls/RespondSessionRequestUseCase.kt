package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.JsonRpcResponse
import com.reown.android.internal.common.exception.CannotFindSequenceForTopic
import com.reown.android.internal.common.exception.RequestExpiredException
import com.reown.android.internal.common.json_rpc.domain.link_mode.LinkModeJsonRpcInteractorInterface
import com.reown.android.internal.common.model.AppMetaDataType
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.IrnParams
import com.reown.android.internal.common.model.Tags
import com.reown.android.internal.common.model.TransportType
import com.reown.android.internal.common.model.type.EngineEvent
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.android.internal.common.storage.verify.VerifyContextStorageRepository
import com.reown.android.internal.utils.CoreValidator.isExpired
import com.reown.android.internal.utils.fiveMinutesInSeconds
import com.reown.android.pulse.domain.InsertEventUseCase
import com.reown.android.pulse.model.Direction
import com.reown.android.pulse.model.EventType
import com.reown.android.pulse.model.properties.Properties
import com.reown.android.pulse.model.properties.Props
import com.reown.foundation.common.model.Topic
import com.reown.foundation.common.model.Ttl
import com.reown.foundation.util.Logger
import com.reown.sign.common.exceptions.NO_SEQUENCE_FOR_TOPIC_MESSAGE
import com.reown.sign.engine.model.tvf.TVF
import com.reown.sign.engine.sessionRequestEventsQueue
import com.reown.sign.json_rpc.domain.GetPendingJsonRpcHistoryEntryByIdUseCase
import com.reown.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.JsonClass
import uniffi.yttrium.SessionRequestJsonRpcErrorResponseFfi
import uniffi.yttrium.SessionRequestJsonRpcResponseFfi
import uniffi.yttrium.SessionRequestJsonRpcResultResponseFfi
import uniffi.yttrium.SignClient

@JsonClass(generateAdapter = true)
data class JsonRpcErrorData(
    val code: Int,
    val message: String,
    val data: String? = null
)

internal class RespondSessionRequestUseCase(
//    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
//    private val getPendingJsonRpcHistoryEntryByIdUseCase: GetPendingJsonRpcHistoryEntryByIdUseCase,
//    private val linkModeJsonRpcInteractor: LinkModeJsonRpcInteractorInterface,
//    private val logger: Logger,
    private val verifyContextStorageRepository: VerifyContextStorageRepository,
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
//    private val insertEventUseCase: InsertEventUseCase,
//    private val clientId: String,
//    private val tvf: TVF,
    private val signClient: SignClient
) : RespondSessionRequestUseCaseInterface {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    override val events: SharedFlow<EngineEvent> = _events.asSharedFlow()
    override suspend fun respondSessionRequest(
        topic: String,
        jsonRpcResponse: JsonRpcResponse,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) = supervisorScope {
        try {
            val responseFfi = when (jsonRpcResponse) {
                is JsonRpcResponse.JsonRpcResult -> {
                    val responseResultFfi = SessionRequestJsonRpcResultResponseFfi(
                        id = jsonRpcResponse.id.toULong(),
                        jsonrpc = "2.0",
                        result = jsonRpcResponse.result.toString()
                    )
                    SessionRequestJsonRpcResponseFfi.Result(responseResultFfi)
                }

                is JsonRpcResponse.JsonRpcError -> {
                    val errorJson = moshi.adapter(JsonRpcResponse.Error::class.java).toJson(jsonRpcResponse.error)
                    println("kobe: parsed error JSON: $errorJson")
                    
                    val responseErrorFfi = SessionRequestJsonRpcErrorResponseFfi(
                        id = jsonRpcResponse.id.toULong(),
                        jsonrpc = "2.0",
                        error = errorJson
                    )
                    SessionRequestJsonRpcResponseFfi.Error(responseErrorFfi)
                }
            }

            val result = async {
                try {
                    println("kobe: sending response: $responseFfi")
                    signClient.respond(topic, responseFfi)
                } catch (e: Exception) {
                    println("kobe: session request error: $e")
                    onFailure(e)
                }
            }.await()

            println("kobe: session request responded: $result")
            onSuccess()

        } catch (e: Exception) {
            println("kobe: session request error: $e")
            onFailure(e)
        }
//        val topicWrapper = Topic(topic)
//        if (!sessionStorageRepository.isSessionValid(topicWrapper)) {
//            logger.error("Request response -  invalid session: $topic, id: ${jsonRpcResponse.id}")
//            return@supervisorScope onFailure(CannotFindSequenceForTopic("$NO_SEQUENCE_FOR_TOPIC_MESSAGE$topic"))
//        }
//        val session = sessionStorageRepository.getSessionWithoutMetadataByTopic(topicWrapper)
//            .run {
//                val peerAppMetaData = metadataStorageRepository.getByTopicAndType(this.topic, AppMetaDataType.PEER)
//                this.copy(peerAppMetaData = peerAppMetaData)
//            }
//
//        val pendingRequest = getPendingJsonRpcHistoryEntryByIdUseCase(jsonRpcResponse.id)
//        if (pendingRequest == null) {
//            logger.error("Request doesn't exist: $topic, id: ${jsonRpcResponse.id}")
//            return@supervisorScope onFailure(RequestExpiredException("This request has expired, id: ${jsonRpcResponse.id}"))
//        }
//        pendingRequest.params.expiry?.let {
//            if (Expiry(it).isExpired()) {
//                logger.error("Request Expired: $topic, id: ${jsonRpcResponse.id}")
//                return@supervisorScope onFailure(RequestExpiredException("This request has expired, id: ${jsonRpcResponse.id}"))
//            }
//        }
//
//        if (session.transportType == TransportType.LINK_MODE && session.peerLinkMode == true) {
//            if (session.peerAppLink.isNullOrEmpty()) return@supervisorScope onFailure(IllegalStateException("App link is missing"))
//            try {
//                removePendingSessionRequestAndEmit(jsonRpcResponse.id)
//                linkModeJsonRpcInteractor.triggerResponse(Topic(topic), jsonRpcResponse, session.peerAppLink)
//                insertEventUseCase(
//                    Props(
//                        EventType.SUCCESS,
//                        Tags.SESSION_REQUEST_LINK_MODE_RESPONSE.id.toString(),
//                        Properties(correlationId = jsonRpcResponse.id, clientId = clientId, direction = Direction.SENT.state)
//                    )
//                )
//            } catch (e: Exception) {
//                onFailure(e)
//            }
//        } else {
//            val tvfData = tvf.collect(pendingRequest.params.rpcMethod, pendingRequest.params.rpcParams, pendingRequest.params.chainId)
//            val txHashes = (jsonRpcResponse as? JsonRpcResponse.JsonRpcResult)?.let {
//                tvf.collectTxHashes(
//                    pendingRequest.params.rpcMethod,
//                    it.result.toString(),
//                    pendingRequest.params.rpcParams
//                )
//            }
//            val irnParams = IrnParams(
//                Tags.SESSION_REQUEST_RESPONSE,
//                Ttl(fiveMinutesInSeconds),
//                correlationId = jsonRpcResponse.id,
//                rpcMethods = tvfData.first,
//                contractAddresses = tvfData.second,
//                txHashes = txHashes,
//                chainId = tvfData.third
//            )
//            logger.log("Sending session request response on topic: $topic, id: ${jsonRpcResponse.id}")
//            jsonRpcInteractor.publishJsonRpcResponse(
//                topic = Topic(topic), params = irnParams, response = jsonRpcResponse,
//                onSuccess = {
//                    onSuccess()
//                    logger.log("Session request response sent successfully on topic: $topic, id: ${jsonRpcResponse.id}")
//                    scope.launch {
//                        supervisorScope {
//                            removePendingSessionRequestAndEmit(jsonRpcResponse.id)
//                        }
//                    }
//                },
//                onFailure = { error ->
//                    logger.error("Sending session response error: $error, id: ${jsonRpcResponse.id}")
//                    onFailure(error)
//                }
//            )
//        }
    }

    private suspend fun removePendingSessionRequestAndEmit(id: Long) {
        verifyContextStorageRepository.delete(id)
        sessionRequestEventsQueue.find { pendingRequestEvent -> pendingRequestEvent.request.request.id == id }?.let { event ->
            sessionRequestEventsQueue.remove(event)
        }
        if (sessionRequestEventsQueue.isNotEmpty()) {
            sessionRequestEventsQueue.find { event -> if (event.request.expiry != null) !event.request.expiry.isExpired() else true }?.let { event ->
                _events.emit(event)
            }
        }
    }
}

internal interface RespondSessionRequestUseCaseInterface {
    val events: SharedFlow<EngineEvent>
    suspend fun respondSessionRequest(
        topic: String,
        jsonRpcResponse: JsonRpcResponse,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    )
}