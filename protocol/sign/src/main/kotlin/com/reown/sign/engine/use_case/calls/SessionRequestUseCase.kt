package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.JsonRpcResponse
import com.reown.android.internal.common.exception.CannotFindSequenceForTopic
import com.reown.android.internal.common.exception.InvalidExpiryException
import com.reown.android.internal.common.json_rpc.domain.link_mode.LinkModeJsonRpcInteractorInterface
import com.reown.android.internal.common.model.AppMetaDataType
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.IrnParams
import com.reown.android.internal.common.model.Namespace
import com.reown.android.internal.common.model.SDKError
import com.reown.android.internal.common.model.Tags
import com.reown.android.internal.common.model.TransportType
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.android.internal.utils.CoreValidator
import com.reown.android.internal.utils.currentTimeInSeconds
import com.reown.android.internal.utils.fiveMinutesInSeconds
import com.reown.android.pulse.domain.InsertEventUseCase
import com.reown.android.pulse.model.Direction
import com.reown.android.pulse.model.EventType
import com.reown.android.pulse.model.properties.Properties
import com.reown.android.pulse.model.properties.Props
import com.reown.foundation.common.model.Topic
import com.reown.foundation.common.model.Ttl
import com.reown.foundation.util.Logger
import com.reown.sign.common.exceptions.InvalidRequestException
import com.reown.sign.common.exceptions.NO_SEQUENCE_FOR_TOPIC_MESSAGE
import com.reown.sign.common.exceptions.UnauthorizedMethodException
import com.reown.sign.common.model.vo.clientsync.session.SignRpc
import com.reown.sign.common.model.vo.clientsync.session.params.SignParams
import com.reown.sign.common.model.vo.clientsync.session.payload.SessionRequestVO
import com.reown.sign.common.validator.SignValidator
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.engine.model.tvf.EthSendTransaction
import com.reown.sign.storage.sequence.SessionStorageRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import com.reown.sign.engine.model.tvf.TVF as TVF1

