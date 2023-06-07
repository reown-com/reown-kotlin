@file:JvmSynthetic

package com.walletconnect.push.wallet.engine

import android.content.res.Resources.NotFoundException
import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import com.walletconnect.android.CoreClient
import com.walletconnect.android.internal.common.JsonRpcResponse
import com.walletconnect.android.internal.common.crypto.codec.Codec
import com.walletconnect.android.internal.common.crypto.kmr.KeyManagementRepository
import com.walletconnect.android.internal.common.exception.Uncategorized
import com.walletconnect.android.internal.common.explorer.ExplorerRepository
import com.walletconnect.android.internal.common.explorer.data.model.Listing
import com.walletconnect.android.internal.common.json_rpc.data.JsonRpcSerializer
import com.walletconnect.android.internal.common.jwt.did.EncodeDidJwtPayloadUseCase
import com.walletconnect.android.internal.common.jwt.did.encodeDidJwt
import com.walletconnect.android.internal.common.jwt.did.extractVerifiedDidJwtClaims
import com.walletconnect.android.internal.common.model.AccountId
import com.walletconnect.android.internal.common.model.ConnectionState
import com.walletconnect.android.internal.common.model.DidJwt
import com.walletconnect.android.internal.common.model.EnvelopeType
import com.walletconnect.android.internal.common.model.Expiry
import com.walletconnect.android.internal.common.model.IrnParams
import com.walletconnect.android.internal.common.model.Participants
import com.walletconnect.android.internal.common.model.Redirect
import com.walletconnect.android.internal.common.model.SDKError
import com.walletconnect.android.internal.common.model.Tags
import com.walletconnect.android.internal.common.model.WCRequest
import com.walletconnect.android.internal.common.model.WCResponse
import com.walletconnect.android.internal.common.model.params.PushParams
import com.walletconnect.android.internal.common.model.sync.ClientJsonRpc
import com.walletconnect.android.internal.common.model.type.EngineEvent
import com.walletconnect.android.internal.common.model.type.JsonRpcInteractorInterface
import com.walletconnect.android.internal.common.scope
import com.walletconnect.android.internal.common.wcKoinApp
import com.walletconnect.android.internal.utils.CURRENT_TIME_IN_SECONDS
import com.walletconnect.android.internal.utils.DAY_IN_SECONDS
import com.walletconnect.android.keyserver.domain.IdentitiesInteractor
import com.walletconnect.android.pairing.handler.PairingControllerInterface
import com.walletconnect.foundation.common.model.PublicKey
import com.walletconnect.foundation.common.model.Topic
import com.walletconnect.foundation.common.model.Ttl
import com.walletconnect.foundation.util.Logger
import com.walletconnect.push.common.JsonRpcMethod
import com.walletconnect.push.common.PeerError
import com.walletconnect.push.common.calcExpiry
import com.walletconnect.push.common.data.jwt.EncodePushAuthDidJwtPayloadUseCase
import com.walletconnect.push.common.data.jwt.PushSubscriptionJwtClaim
import com.walletconnect.push.common.data.storage.SubscriptionStorageRepository
import com.walletconnect.push.common.domain.ExtractPushConfigUseCase
import com.walletconnect.push.common.model.EngineDO
import com.walletconnect.push.common.model.PushRpc
import com.walletconnect.push.common.model.toEngineDO
import com.walletconnect.push.wallet.data.MessagesRepository
import com.walletconnect.push.wallet.data.wellknown.did.DidJsonDTO
import com.walletconnect.push.wallet.engine.domain.calls.ApproveUseCaseInterface
import com.walletconnect.push.wallet.engine.domain.calls.DecryptMessageUseCaseInterface
import com.walletconnect.push.wallet.engine.domain.calls.DeleteMessageUseCaseInterface
import com.walletconnect.push.wallet.engine.domain.calls.DeleteSubscriptionUseCaseInterface
import com.walletconnect.push.wallet.engine.domain.calls.GetListOfActiveSubscriptionsUseCaseInterface
import com.walletconnect.push.wallet.engine.domain.calls.GetListOfMessagesUseCaseInterface
import com.walletconnect.push.wallet.engine.domain.calls.RejectUseCaseInterface
import com.walletconnect.push.wallet.engine.domain.calls.SubscribeToDappUseCaseInterface
import com.walletconnect.push.wallet.engine.domain.calls.UpdateUseCaseInterface
import com.walletconnect.util.bytesToHex
import com.walletconnect.util.generateId
import com.walletconnect.util.hexToBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

