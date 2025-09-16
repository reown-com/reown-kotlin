package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.exception.CannotFindSequenceForTopic
import com.reown.android.internal.common.model.AppMetaDataType
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.Namespace
import com.reown.android.internal.common.model.SDKError
import com.reown.android.internal.common.model.type.EngineEvent
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.android.internal.utils.currentTimeInSeconds
import com.reown.android.internal.utils.fiveMinutesInSeconds
import com.reown.foundation.common.model.Topic
import com.reown.foundation.common.model.Ttl
import com.reown.foundation.util.Logger
import com.reown.sign.client.Sign
import com.reown.sign.common.exceptions.NO_SEQUENCE_FOR_TOPIC_MESSAGE
import com.reown.sign.common.exceptions.UnauthorizedMethodException
import com.reown.sign.common.model.vo.clientsync.session.SignRpc
import com.reown.sign.common.validator.SignValidator
import com.reown.sign.engine.domain.wallet_service.WalletServiceFinder
import com.reown.sign.engine.domain.wallet_service.WalletServiceRequester
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.supervisorScope
import uniffi.yttrium.SessionRequestFfi
import uniffi.yttrium.SessionRequestJsonRpcFfi
import uniffi.yttrium.SessionRequestRequestFfi
import uniffi.yttrium.SignClient