internal class SessionRequestUseCase(
    private val sessionStorageRepository: SessionStorageRepository,
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val linkModeJsonRpcInteractor: LinkModeJsonRpcInteractorInterface,
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
    private val insertEventUseCase: InsertEventUseCase,
    private val clientId: String,
    private val logger: Logger,
    moshiBuilder: Moshi.Builder
) : SessionRequestUseCaseInterface {
    private val moshi: Moshi = moshiBuilder.build()
    private val _errors: MutableSharedFlow<SDKError> = MutableSharedFlow()
    override val errors: SharedFlow<SDKError> = _errors.asSharedFlow()

    override suspend fun sessionRequest(request: EngineDO.Request, onSuccess: (Long) -> Unit, onFailure: (Throwable) -> Unit) = supervisorScope {
        if (!sessionStorageRepository.isSessionValid(Topic(request.topic))) {
            return@supervisorScope onFailure(CannotFindSequenceForTopic("$NO_SEQUENCE_FOR_TOPIC_MESSAGE${request.topic}"))
        }

        val session = sessionStorageRepository.getSessionWithoutMetadataByTopic(Topic(request.topic))
            .run {
                val peerAppMetaData = metadataStorageRepository.getByTopicAndType(this.topic, AppMetaDataType.PEER)
                this.copy(peerAppMetaData = peerAppMetaData)
            }

        val nowInSeconds = currentTimeInSeconds
        if (!CoreValidator.isExpiryWithinBounds(request.expiry)) {
            logger.error("Sending session request error: expiry not within bounds")
            return@supervisorScope onFailure(InvalidExpiryException())
        }
        val expiry = request.expiry ?: Expiry(currentTimeInSeconds + fiveMinutesInSeconds)
        SignValidator.validateSessionRequest(request) { error ->
            logger.error("Sending session request error: invalid session request, ${error.message}")
            return@supervisorScope onFailure(InvalidRequestException(error.message))
        }

        val namespaces: Map<String, Namespace.Session> = sessionStorageRepository.getSessionWithoutMetadataByTopic(Topic(request.topic)).sessionNamespaces
        SignValidator.validateChainIdWithMethodAuthorisation(request.chainId, request.method, namespaces) { error ->
            logger.error("Sending session request error: unauthorized method, ${error.message}")
            return@supervisorScope onFailure(UnauthorizedMethodException(error.message))
        }

        val params = SignParams.SessionRequestParams(SessionRequestVO(request.method, request.params, expiry.seconds), request.chainId)
        val sessionPayload = SignRpc.SessionRequest(params = params)

        if (session.transportType == TransportType.LINK_MODE && session.peerLinkMode == true) {
            if (session.peerAppLink.isNullOrEmpty()) return@supervisorScope onFailure(IllegalStateException("App link is missing"))
            try {
                linkModeJsonRpcInteractor.triggerRequest(sessionPayload, Topic(request.topic), session.peerAppLink)
                insertEventUseCase(
                    Props(
                        EventType.SUCCESS,
                        Tags.SESSION_REQUEST_LINK_MODE.id.toString(),
                        Properties(correlationId = sessionPayload.id, clientId = clientId, direction = Direction.SENT.state)
                    )
                )
            } catch (e: Exception) {
                onFailure(e)
            }
        } else {
            val irnParamsTtl = expiry.run {
                val defaultTtl = fiveMinutesInSeconds
                val extractedTtl = seconds - nowInSeconds
                val newTtl = extractedTtl.takeIf { extractedTtl >= defaultTtl } ?: defaultTtl

                Ttl(newTtl)
            }

            println("kobe: params: ${sessionPayload.rpcParams}")

            val contractAddresses = if (sessionPayload.rpcMethod == "eth_sendTransaction") {
                try {
                    val test = moshi.adapter(Array<EthSendTransaction>::class.java).fromJson(sessionPayload.rpcParams)
                    println("kobe: payload: ${test?.get(0)}")
                    listOf(test?.get(0)?.to ?: "")
                } catch (e: Exception) {
                    println("kobe: error: $e")
                    listOf("")
                }
            } else {
                null
            }

            println("kobe: rpcMethods: ${sessionPayload.rpcMethod}; contractAddresses: $contractAddresses; chainId: ${sessionPayload.params.chainId}")

            val irnParams = IrnParams(
                Tags.SESSION_REQUEST,
                irnParamsTtl,
                correlationId = sessionPayload.id.toString(),
                rpcMethods = listOf(sessionPayload.rpcMethod),
                contractAddresses = contractAddresses,
                chainId = sessionPayload.params.chainId,
                prompt = true
            )
            val requestTtlInSeconds = expiry.run { seconds - nowInSeconds }

            logger.log("Sending session request on topic: ${request.topic}}")
            jsonRpcInteractor.publishJsonRpcRequest(Topic(request.topic), irnParams, sessionPayload,
                onSuccess = {
                    logger.log("Session request sent successfully on topic: ${request.topic}")
                    onSuccess(sessionPayload.id)
                    scope.launch {
                        try {
                            withTimeout(TimeUnit.SECONDS.toMillis(requestTtlInSeconds)) {
                                collectResponse(sessionPayload.id) { cancel() }
                            }
                        } catch (e: TimeoutCancellationException) {
                            _errors.emit(SDKError(e))
                        }
                    }
                },
                onFailure = { error ->
                    logger.error("Sending session request error: $error")
                    onFailure(error)
                }
            )
        }
    }

    private suspend fun collectResponse(id: Long, onResponse: (Result<JsonRpcResponse.JsonRpcResult>) -> Unit = {}) {
        jsonRpcInteractor.peerResponse
            .filter { response -> response.response.id == id }
            .collect { response ->
                when (val result = response.response) {
                    is JsonRpcResponse.JsonRpcResult -> onResponse(Result.success(result))
                    is JsonRpcResponse.JsonRpcError -> onResponse(Result.failure(Throwable(result.errorMessage)))
                }
            }
    }
}

internal interface SessionRequestUseCaseInterface {
    val errors: SharedFlow<SDKError>
    suspend fun sessionRequest(request: EngineDO.Request, onSuccess: (Long) -> Unit, onFailure: (Throwable) -> Unit)
}