internal class PushWalletEngine(
    private val jsonRpcInteractor: JsonRpcInteractorInterface,
    private val crypto: KeyManagementRepository,
    private val pairingHandler: PairingControllerInterface,
    private val subscriptionStorageRepository: SubscriptionStorageRepository,
    private val messagesRepository: MessagesRepository,
    private val identitiesInteractor: IdentitiesInteractor,
    private val serializer: JsonRpcSerializer,
    private val explorerRepository: ExplorerRepository,
    private val extractPushConfigUseCase: ExtractPushConfigUseCase,
    private val subscriptToDappUseCase: SubscribeToDappUseCaseInterface,
    private val approveUseCase: ApproveUseCaseInterface,
    private val rejectUseCase: RejectUseCaseInterface,
    private val updateUseCase: UpdateUseCaseInterface,
    private val deleteSubscriptionUseCaseInterface: DeleteSubscriptionUseCaseInterface,
    private val deleteMessageUseCaseInterface: DeleteMessageUseCaseInterface,
    private val decryptMessageUseCase: DecryptMessageUseCaseInterface,
    private val getListOfActiveSubscriptionsUseCaseInterface: GetListOfActiveSubscriptionsUseCaseInterface,
    private val getListOfMessagesUseCaseInterface: GetListOfMessagesUseCaseInterface,
    private val logger: Logger,
) : SubscribeToDappUseCaseInterface by subscriptToDappUseCase,
    ApproveUseCaseInterface by approveUseCase,
    RejectUseCaseInterface by rejectUseCase,
    UpdateUseCaseInterface by updateUseCase,
    DeleteSubscriptionUseCaseInterface by deleteSubscriptionUseCaseInterface,
    DeleteMessageUseCaseInterface by deleteMessageUseCaseInterface,
    DecryptMessageUseCaseInterface by decryptMessageUseCase,
    GetListOfActiveSubscriptionsUseCaseInterface by getListOfActiveSubscriptionsUseCaseInterface,
    GetListOfMessagesUseCaseInterface by getListOfMessagesUseCaseInterface {
    private var jsonRpcRequestsJob: Job? = null
    private var jsonRpcResponsesJob: Job? = null
    private var internalErrorsJob: Job? = null
    private val _engineEvent: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val engineEvent: SharedFlow<EngineEvent> = _engineEvent.asSharedFlow()

    init {
        pairingHandler.register(
            JsonRpcMethod.WC_PUSH_REQUEST,
            JsonRpcMethod.WC_PUSH_MESSAGE
        )
    }

    fun setup() {
        jsonRpcInteractor.isConnectionAvailable
            .onEach { isAvailable -> _engineEvent.emit(ConnectionState(isAvailable)) }
            .filter { isAvailable: Boolean -> isAvailable }
            .onEach {
                supervisorScope {
                    launch(Dispatchers.IO) {
                        resubscribeToSubscriptions()
                    }
                }

                if (jsonRpcRequestsJob == null) {
                    jsonRpcRequestsJob = collectJsonRpcRequests()
                }

                if (jsonRpcResponsesJob == null) {
                    jsonRpcResponsesJob = collectJsonRpcResponses()
                }

                if (internalErrorsJob == null) {
                    internalErrorsJob = collectInternalErrors()
                }
            }
            .launchIn(scope)
    }

    fun decryptMessage(topic: String, message: String, onSuccess: (EngineDO.PushMessage) -> Unit, onFailure: (Throwable) -> Unit) {
        try {
            val codec = wcKoinApp.koin.get<Codec>()
            val decryptedMessageString = codec.decrypt(Topic(topic), message)
            // How to look in JsonRpcHistory for dupes without Rpc ID
            val clientJsonRpc = serializer.tryDeserialize<ClientJsonRpc>(decryptedMessageString) ?: return onFailure(IllegalArgumentException("Unable to deserialize message"))
            val pushMessage = serializer.deserialize(clientJsonRpc.method, decryptedMessageString)
            val pushMessageEngineDO = PushParams.MessageParams::class.safeCast(pushMessage)?.toEngineDO() ?: return onFailure(IllegalArgumentException("Unable to deserialize message"))

            onSuccess(pushMessageEngineDO)
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    suspend fun getListOfActiveSubscriptions(): Map<String, EngineDO.PushSubscription> =
        subscriptionStorageRepository.getAllSubscriptions()
            .filter { subscription -> subscription.subscriptionTopic?.value.isNullOrBlank().not() }
            .associateBy { subscription -> subscription.subscriptionTopic!!.value }

    suspend fun getListOfMessages(topic: String): Map<Long, EngineDO.PushRecord> = supervisorScope {
        messagesRepository.getMessagesByTopic(topic).map { messageRecord ->
            EngineDO.PushRecord(
                id = messageRecord.id,
                topic = messageRecord.topic,
                publishedAt = messageRecord.publishedAt,
                message = EngineDO.PushMessage(
                    title = messageRecord.message.title,
                    body = messageRecord.message.body,
                    icon = messageRecord.message.icon,
                    url = messageRecord.message.url,
                    type = messageRecord.message.type,
                )
            )
        }.associateBy { pushRecord ->
            pushRecord.id
        }
    }

    private suspend fun collectJsonRpcRequests(): Job =
        jsonRpcInteractor.clientSyncJsonRpc
            .filter { request -> request.params is PushParams }
            .onEach { request ->
                when (val requestParams = request.params) {
                    is PushParams.RequestParams -> onPushRequest(request, requestParams)
                    is PushParams.MessageParams -> onPushMessage(request, requestParams)
                    is PushParams.ProposeParams -> onPushPropose(request, requestParams)
                    is PushParams.DeleteParams -> onPushDelete(request)
                }
            }.launchIn(scope)

    private fun collectJsonRpcResponses(): Job =
        jsonRpcInteractor.peerResponse.onEach { response ->
            when (val responseParams = response.params) {
                is PushParams.DeleteParams -> onPushDeleteResponse()
                is PushParams.SubscribeParams -> onPushSubscribeResponse(response)
                is PushParams.UpdateParams -> onPushUpdateResponse(response, responseParams)
            }
        }.launchIn(scope)

    private fun collectInternalErrors(): Job =
        merge(jsonRpcInteractor.internalErrors, pairingHandler.findWrongMethodsFlow)
            .onEach { exception -> _engineEvent.emit(exception) }
            .launchIn(scope)

    private suspend fun onPushRequest(request: WCRequest, params: PushParams.RequestParams) = supervisorScope {
        val irnParams = IrnParams(Tags.PUSH_REQUEST_RESPONSE, Ttl(DAY_IN_SECONDS))

        try {
            subscriptionStorageRepository.insertSubscription(
                requestId = request.id,
                keyAgreementTopic = "",
                responseTopic = request.topic.value,
                peerPublicKeyAsHex = params.publicKey,
                subscriptionTopic = null,
                account = params.account,
                relayProtocol = null,
                relayData = null,
                name = params.metaData.name,
                description = params.metaData.description,
                url = params.metaData.url,
                icons = params.metaData.icons,
                native = params.metaData.redirect?.native,
                "",
                emptyMap(),
                calcExpiry()
            )

            val newSubscription = subscriptionStorageRepository.getSubscriptionsByRequestId(request.id)

            _engineEvent.emit(
                EngineDO.PushRequest(
                    newSubscription.requestId,
                    newSubscription.responseTopic.value,
                    newSubscription.account.value,
                    newSubscription.relay,
                    newSubscription.metadata
                )
            )
        } catch (e: Exception) {
            jsonRpcInteractor.respondWithError(
                request,
                Uncategorized.GenericError("Cannot handle the push request: ${e.message}, topic: ${request.topic}"),
                irnParams
            )

            _engineEvent.emit(SDKError(e))
        }
    }

    private suspend fun onPushPropose(request: WCRequest, params: PushParams.ProposeParams) = supervisorScope {
        val irnParams = IrnParams(Tags.PUSH_PROPOSE_RESPONSE, Ttl(DAY_IN_SECONDS))

        val dappPushScope: List<EngineDO.PushScope.Remote> = extractPushConfigUseCase.invoke(params.metaData.url.toUri()).getOrElse {
            return@supervisorScope _engineEvent.emit(SDKError(Exception("")))
        }
        val mapOfScope: Map<String, EngineDO.PushScope.Cached> = dappPushScope.associate {
            it.name to EngineDO.PushScope.Cached(it.name, it.description, it.name in params.scope)
        }

        try {
            subscriptionStorageRepository.insertSubscription(
                requestId = request.id,
                keyAgreementTopic = request.topic.value,
                responseTopic = sha256(params.publicKey.hexToBytes()),
                peerPublicKeyAsHex = params.publicKey,
                subscriptionTopic = null,
                account = params.account,
                relayProtocol = null,
                relayData = null,
                name = params.metaData.name,
                description = params.metaData.description,
                url = params.metaData.url,
                icons = params.metaData.icons,
                native = params.metaData.redirect?.native,
                didJwt = "",
                mapOfScope = mapOfScope.mapValues { entry -> entry.value.description to entry.value.isSelected },
                expiry = calcExpiry()
            )

            val newSubscription = subscriptionStorageRepository.getSubscriptionsByRequestId(request.id)

            _engineEvent.emit(
                EngineDO.PushPropose(
                    newSubscription.requestId,
                    newSubscription.responseTopic.value,
                    newSubscription.account.value,
                    newSubscription.relay,
                    newSubscription.metadata
                )
            )
        } catch (e: Exception) {
            jsonRpcInteractor.respondWithError(
                request,
                Uncategorized.GenericError("Cannot handle the push request: ${e.message}, topic: ${request.topic}"),
                irnParams
            )

            _engineEvent.emit(SDKError(e))
        }
    }

    private suspend fun onPushMessage(request: WCRequest, params: PushParams.MessageParams) = supervisorScope {
        val irnParams = IrnParams(Tags.PUSH_MESSAGE_RESPONSE, Ttl(DAY_IN_SECONDS))

        try {
            jsonRpcInteractor.respondWithSuccess(request, irnParams)
            // TODO: refactor to use the RPC published at value 
            val currentTime = CURRENT_TIME_IN_SECONDS
            messagesRepository.insertMessage(request.id, request.topic.value, currentTime, params.title, params.body, params.icon, params.url, params.type)
            val messageRecord = EngineDO.PushRecord(
                id = request.id,
                topic = request.topic.value,
                publishedAt = currentTime,
                message = params.toEngineDO()
            )
            scope.launch { _engineEvent.emit(messageRecord) }
        } catch (e: Exception) {
            jsonRpcInteractor.respondWithError(
                request,
                Uncategorized.GenericError("Cannot handle the push message: ${e.message}, topic: ${request.topic}"),
                irnParams
            )
        }
    }

    private suspend fun onPushDelete(request: WCRequest) = supervisorScope {
        val irnParams = IrnParams(Tags.PUSH_DELETE_RESPONSE, Ttl(DAY_IN_SECONDS))

        try {
            jsonRpcInteractor.respondWithSuccess(request, irnParams)
            jsonRpcInteractor.unsubscribe(request.topic)
            subscriptionStorageRepository.deleteSubscription(request.topic.value)

            scope.launch { _engineEvent.emit(EngineDO.PushDelete(request.topic.value)) }
        } catch (e: Exception) {
            scope.launch { _engineEvent.emit(SDKError(e)) }
        }
    }

    private fun onPushDeleteResponse() {
        // TODO: Review if we need this
    }

    private suspend fun onPushSubscribeResponse(wcResponse: WCResponse) = supervisorScope {
        try {
            when (val response = wcResponse.response) {
                is JsonRpcResponse.JsonRpcResult -> {
                    val subscription = subscriptionStorageRepository.getAllSubscriptions().firstOrNull { it.responseTopic.value == wcResponse.topic.value } ?: return@supervisorScope _engineEvent.emit(
                        SDKError(NotFoundException("Cannot find subscription for topic: ${wcResponse.topic.value}"))
                    )
                    val selfPublicKey = crypto.getSelfPublicFromKeyAgreement(subscription.keyAgreementTopic)
                    val dappPublicKey = PublicKey(
                        (((wcResponse.response as JsonRpcResponse.JsonRpcResult).result as Map<*, *>)["publicKey"] as String)
                    ) // TODO: Add an entry in JsonRpcResultAdapter and create data class for response
                    val pushTopic = crypto.generateTopicFromKeyAgreement(selfPublicKey, dappPublicKey)
                    val updatedExpiry = calcExpiry()
                    subscriptionStorageRepository.updateSubscriptionToResponded(subscription.responseTopic.value, pushTopic.value, dappPublicKey.keyAsHex, updatedExpiry)

                    _engineEvent.emit(
                        subscription.copy(
                            subscriptionTopic = pushTopic,
                            peerPublicKey = PublicKey(dappPublicKey.keyAsHex),
                            expiry = Expiry(updatedExpiry)
                        )
                    )
                }

                is JsonRpcResponse.JsonRpcError -> {
                    _engineEvent.emit(EngineDO.PushSubscribeError(wcResponse.response.id, response.error.message))
                }
            }
        } catch (exception: Exception) {
            _engineEvent.emit(SDKError(exception))
        }
    }

    private suspend fun onPushUpdateResponse(wcResponse: WCResponse, updateParams: PushParams.UpdateParams) = supervisorScope {
        try {
            when (val response = wcResponse.response) {
                is JsonRpcResponse.JsonRpcResult -> {
                    val subscription = subscriptionStorageRepository.getAllSubscriptions().firstOrNull { it.subscriptionTopic?.value == wcResponse.topic.value }
                        ?: throw NotFoundException("Cannot find subscription for topic: ${wcResponse.topic.value}")
                    val pushUpdateJwtClaim = extractVerifiedDidJwtClaims<PushSubscriptionJwtClaim>(updateParams.subscriptionAuth).getOrElse { error ->
                        _engineEvent.emit(SDKError(error))
                        return@supervisorScope
                    }
                    val listOfUpdateScopeNames = pushUpdateJwtClaim.scope.split(" ")
                    val updateScopeMap: Map<String, EngineDO.PushScope.Cached> = subscription.scope.entries.associate { (scopeName, scopeDescIsSelected) ->
                        val (desc, _) = scopeDescIsSelected
                        val isNewScopeTrue = listOfUpdateScopeNames.contains(scopeName)

                        scopeName to EngineDO.PushScope.Cached(scopeName, desc, isNewScopeTrue)
                    }
                    val newExpiry = calcExpiry()

                    subscriptionStorageRepository.updateSubscriptionScopeAndJwt(
                        wcResponse.topic.value,
                        updateScopeMap.mapValues { (_, pushScope) -> pushScope.description to pushScope.isSelected },
                        updateParams.subscriptionAuth,
                        newExpiry
                    )

                    _engineEvent.emit(
                        EngineDO.PushUpdate(
                            requestId = subscription.requestId,
                            responseTopic = subscription.responseTopic.value,
                            peerPublicKeyAsHex = subscription.peerPublicKey?.keyAsHex,
                            subscriptionTopic = subscription.subscriptionTopic?.value,
                            account = subscription.account,
                            relay = subscription.relay,
                            metadata = subscription.metadata,
                            didJwt = subscription.didJwt,
                            scope = updateScopeMap,
                            expiry = Expiry(newExpiry)
                        )
                    )
                }

                is JsonRpcResponse.JsonRpcError -> {
                    _engineEvent.emit(EngineDO.PushUpdateError(wcResponse.response.id, response.error.message))
                }
            }
        } catch (exception: Exception) {
            _engineEvent.emit(SDKError(exception))
        }
    }

    private suspend fun resubscribeToSubscriptions() {
        val subscriptionTopics = getListOfActiveSubscriptions().keys.toList()
        jsonRpcInteractor.batchSubscribe(subscriptionTopics) { error -> scope.launch { _engineEvent.emit(SDKError(error)) } }
    }

    private companion object {
        const val DID_JSON = ".well-known/did.json"
        const val WC_PUSH_CONFIG_JSON = ".well-known/wc-push-config.json"
    }
}