internal class SessionRequestUseCase(
    private val sessionStorageRepository: SessionStorageRepository,
//    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
//    private val linkModeJsonRpcInteractor: LinkModeJsonRpcInteractorInterface,
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
//    private val insertEventUseCase: InsertEventUseCase,
//    private val clientId: String,
    private val logger: Logger,
//    private val tvf: TVF,
    private val walletServiceFinder: WalletServiceFinder,
    private val walletServiceRequester: WalletServiceRequester,
    private val signClient: SignClient
) : SessionRequestUseCaseInterface {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    override val requestEvents: SharedFlow<EngineEvent> = _events.asSharedFlow()

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

//        val nowInSeconds = currentTimeInSeconds
//        if (!CoreValidator.isExpiryWithinBounds(request.expiry)) {
//            logger.error("Sending session request error: expiry not within bounds")
//            return@supervisorScope onFailure(InvalidExpiryException())
//        }
//        val expiry = request.expiry ?: Expiry(currentTimeInSeconds + fiveMinutesInSeconds)
//        SignValidator.validateSessionRequest(request) { error ->
//            logger.error("Sending session request error: invalid session request, ${error.message}")
//            return@supervisorScope onFailure(InvalidRequestException(error.message))
//        }

        val namespaces: Map<String, Namespace.Session> =
            sessionStorageRepository.getSessionWithoutMetadataByTopic(Topic(request.topic)).sessionNamespaces
        SignValidator.validateChainIdWithMethodAuthorisation(request.chainId, request.method, namespaces) { error ->
            logger.error("Sending session request error: unauthorized method, ${error.message}")
            return@supervisorScope onFailure(UnauthorizedMethodException(error.message))
        }

        val sessionRequestFfi = SessionRequestFfi(
            chainId = request.chainId,
            request = SessionRequestRequestFfi(
                method = request.method,
                params = request.params,
                expiry = request.expiry?.seconds?.toULong()
            )
        )

        try {
            val id = async { signClient.request(topic = request.topic, sessionRequest = sessionRequestFfi) }.await()
            onSuccess(id.toLong())
        } catch (e: Exception) {
            logger.error("Sending session request error: $e")
            onFailure(e)
        }


//        val params = SignParams.SessionRequestParams(SessionRequestVO(request.method, request.params, expiry.seconds), request.chainId)
//        val sessionPayload = SignRpc.SessionRequest(params = params)
//        val walletServiceUrl = try {
//            walletServiceFinder.findMatchingWalletService(request, session)
//        } catch (e: Exception) {
//            null
//        }


//        if (walletServiceUrl != null) {
//            try {
//                val response = async { walletServiceRequester.request(sessionPayload, walletServiceUrl.toString()) }.await()
//                val jsonRpcResult = EngineDO.JsonRpcResponse.JsonRpcResult(id = sessionPayload.id, result = response)
//                _events.emit(EngineDO.SessionPayloadResponse(request.topic, params.chainId, request.method, jsonRpcResult))
//            } catch (e: Exception) {
//                logger.error("Sending session request error: $e")
//                val jsonRpcResult =
//                    EngineDO.JsonRpcResponse.JsonRpcError(id = sessionPayload.id, error = EngineDO.JsonRpcResponse.Error(0, e.message ?: ""))
//                _events.emit(EngineDO.SessionPayloadResponse(request.topic, params.chainId, request.method, jsonRpcResult))
//            }
//        } else {
//            if (session.transportType == TransportType.LINK_MODE && session.peerLinkMode == true) {
//                if (session.peerAppLink.isNullOrEmpty()) return@supervisorScope onFailure(IllegalStateException("App link is missing"))
//                triggerLinkModeRequest(sessionPayload, request, session.peerAppLink, onFailure)
//            } else {
//                triggerRelayRequest(expiry, nowInSeconds, sessionPayload, request, onSuccess, onFailure)
//            }
//        }
    }

    private fun triggerRelayRequest(
        expiry: Expiry,
        nowInSeconds: Long,
        sessionPayload: SignRpc.SessionRequest,
        request: EngineDO.Request,
        onSuccess: (Long) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val irnParamsTtl = expiry.run {
            val defaultTtl = fiveMinutesInSeconds
            val extractedTtl = seconds - nowInSeconds
            val newTtl = extractedTtl.takeIf { extractedTtl >= defaultTtl } ?: defaultTtl

            Ttl(newTtl)
        }

//        val tvfData = tvf.collect(sessionPayload.rpcMethod, sessionPayload.rpcParams, sessionPayload.params.chainId)
//        val irnParams = IrnParams(
//            Tags.SESSION_REQUEST,
//            irnParamsTtl,
//            correlationId = sessionPayload.id,
//            rpcMethods = tvfData.first,
//            contractAddresses = tvfData.second,
//            chainId = tvfData.third,
//            prompt = true
//        )
//        val requestTtlInSeconds = expiry.run { seconds - nowInSeconds }

        logger.log("Sending session request on topic: ${request.topic}}")
//        jsonRpcInteractor.publishJsonRpcRequest(
//            Topic(request.topic), irnParams, sessionPayload,
//            onSuccess = {
//                logger.log("Session request sent successfully on topic: ${request.topic}")
//                onSuccess(sessionPayload.id)
//                scope.launch {
//                    try {
//                        withTimeout(TimeUnit.SECONDS.toMillis(requestTtlInSeconds)) {
//                            collectResponse(sessionPayload.id) { cancel() }
//                        }
//                    } catch (e: TimeoutCancellationException) {
//                        _errors.emit(SDKError(e))
//                    }
//                }
//            },
//            onFailure = { error ->
//                logger.error("Sending session request error: $error")
//                onFailure(error)
//            }
//        )
    }

    private suspend fun SessionRequestUseCase.triggerLinkModeRequest(
        sessionPayload: SignRpc.SessionRequest,
        request: EngineDO.Request,
        peerAppLink: String,
        onFailure: (Throwable) -> Unit
    ) {
        try {
//            linkModeJsonRpcInteractor.triggerRequest(sessionPayload, Topic(request.topic), peerAppLink)
//            insertEventUseCase(
//                Props(
//                    EventType.SUCCESS,
//                    Tags.SESSION_REQUEST_LINK_MODE.id.toString(),
//                    Properties(correlationId = sessionPayload.id, clientId = clientId, direction = Direction.SENT.state)
//                )
//            )
        } catch (e: Exception) {
            onFailure(e)
        }
    }

//    private suspend fun collectResponse(id: Long, onResponse: (Result<JsonRpcResponse.JsonRpcResult>) -> Unit = {}) {
//        jsonRpcInteractor.peerResponse
//            .filter { response -> response.response.id == id }
//            .collect { response ->
//                when (val result = response.response) {
//                    is JsonRpcResponse.JsonRpcResult -> onResponse(Result.success(result))
//                    is JsonRpcResponse.JsonRpcError -> onResponse(Result.failure(Throwable(result.errorMessage)))
//                }
//            }
//    }
}

internal interface SessionRequestUseCaseInterface {
    val errors: SharedFlow<SDKError>
    val requestEvents: SharedFlow<EngineEvent>
    suspend fun sessionRequest(request: EngineDO.Request, onSuccess: (Long) -> Unit, onFailure: (Throwable) -> Unit)
}