package com.reown.android.internal.common.json_rpc.domain.relay

import com.reown.android.internal.common.ConditionalExponentialBackoffStrategy
import com.reown.android.internal.common.JsonRpcResponse
import com.reown.android.internal.common.crypto.codec.Codec
import com.reown.android.internal.common.crypto.sha256
import com.reown.android.internal.common.exception.NoConnectivityException
import com.reown.android.internal.common.exception.NoInternetConnectionException
import com.reown.android.internal.common.json_rpc.data.JsonRpcSerializer
import com.reown.android.internal.common.json_rpc.model.toRelay
import com.reown.android.internal.common.json_rpc.model.toWCRequest
import com.reown.android.internal.common.json_rpc.model.toWCResponse
import com.reown.android.internal.common.model.EnvelopeType
import com.reown.android.internal.common.model.IrnParams
import com.reown.android.internal.common.model.Participants
import com.reown.android.internal.common.model.SDKError
import com.reown.android.internal.common.model.TransportType
import com.reown.android.internal.common.model.WCRequest
import com.reown.android.internal.common.model.WCResponse
import com.reown.android.internal.common.model.params.ChatNotifyResponseAuthParams
import com.reown.android.internal.common.model.params.CoreSignParams
import com.reown.android.internal.common.model.sync.ClientJsonRpc
import com.reown.android.internal.common.model.type.ClientParams
import com.reown.android.internal.common.model.type.Error
import com.reown.android.internal.common.model.type.JsonRpcClientSync
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.storage.push_messages.PushMessagesRepository
import com.reown.android.internal.common.storage.rpc.JsonRpcHistory
import com.reown.android.internal.common.wcKoinApp
import com.reown.android.internal.utils.ObservableMap
import com.reown.android.relay.RelayConnectionInterface
import com.reown.android.relay.WSSConnectionState
import com.reown.foundation.common.model.SubscriptionId
import com.reown.foundation.common.model.Topic
import com.reown.foundation.network.model.Relay
import com.reown.foundation.util.Logger
import com.reown.utils.Empty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.bouncycastle.util.encoders.Base64
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private const val BATCH_SUBSCRIBE_TOPIC_LIMIT = 500

internal data class Subscription(
    val decryptedMessage: String,
    val encryptedMessage: String,
    val topic: Topic,
    val publishedAt: Long,
    val attestation: String?,
)

