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
import com.reown.sign.engine.model.tvf.EthSendTransaction
import com.reown.sign.engine.model.tvf.SolanaSignAllTransactionsResult
import com.reown.sign.engine.model.tvf.SolanaSignAndSendTransactionResult
import com.reown.sign.engine.model.tvf.SolanaSignTransactionResult
import com.reown.sign.engine.model.tvf.TVF
import com.reown.sign.engine.sessionRequestEventsQueue
import com.reown.sign.json_rpc.domain.GetPendingJsonRpcHistoryEntryByIdUseCase
import com.reown.sign.storage.sequence.SessionStorageRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

internal class RespondSessionRequestUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val getPendingJsonRpcHistoryEntryByIdUseCase: GetPendingJsonRpcHistoryEntryByIdUseCase,
    private val linkModeJsonRpcInteractor: LinkModeJsonRpcInteractorInterface,
    private val logger: Logger,
    private val verifyContextStorageRepository: VerifyContextStorageRepository,
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
    private val insertEventUseCase: InsertEventUseCase,
    private val clientId: String,
    moshiBuilder: Moshi.Builder
) : RespondSessionRequestUseCaseInterface {
    private val moshi: Moshi = moshiBuilder.build()
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    override val events: SharedFlow<EngineEvent> = _events.asSharedFlow()
    override suspend fun respondSessionRequest(
        topic: String,
        jsonRpcResponse: JsonRpcResponse,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) = supervisorScope {
        val topicWrapper = Topic(topic)
        if (!sessionStorageRepository.isSessionValid(topicWrapper)) {
            logger.error("Request response -  invalid session: $topic, id: ${jsonRpcResponse.id}")
            return@supervisorScope onFailure(CannotFindSequenceForTopic("$NO_SEQUENCE_FOR_TOPIC_MESSAGE$topic"))
        }
        val session = sessionStorageRepository.getSessionWithoutMetadataByTopic(topicWrapper)
            .run {
                val peerAppMetaData = metadataStorageRepository.getByTopicAndType(this.topic, AppMetaDataType.PEER)
                this.copy(peerAppMetaData = peerAppMetaData)
            }

        val pendingRequest = getPendingJsonRpcHistoryEntryByIdUseCase(jsonRpcResponse.id)
        if (pendingRequest == null) {
            logger.error("Request doesn't exist: $topic, id: ${jsonRpcResponse.id}")
            throw RequestExpiredException("This request has expired, id: ${jsonRpcResponse.id}")
        }
        pendingRequest.params.expiry?.let {
            if (Expiry(it).isExpired()) {
                logger.error("Request Expired: $topic, id: ${jsonRpcResponse.id}")
                throw RequestExpiredException("This request has expired, id: ${jsonRpcResponse.id}")
            }
        }

        if (session.transportType == TransportType.LINK_MODE && session.peerLinkMode == true) {
            if (session.peerAppLink.isNullOrEmpty()) return@supervisorScope onFailure(IllegalStateException("App link is missing"))
            try {
                removePendingSessionRequestAndEmit(jsonRpcResponse.id)
                linkModeJsonRpcInteractor.triggerResponse(Topic(topic), jsonRpcResponse, session.peerAppLink)
                insertEventUseCase(
                    Props(
                        EventType.SUCCESS,
                        Tags.SESSION_REQUEST_LINK_MODE_RESPONSE.id.toString(),
                        Properties(correlationId = jsonRpcResponse.id, clientId = clientId, direction = Direction.SENT.state)
                    )
                )
            } catch (e: Exception) {
                onFailure(e)
            }
        } else {

            val contractAddresses = if (pendingRequest.params.rpcMethod == "eth_sendTransaction") {
                try {
                    val test = moshi.adapter(Array<EthSendTransaction>::class.java).fromJson(pendingRequest.params.request.params)
                    println("kobe: payload: ${test?.get(0)}")
                    listOf(test?.get(0)?.to ?: "")
                } catch (e: Exception) {
                    println("kobe: error: $e")
                   listOf("")
                }
            } else {
                null
            }

            val txHashes = if (jsonRpcResponse is JsonRpcResponse.JsonRpcResult) {
                when (pendingRequest.params.rpcMethod) {
                    "eth_sendTransaction", "eth_sendRawTransaction" -> {
                        val txHash = jsonRpcResponse.result.toString()

                        listOf(txHash)
                    }

                    "wallet_sendCalls" -> {
                        val txHash = jsonRpcResponse.result.toString()

                        println("kobe: wallet send calls result: $txHash")
                        listOf(txHash)
                    }

                    "solana_signTransaction" -> {
                        try {
                            val result = moshi.adapter(SolanaSignTransactionResult::class.java).fromJson(jsonRpcResponse.result as String)?.signature ?: ""
                            println("kobe: solana sign result: $result")
                            listOf(result)
                        } catch (e: Exception) {
                            println("kobe: solana sign error: $e")
                            listOf("")
                        }

                    }

                    "solana_signAndSendTransaction" -> { //todo: test
                        try {
                            val result = moshi.adapter(SolanaSignAndSendTransactionResult::class.java).fromJson(jsonRpcResponse.result as String)
                            println("kobe: solana sign and send result: $result")
                            listOf(result?.signature ?: "")
                        } catch (e: Exception) {
                            println("kobe: sign send error: $e")
                            listOf("")
                        }
                    }

                    "solana_signAllTransactions" -> {
                        try {
                            println("kobe: solana sign all result: ${jsonRpcResponse.result}")
                            val result = moshi.adapter(SolanaSignAllTransactionsResult::class.java).fromJson(jsonRpcResponse.result as String)
                            println("kobe: solana sign all result: $result")
                            result?.transactions ?: listOf("")
                        } catch (e: Exception) {
                            println("kobe: solana sign all error: $e")
                            listOf("")
                        }
                    }

                    else -> null
                }
            } else {
                null
            }

            println("kobe: rpcMethods: ${pendingRequest.params.rpcMethod}; contractAddresses: $contractAddresses; chainId: ${pendingRequest.params.chainId}, txHashes: $txHashes")

            val irnParams = IrnParams(
                Tags.SESSION_REQUEST_RESPONSE,
                Ttl(fiveMinutesInSeconds),
                correlationId = jsonRpcResponse.id.toString(),
                rpcMethods = listOf(pendingRequest.params.rpcMethod),
                contractAddresses = contractAddresses,
                txHashes = txHashes,
                chainId = pendingRequest.params.chainId
            )
            logger.log("Sending session request response on topic: $topic, id: ${jsonRpcResponse.id}")
            jsonRpcInteractor.publishJsonRpcResponse(topic = Topic(topic), params = irnParams, response = jsonRpcResponse,
                onSuccess = {
                    onSuccess()
                    logger.log("Session request response sent successfully on topic: $topic, id: ${jsonRpcResponse.id}")
                    scope.launch {
                        supervisorScope {
                            removePendingSessionRequestAndEmit(jsonRpcResponse.id)
                        }
                    }
                },
                onFailure = { error ->
                    logger.error("Sending session response error: $error, id: ${jsonRpcResponse.id}")
                    onFailure(error)
                }
            )
        }
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