internal class RelayJsonRpcInteractor(
    private val relay: RelayConnectionInterface,
    private val chaChaPolyCodec: Codec,
    private val jsonRpcHistory: JsonRpcHistory,
    private val pushMessageStorage: PushMessagesRepository,
    private val logger: Logger,
    private val backoffStrategy: ConditionalExponentialBackoffStrategy
) : RelayJsonRpcInteractorInterface {
    private val serializer: JsonRpcSerializer get() = wcKoinApp.koin.get()

    private val _clientSyncJsonRpc: MutableSharedFlow<WCRequest> = MutableSharedFlow()
    override val clientSyncJsonRpc: SharedFlow<WCRequest> = _clientSyncJsonRpc.asSharedFlow()

    private val _peerResponse: MutableSharedFlow<WCResponse> = MutableSharedFlow()
    override val peerResponse: SharedFlow<WCResponse> = _peerResponse.asSharedFlow()

    private val _internalErrors = MutableSharedFlow<SDKError>()
    override val internalErrors: SharedFlow<SDKError> = _internalErrors.asSharedFlow()
    override val wssConnectionState: StateFlow<WSSConnectionState> get() = relay.wssConnectionState

    private var subscriptions = ObservableMap<String, String> { newMap -> if (newMap.isEmpty()) backoffStrategy.shouldBackoff(false) }
    override val onResubscribe: Flow<Any?> get() = relay.onResubscribe

    init {
        manageSubscriptions()
    }

    override fun checkNetworkConnectivity() {
        if (relay.isNetworkAvailable.value != null && relay.isNetworkAvailable.value == false) {
            throw NoInternetConnectionException("Connection error: Please check your Internet connection")
        }
    }

    override fun proposeSession(
        topic: Topic,
        payload: JsonRpcClientSync<*>,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        try {
            checkNetworkConnectivity()
        } catch (e: NoConnectivityException) {
            return onFailure(e)
        }

        try {
            val requestJson = serializer.serialize(payload) ?: throw IllegalStateException("RelayJsonRpcInteractor: Unknown Request Params")
            if (jsonRpcHistory.setRequest(payload.id, topic, payload.method, requestJson, TransportType.RELAY)) {
                val encryptedRequest = chaChaPolyCodec.encrypt(topic, requestJson, EnvelopeType.ZERO)
                val encryptedRequestString = Base64.toBase64String(encryptedRequest)
                relay.proposeSession(pairingTopic = topic, sessionProposal = encryptedRequestString, correlationId = payload.id) { result ->
                    result.fold(
                        onSuccess = { onSuccess() },
                        onFailure = { error ->
                            logger.error("JsonRpcInteractor: Cannot send the session proposal request, error: $error")
                            onFailure(Throwable("Session proposal error: ${error.message}"))
                        }
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("JsonRpcInteractor: Cannot send the request, exception: $e")
            onFailure(Throwable("Publish Request Error: $e"))
        }
    }

    override fun approveSession(
        pairingTopic: Topic,
        sessionTopic: Topic,
        sessionProposalResponse: CoreSignParams.ApprovalParams,
        settleRequest: JsonRpcClientSync<*>,
        approvedChains: List<String>?,
        approvedMethods: List<String>?,
        approvedEvents: List<String>?,
        sessionProperties: Map<String, String>?,
        scopedProperties: Map<String, String>?,
        correlationId: Long,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        try {
            checkNetworkConnectivity()
        } catch (e: NoConnectivityException) {
            return onFailure(e)
        }

        try {
            val proposalResponseJson =
                serializer.serialize(JsonRpcResponse.JsonRpcResult(id = correlationId, result = sessionProposalResponse))
                    ?: throw IllegalStateException("RelayJsonRpcInteractor: Unknown Request Params")
            val settlementRequestJson =
                serializer.serialize(settleRequest) ?: throw IllegalStateException("RelayJsonRpcInteractor: Unknown Request Params")
            if (jsonRpcHistory.setRequest(settleRequest.id, sessionTopic, settleRequest.method, settlementRequestJson, TransportType.RELAY)) {
                val encryptedProposalResponseJson = chaChaPolyCodec.encrypt(pairingTopic, proposalResponseJson, EnvelopeType.ZERO)
                val encryptedSettlementRequestJson = chaChaPolyCodec.encrypt(sessionTopic, settlementRequestJson, EnvelopeType.ZERO)
                val encryptedProposalResponseString = Base64.toBase64String(encryptedProposalResponseJson)
                val encryptedSettlementRequestString = Base64.toBase64String(encryptedSettlementRequestJson)

                relay.approveSession(
                    pairingTopic = pairingTopic,
                    sessionTopic = sessionTopic,
                    approvedChains = approvedChains,
                    approvedMethods = approvedMethods,
                    approvedEvents = approvedEvents,
                    sessionProperties = sessionProperties,
                    scopedProperties = scopedProperties,
                    correlationId = correlationId,
                    sessionSettlementRequest = encryptedSettlementRequestString,
                    sessionProposalResponse = encryptedProposalResponseString
                ) { result ->
                    result.fold(
                        onSuccess = {
                            jsonRpcHistory.updateRequestWithResponse(correlationId, proposalResponseJson)
                            onSuccess()
                        },
                        onFailure = { error ->
                            logger.error("JsonRpcInteractor: Cannot send the session approve request, error: $error")
                            onFailure(Throwable("Session approve error: ${error.message}"))
                        }
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("JsonRpcInteractor: Cannot send the request, exception: $e")
            onFailure(Throwable("Publish Request Error: $e"))
        }
    }

    override fun publishJsonRpcRequest(
        topic: Topic,
        params: IrnParams,
        payload: JsonRpcClientSync<*>,
        envelopeType: EnvelopeType,
        participants: Participants?,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        try {
            checkNetworkConnectivity()
        } catch (e: NoConnectivityException) {
            return onFailure(e)
        }

        try {
            val requestJson = serializer.serialize(payload) ?: throw IllegalStateException("RelayJsonRpcInteractor: Unknown Request Params")
            if (jsonRpcHistory.setRequest(payload.id, topic, payload.method, requestJson, TransportType.RELAY)) {
                val encryptedRequest = chaChaPolyCodec.encrypt(topic, requestJson, envelopeType, participants)
                val encryptedRequestString = Base64.toBase64String(encryptedRequest)
                relay.publish(topic.value, encryptedRequestString, params.toRelay()) { result ->
                    result.fold(
                        onSuccess = { onSuccess() },
                        onFailure = { error ->
                            logger.error("JsonRpcInteractor: Cannot send the request, error: $error")
                            onFailure(Throwable("Publish error: ${error.message}"))
                        }
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("JsonRpcInteractor: Cannot send the request, exception: $e")
            onFailure(Throwable("Publish Request Error: $e"))
        }
    }

    override fun publishJsonRpcResponse(
        topic: Topic,
        params: IrnParams,
        response: JsonRpcResponse,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
        participants: Participants?,
        envelopeType: EnvelopeType,
    ) {
        try {
            checkNetworkConnectivity()
        } catch (e: NoConnectivityException) {
            return onFailure(e)
        }

        try {
            val responseJson = serializer.serialize(response) ?: throw IllegalStateException("RelayJsonRpcInteractor: Unknown Response Params")
            val encryptedResponse = chaChaPolyCodec.encrypt(topic, responseJson, envelopeType, participants)
            val encryptedResponseString = Base64.toBase64String(encryptedResponse)
            relay.publish(topic.value, encryptedResponseString, params.toRelay()) { result ->
                result.fold(
                    onSuccess = {
                        jsonRpcHistory.updateRequestWithResponse(response.id, responseJson)
                        onSuccess()
                    },
                    onFailure = { error ->
                        logger.error("JsonRpcInteractor: Cannot send the response, error: $error")
                        onFailure(Throwable("Publish error: ${error.message}"))
                    }
                )
            }
        } catch (e: Exception) {
            logger.error("JsonRpcInteractor: Cannot send the response, exception: $e")
            onFailure(Throwable("Publish Response Error: $e"))
        }
    }

    override fun subscribe(topic: Topic, onSuccess: (Topic) -> Unit, onFailure: (Throwable) -> Unit) {
        try {
            checkNetworkConnectivity()
        } catch (e: NoConnectivityException) {
            return onFailure(e)
        }

        try {
            backoffStrategy.shouldBackoff(true)
            relay.subscribe(topic.value) { result ->
                result.fold(
                    onSuccess = { acknowledgement ->
                        subscriptions[topic.value] = acknowledgement.result
                        onSuccess(topic)
                    },
                    onFailure = { error ->
                        logger.error("Subscribe to topic error: $topic error: $error")
                        onFailure(Throwable("Subscribe error: ${error.message}"))
                    }
                )
            }
        } catch (e: Exception) {
            logger.error("Subscribe to topic error: $topic error: $e")
            onFailure(Throwable("Subscribe error: ${e.message}"))
        }
    }

    override fun batchSubscribe(topics: List<String>, onSuccess: (List<String>) -> Unit, onFailure: (Throwable) -> Unit) {
        try {
            checkNetworkConnectivity()
        } catch (e: NoConnectivityException) {
            return onFailure(e)
        }
        if (topics.isEmpty()) {
            onSuccess(emptyList())
            return
        }
        backoffStrategy.shouldBackoff(true)
        val chunks = topics.chunked(BATCH_SUBSCRIBE_TOPIC_LIMIT)
        val completedChunks = AtomicInteger(0)
        val callbackDelivered = AtomicBoolean(false)

        chunks.forEach { chunk ->
            if (callbackDelivered.get()) return@forEach
            try {
                relay.batchSubscribe(chunk) { result ->
                    if (callbackDelivered.get()) return@batchSubscribe
                    result.fold(
                        onSuccess = { acknowledgement ->
                            val count = completedChunks.incrementAndGet()
                            if (!callbackDelivered.get()) {
                                synchronized(subscriptions) {
                                    subscriptions.plusAssign(chunk.zip(acknowledgement.result).toMap())
                                }
                            }
                            if (count == chunks.size && callbackDelivered.compareAndSet(false, true)) {
                                onSuccess(topics)
                            }
                        },
                        onFailure = { error ->
                            if (callbackDelivered.compareAndSet(false, true)) {
                                logger.error("Batch subscribe to topics error: $chunk error: $error")
                                onFailure(Throwable("Batch subscribe error: ${error.message}"))
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                if (callbackDelivered.compareAndSet(false, true)) {
                    logger.error("Batch subscribe to topics error: $chunk error: $e")
                    onFailure(Throwable("Batch subscribe error: ${e.message}"))
                }
            }
        }
    }

    override fun unsubscribe(topic: Topic, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) {
        try {
            checkNetworkConnectivity()
        } catch (e: NoConnectivityException) {
            return onFailure(e)
        }

        if (subscriptions.contains(topic.value)) {
            val subscriptionId = SubscriptionId(subscriptions[topic.value].toString())
            relay.unsubscribe(topic.value, subscriptionId.id) { result ->
                result.fold(
                    onSuccess = {
                        scope.launch {
                            supervisorScope {
                                jsonRpcHistory.deleteRecordsByTopic(topic)
                                subscriptions.remove(topic.value)
                                pushMessageStorage.deletePushMessagesByTopic(topic.value)
                                onSuccess()
                            }
                        }
                    },
                    onFailure = { error ->
                        logger.error("Unsubscribe to topic: $topic error: $error")
                        onFailure(Throwable("Unsubscribe error: ${error.message}"))
                    }
                )
            }
        }
    }

    override fun respondWithParams(
        request: WCRequest,
        clientParams: ClientParams,
        irnParams: IrnParams,
        envelopeType: EnvelopeType,
        participants: Participants?,
        onFailure: (Throwable) -> Unit,
        onSuccess: () -> Unit
    ) {
        val result = JsonRpcResponse.JsonRpcResult(id = request.id, result = clientParams)

        publishJsonRpcResponse(
            request.topic, irnParams, result, envelopeType = envelopeType, participants = participants,
            onFailure = { error -> onFailure(error) },
            onSuccess = { onSuccess() }
        )
    }

    override fun respondWithParams(
        requestId: Long,
        topic: Topic,
        clientParams: ClientParams,
        irnParams: IrnParams,
        envelopeType: EnvelopeType,
        participants: Participants?,
        onFailure: (Throwable) -> Unit,
        onSuccess: () -> Unit
    ) {
        val result = JsonRpcResponse.JsonRpcResult(id = requestId, result = clientParams)

        publishJsonRpcResponse(
            topic, irnParams, result, envelopeType = envelopeType, participants = participants,
            onFailure = { error -> onFailure(error) },
            onSuccess = { onSuccess() }
        )
    }

    // TODO: Can we replace this function with different parameters? Instead of request, just pass request id and request topic.
    override fun respondWithSuccess(
        request: WCRequest,
        irnParams: IrnParams,
        envelopeType: EnvelopeType,
        participants: Participants?,
    ) {
        val result = JsonRpcResponse.JsonRpcResult(id = request.id, result = true)

        try {
            publishJsonRpcResponse(
                request.topic, irnParams, result, envelopeType = envelopeType, participants = participants,
                onFailure = { error -> handleError("Cannot send the responseWithSuccess, error: ${error.stackTraceToString()}") })
        } catch (e: Exception) {
            handleError("publishFailure; ${e.stackTraceToString()}")
        }
    }

    // TODO: Can we replace this function with different parameters? Instead of request, just pass request id and request topic. And we never use WCRequest in the onSuccess callback so we can remove that as well
    override fun respondWithError(
        request: WCRequest,
        error: Error,
        irnParams: IrnParams,
        envelopeType: EnvelopeType,
        participants: Participants?,
        onSuccess: (WCRequest) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        logger.error("Responding with error: ${error.message}: ${error.code}")
        val jsonRpcError = JsonRpcResponse.JsonRpcError(id = request.id, error = JsonRpcResponse.Error(error.code, error.message))

        try {
            publishJsonRpcResponse(
                request.topic, irnParams, jsonRpcError, envelopeType = envelopeType, participants = participants,
                onSuccess = { onSuccess(request) },
                onFailure = { failure ->
                    onFailure(failure)
                    handleError("Cannot send respondWithError: ${failure.stackTraceToString()}")
                })
        } catch (e: Exception) {
            handleError("publishFailure; ${e.stackTraceToString()}")
        }
    }

    override fun respondWithError(
        requestId: Long,
        topic: Topic,
        error: Error,
        irnParams: IrnParams,
        envelopeType: EnvelopeType,
        participants: Participants?,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        logger.error("Responding with error: ${error.message}: ${error.code}")
        val jsonRpcError = JsonRpcResponse.JsonRpcError(id = requestId, error = JsonRpcResponse.Error(error.code, error.message))

        try {
            publishJsonRpcResponse(
                topic, irnParams, jsonRpcError, envelopeType = envelopeType, participants = participants,
                onSuccess = { onSuccess() },
                onFailure = { failure ->
                    onFailure(failure)
                    handleError("Cannot send respondWithError: ${failure.stackTraceToString()}")
                })
        } catch (e: Exception) {
            handleError("publishFailure; ${e.stackTraceToString()}")
        }
    }

    private fun manageSubscriptions() {
        scope.launch {
            relay.subscriptionRequest.map { relayRequest ->
                //TODO silences 4050
                if (relayRequest.tag == 4050) return@map Subscription(String.Empty, String.Empty, Topic(""), 0L, String.Empty)
                val topic = Topic(relayRequest.subscriptionTopic)
                storePushRequestsIfEnabled(relayRequest, topic)
                Subscription(decryptMessage(topic, relayRequest), relayRequest.message, topic, relayRequest.publishedAt, relayRequest.attestation)
            }.collect { subscription ->
                if (subscription.decryptedMessage.isNotEmpty()) {
                    try {
                        manageSubscriptions(subscription)
                    } catch (e: Exception) {
                        handleError("ManSub: ${e.stackTraceToString()}")
                    }
                }
            }
        }
    }

    private fun storePushRequestsIfEnabled(relayRequest: Relay.Model.Call.Subscription.Request, topic: Topic) {
        pushMessageStorage.arePushNotificationsEnabled
            .filter { areEnabled -> areEnabled }
            .onEach {
                pushMessageStorage.notificationTags
                    .filter { tag -> tag == relayRequest.tag }
                    .onEach { tag ->
                        pushMessageStorage.insertPushMessage(
                            sha256(relayRequest.message.toByteArray()),
                            topic.value,
                            relayRequest.message,
                            tag
                        )
                    }
            }.launchIn(scope)
    }

    private fun decryptMessage(topic: Topic, relayRequest: Relay.Model.Call.Subscription.Request): String =
        try {
            chaChaPolyCodec.decrypt(topic, Base64.decode(relayRequest.message))
        } catch (e: Exception) {
            handleError("ManSub: ${e.stackTraceToString()}")
            String.Empty
        }

    private suspend fun manageSubscriptions(subscription: Subscription) {
        serializer.tryDeserialize<ClientJsonRpc>(subscription.decryptedMessage)?.let { clientJsonRpc ->
            handleRequest(clientJsonRpc, subscription)
        } ?: serializer.tryDeserialize<JsonRpcResponse.JsonRpcResult>(subscription.decryptedMessage)?.let { result ->
            handleJsonRpcResult(result, subscription.topic)
        } ?: serializer.tryDeserialize<JsonRpcResponse.JsonRpcError>(subscription.decryptedMessage)?.let { error ->
            handleJsonRpcError(error)
        } ?: handleError("JsonRpcInteractor: Received unknown object type")
    }

    private suspend fun handleRequest(clientJsonRpc: ClientJsonRpc, subscription: Subscription) {
        if (jsonRpcHistory.setRequest(
                clientJsonRpc.id,
                subscription.topic,
                clientJsonRpc.method,
                subscription.decryptedMessage,
                TransportType.RELAY
            )
        ) {
            serializer.deserialize(clientJsonRpc.method, subscription.decryptedMessage)?.let { params ->
                _clientSyncJsonRpc.emit(subscription.toWCRequest(clientJsonRpc, params, TransportType.RELAY))
            } ?: handleError("JsonRpcInteractor: Unknown request params")
        }
    }

    private suspend fun handleJsonRpcResult(jsonRpcResult: JsonRpcResponse.JsonRpcResult, topic: Topic) {
        val serializedResult = serializer.serialize(jsonRpcResult) ?: return handleError("JsonRpcInteractor: Unknown result params")
        val jsonRpcRecord = jsonRpcHistory.updateRequestWithResponse(jsonRpcResult.id, serializedResult)

        if (jsonRpcRecord != null) {
            serializer.deserialize(jsonRpcRecord.method, jsonRpcRecord.body)?.let { params ->
                val responseVO = JsonRpcResponse.JsonRpcResult(jsonRpcResult.id, result = jsonRpcResult.result)
                _peerResponse.emit(jsonRpcRecord.toWCResponse(responseVO, params))
            } ?: handleError("JsonRpcInteractor: Unknown result params")
        } else {
            handleJsonRpcResponsesWithoutStoredRequest(jsonRpcResult, topic)
        }
    }

    private suspend fun handleJsonRpcResponsesWithoutStoredRequest(jsonRpcResult: JsonRpcResponse.JsonRpcResult, topic: Topic) {
        // todo: HANDLE DUPLICATES! maybe store results to check for duplicates????? https://github.com/WalletConnect/WalletConnectKotlinV2/issues/871
        //  Currently it's engine/usecase responsibility to handle duplicate responses
        if (jsonRpcResult.result is ChatNotifyResponseAuthParams.ResponseAuth) _peerResponse.emit(
            WCResponse(
                topic,
                String.Empty,
                jsonRpcResult,
                jsonRpcResult.result
            )
        )
    }

    private suspend fun handleJsonRpcError(jsonRpcError: JsonRpcResponse.JsonRpcError) {
        val serializedResult = serializer.serialize(jsonRpcError) ?: return handleError("JsonRpcInteractor: Unknown result params")
        val jsonRpcRecord = jsonRpcHistory.updateRequestWithResponse(jsonRpcError.id, serializedResult)

        if (jsonRpcRecord != null) {
            serializer.deserialize(jsonRpcRecord.method, jsonRpcRecord.body)?.let { params ->
                _peerResponse.emit(jsonRpcRecord.toWCResponse(jsonRpcError, params))
            } ?: handleError("JsonRpcInteractor: Unknown error params")
        }
    }

    private fun handleError(errorMessage: String) {
        logger.error("JsonRpcInteractor error: $errorMessage")
        scope.launch {
            _internalErrors.emit(SDKError(Throwable(errorMessage)))
        }
    }
}
