@file:JvmSynthetic

package com.reown.sign.client

import com.reown.android.Core
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.di.DatabaseConfig
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.SDKError
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.wcKoinApp
import com.reown.android.pairing.model.mapper.toPairing
import com.reown.android.relay.WSSConnectionState
import com.reown.foundation.common.model.Topic
import com.reown.sign.client.mapper.*
import com.reown.sign.common.exceptions.SignClientAlreadyInitializedException
import com.reown.sign.di.engineModule
import com.reown.sign.di.signJsonRpcModule
import com.reown.sign.di.storageModule
import com.reown.sign.engine.domain.SignEngine
import com.reown.sign.engine.model.EngineDO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.KoinApplication
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.atomic.AtomicBoolean

class SignProtocol(private val koinApp: KoinApplication = wcKoinApp) : SignInterface {
    private lateinit var signEngine: SignEngine
    private var atomicBoolean: AtomicBoolean? = null

    companion object {
        val instance = SignProtocol()
    }

    override fun initialize(init: Sign.Params.Init, onSuccess: () -> Unit, onError: (Sign.Model.Error) -> Unit) {
        // TODO: re-init scope
        if (!::signEngine.isInitialized) {
            try {
                koinApp.modules(
                    signJsonRpcModule(),
                    storageModule(koinApp.koin.get<DatabaseConfig>().SIGN_SDK_DB_NAME),
                    engineModule()
                )

                signEngine = koinApp.koin.get()
                signEngine.setup()
                onSuccess()
            } catch (e: Exception) {
                onError(Sign.Model.Error(e))
            }
        } else {
            onError(Sign.Model.Error(SignClientAlreadyInitializedException()))
        }
    }

    @Throws(IllegalStateException::class)
    override fun setWalletDelegate(delegate: SignInterface.WalletDelegate) {
        checkEngineInitialization()

        wcKoinApp.modules(module { single(named(AndroidCommonDITags.ENABLE_AUTHENTICATE)) { delegate.onSessionAuthenticate != null } })
        handleConnectionState { connectionState -> delegate.onConnectionStateChange(connectionState) }
        signEngine.engineEvent.onEach { event ->
            when (event) {
                is EngineDO.SessionProposalEvent -> delegate.onSessionProposal(event.proposal.toClientSessionProposal(), event.context.toCore())
                is EngineDO.SessionAuthenticateEvent -> delegate.onSessionAuthenticate?.invoke(
                    event.toClientSessionAuthenticate(),
                    event.verifyContext.toCore()
                )

                is EngineDO.SessionRequestEvent -> delegate.onSessionRequest(event.request.toClientSessionRequest(), event.context.toCore())
                is EngineDO.SessionDelete -> delegate.onSessionDelete(event.toClientDeletedSession())
                is EngineDO.SessionExtend -> delegate.onSessionExtend(event.toClientActiveSession())
                //Responses
                is EngineDO.SettledSessionResponse -> delegate.onSessionSettleResponse(event.toClientSettledSessionResponse())
                is EngineDO.SessionUpdateNamespacesResponse -> delegate.onSessionUpdateResponse(event.toClientUpdateSessionNamespacesResponse())
                //Utils
                is EngineDO.ExpiredProposal -> delegate.onProposalExpired(event.toClient())
                is EngineDO.ExpiredRequest -> delegate.onRequestExpired(event.toClient())
                is SDKError -> delegate.onError(event.toClientError())
            }
        }.launchIn(scope)
    }

    @Throws(IllegalStateException::class)
    override fun setDappDelegate(delegate: SignInterface.DappDelegate) {
        checkEngineInitialization()

        handleConnectionState { connectionState -> delegate.onConnectionStateChange(connectionState) }
        signEngine.engineEvent.onEach { event ->
            when (event) {
                is EngineDO.SessionRejected -> delegate.onSessionRejected(event.toClientSessionRejected())
                is EngineDO.SessionApproved -> delegate.onSessionApproved(event.toClientSessionApproved())
                is EngineDO.SessionUpdateNamespaces -> delegate.onSessionUpdate(event.toClientSessionsNamespaces())
                is EngineDO.SessionDelete -> delegate.onSessionDelete(event.toClientDeletedSession())
                is EngineDO.SessionEvent -> {
                    delegate.onSessionEvent(event.toClientSessionEvent())
                    delegate.onSessionEvent(event.toClientEvent())
                }

                is EngineDO.SessionExtend -> delegate.onSessionExtend(event.toClientActiveSession())
                //Responses
                is EngineDO.SessionPayloadResponse -> delegate.onSessionRequestResponse(event.toClientSessionPayloadResponse())
                is EngineDO.SessionAuthenticateResponse -> delegate.onSessionAuthenticateResponse(event.toClientSessionAuthenticateResponse())
                //Utils
                is EngineDO.ExpiredProposal -> delegate.onProposalExpired(event.toClient())
                is EngineDO.ExpiredRequest -> delegate.onRequestExpired(event.toClient())
                is SDKError -> delegate.onError(event.toClientError())
            }
        }.launchIn(scope)
    }

    @Deprecated(
        "This method is deprecated. The requiredNamespaces parameter is no longer supported as all namespaces are now treated as optional to improve connection compatibility. Use connect(connectParams: Sign.Params.ConnectParams, onSuccess: (String) -> Unit, onError: (Sign.Model.Error) -> Unit) instead.",
        replaceWith = ReplaceWith("connect(connect, onSuccess, onError)")
    )
    @Throws(IllegalStateException::class)
    override fun connect(
        connect: Sign.Params.Connect,
        onSuccess: (String) -> Unit,
        onError: (Sign.Model.Error) -> Unit,
    ) {
        checkEngineInitialization()
        scope.launch {
            try {
                with(connect) {
                    signEngine.proposeSession(
                        namespaces?.toMapOfEngineNamespacesRequired(),
                        optionalNamespaces?.toMapOfEngineNamespacesOptional(),
                        properties,
                        scopedProperties,
                        pairing.toPairing(),
                        onSuccess = { onSuccess(pairing.uri) },
                        onFailure = { error -> onError(Sign.Model.Error(error)) }
                    )
                }
            } catch (error: Exception) {
                onError(Sign.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun connect(
        connectParams: Sign.Params.ConnectParams,
        onSuccess: (String) -> Unit,
        onError: (Sign.Model.Error) -> Unit,
    ) {
        checkEngineInitialization()
        scope.launch {
            try {
                with(connectParams) {
                    signEngine.proposeSession(
                        null,
                        sessionNamespaces?.toMapOfEngineNamespacesOptional(),
                        properties,
                        scopedProperties,
                        pairing.toPairing(),
                        connectParams.authentication?.map { it.toAuthenticate() },
                        onSuccess = { onSuccess(pairing.uri) },
                        onFailure = { error -> onError(Sign.Model.Error(error)) }
                    )
                }
            } catch (error: Exception) {
                onError(Sign.Model.Error(error))
            }
        }
    }

    @Deprecated("This method is deprecated. Use connect with authenticate instead.")
    @Throws(IllegalStateException::class)
    override fun authenticate(
        authenticate: Sign.Params.Authenticate,
        walletAppLink: String?,
        onSuccess: (String) -> Unit,
        onError: (Sign.Model.Error) -> Unit,
    ) {
        checkEngineInitialization()
        scope.launch {
            try {
                signEngine.authenticate(
                    authenticate.toAuthenticate(),
                    authenticate.methods, authenticate.pairingTopic,
                    if (authenticate.expiry == null) null else Expiry(authenticate.expiry),
                    walletAppLink,
                    onSuccess = { url -> onSuccess(url) },
                    onFailure = { throwable -> onError(Sign.Model.Error(throwable)) })
            } catch (error: Exception) {
                onError(Sign.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun dispatchEnvelope(urlWithEnvelope: String, onError: (Sign.Model.Error) -> Unit) {
        checkEngineInitialization()
        scope.launch {
            try {
                signEngine.dispatchEnvelope(urlWithEnvelope)
            } catch (error: Exception) {
                onError(Sign.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun formatAuthMessage(formatMessage: Sign.Params.FormatMessage): String {
        checkEngineInitialization()
        return runBlocking { signEngine.formatMessage(formatMessage.payloadParams.toEngine(), formatMessage.iss) }
    }

    @Throws(IllegalStateException::class)
    override fun approveSession(approve: Sign.Params.Approve, onSuccess: (Sign.Params.Approve) -> Unit, onError: (Sign.Model.Error) -> Unit) {
        checkEngineInitialization()

        scope.launch {
            try {
                signEngine.approve(
                    proposerPublicKey = approve.proposerPublicKey,
                    sessionNamespaces = approve.namespaces.toMapOfEngineNamespacesSession(),
                    sessionProperties = approve.properties,
                    scopedProperties = approve.scopedProperties,
                    proposalRequestsResponses = EngineDO.ProposalRequestsResponses(
                        authentication = approve.proposalRequestsResponses?.authentication?.toCommon() ?: emptyList()
                    ),
                    onSuccess = { onSuccess(approve) },
                    onFailure = { error -> onError(Sign.Model.Error(error)) }
                )

            } catch (error: Exception) {
                onError(Sign.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun rejectSession(reject: Sign.Params.Reject, onSuccess: (Sign.Params.Reject) -> Unit, onError: (Sign.Model.Error) -> Unit) {
        checkEngineInitialization()

        scope.launch {
            try {
                signEngine.reject(reject.proposerPublicKey, reject.reason, onSuccess = { onSuccess(reject) }) { error ->
                    onError(Sign.Model.Error(error))
                }
            } catch (error: Exception) {
                onError(Sign.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun approveAuthenticate(
        approve: Sign.Params.ApproveAuthenticate,
        onSuccess: (Sign.Params.ApproveAuthenticate) -> Unit,
        onError: (Sign.Model.Error) -> Unit
    ) {
        checkEngineInitialization()

        scope.launch {
            try {
                signEngine.approveSessionAuthenticate(
                    approve.id, approve.cacaos.toCommon(),
                    onSuccess = { onSuccess(approve) },
                    onFailure = { error -> onError(Sign.Model.Error(error)) }
                )

            } catch (error: Exception) {
                onError(Sign.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun rejectAuthenticate(
        reject: Sign.Params.RejectAuthenticate,
        onSuccess: (Sign.Params.RejectAuthenticate) -> Unit,
        onError: (Sign.Model.Error) -> Unit
    ) {
        checkEngineInitialization()

        scope.launch {
            try {
                signEngine.rejectSessionAuthenticate(reject.id, reject.reason, onSuccess = { onSuccess(reject) }) { error ->
                    onError(
                        Sign.Model.Error(
                            error
                        )
                    )
                }
            } catch (error: Exception) {
                onError(Sign.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun request(request: Sign.Params.Request, onSuccess: (Sign.Model.SentRequest) -> Unit, onError: (Sign.Model.Error) -> Unit) {
        checkEngineInitialization()

        scope.launch {
            try {
                signEngine.sessionRequest(
                    request = request.toEngineDORequest(),
                    onSuccess = { requestId -> onSuccess(request.toSentRequest(requestId)) },
                    onFailure = { error -> onError(Sign.Model.Error(error)) }
                )
            } catch (error: Exception) {
                onError(Sign.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun respond(response: Sign.Params.Response, onSuccess: (Sign.Params.Response) -> Unit, onError: (Sign.Model.Error) -> Unit) {
        checkEngineInitialization()

        scope.launch {
            try {
                signEngine.respondSessionRequest(
                    topic = response.sessionTopic,
                    jsonRpcResponse = response.jsonRpcResponse.toJsonRpcResponse(),
                    onSuccess = { onSuccess(response) },
                    onFailure = { error -> onError(Sign.Model.Error(error)) }
                )
            } catch (error: Exception) {
                onError(Sign.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun update(update: Sign.Params.Update, onSuccess: (Sign.Params.Update) -> Unit, onError: (Sign.Model.Error) -> Unit) {
        checkEngineInitialization()

        scope.launch {
            try {
                signEngine.sessionUpdate(
                    topic = update.sessionTopic,
                    namespaces = update.namespaces.toMapOfEngineNamespacesSession(),
                    onSuccess = { onSuccess(update) },
                    onFailure = { error -> onError(Sign.Model.Error(error)) }
                )
            } catch (error: Exception) {
                onError(Sign.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun extend(extend: Sign.Params.Extend, onSuccess: (Sign.Params.Extend) -> Unit, onError: (Sign.Model.Error) -> Unit) {
        checkEngineInitialization()

        scope.launch {
            try {
                signEngine.extend(
                    topic = extend.topic,
                    onSuccess = { onSuccess(extend) },
                    onFailure = { error -> onError(Sign.Model.Error(error)) }
                )
            } catch (error: Exception) {
                onError(Sign.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun emit(emit: Sign.Params.Emit, onSuccess: (Sign.Params.Emit) -> Unit, onError: (Sign.Model.Error) -> Unit) {
        checkEngineInitialization()

        scope.launch {
            try {
                signEngine.emit(
                    topic = emit.topic,
                    event = emit.event.toEngineEvent(emit.chainId),
                    onSuccess = { onSuccess(emit) },
                    onFailure = { error -> onError(Sign.Model.Error(error)) }
                )
            } catch (error: Exception) {
                onError(Sign.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun ping(ping: Sign.Params.Ping, sessionPing: Sign.Listeners.SessionPing?) {
        checkEngineInitialization()

        scope.launch {
            try {
                signEngine.ping(
                    ping.topic,
                    { topic -> sessionPing?.onSuccess(Sign.Model.Ping.Success(topic)) },
                    { error -> sessionPing?.onError(Sign.Model.Ping.Error(error)) },
                    ping.timeout
                )
            } catch (error: Exception) {
                sessionPing?.onError(Sign.Model.Ping.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun disconnect(disconnect: Sign.Params.Disconnect, onSuccess: (Sign.Params.Disconnect) -> Unit, onError: (Sign.Model.Error) -> Unit) {
        checkEngineInitialization()

        scope.launch {
            try {
                signEngine.disconnect(
                    topic = disconnect.sessionTopic,
                    onSuccess = { onSuccess(disconnect) },
                    onFailure = { error -> onError(Sign.Model.Error(error)) }
                )
            } catch (error: Exception) {
                onError(Sign.Model.Error(error))
            }
        }
    }

    override fun decryptMessage(params: Sign.Params.DecryptMessage, onSuccess: (Sign.Model.Message) -> Unit, onError: (Sign.Model.Error) -> Unit) {
        checkEngineInitialization()

        scope.launch {
            try {
                signEngine.decryptNotification(
                    topic = params.topic,
                    message = params.encryptedMessage,
                    onSuccess = { message ->
                        when (message) {
                            is Core.Model.Message.SessionRequest -> onSuccess(message.toSign())
                            is Core.Model.Message.SessionProposal -> onSuccess(message.toSign())
                            is Core.Model.Message.SessionAuthenticate -> onSuccess(message.toSign())
                            else -> {
                                //Ignore
                            }
                        }
                    },
                    onFailure = { error -> onError(Sign.Model.Error(error)) }
                )
            } catch (error: Exception) {
                onError(Sign.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun getListOfActiveSessions(): List<Sign.Model.Session> {
        checkEngineInitialization()
        return runBlocking {
            signEngine.getListOfSettledSessions().map(EngineDO.Session::toClientActiveSession)
        }
    }

    @Throws(IllegalStateException::class)
    override fun getActiveSessionByTopic(topic: String): Sign.Model.Session? {
        checkEngineInitialization()
        return runBlocking {
            signEngine.getListOfSettledSessions().map(EngineDO.Session::toClientActiveSession)
                .find { session -> session.topic == topic }
        }
    }

    @Throws(IllegalStateException::class)
    override fun getPendingSessionRequests(topic: String): List<Sign.Model.SessionRequest> {
        checkEngineInitialization()
        return runBlocking { signEngine.getPendingSessionRequests(Topic(topic)).mapToPendingSessionRequests() }
    }

    @Throws(IllegalStateException::class)
    override fun getSessionProposals(): List<Sign.Model.SessionProposal> {
        checkEngineInitialization()
        return runBlocking { signEngine.getSessionProposals().map(EngineDO.SessionProposal::toClientSessionProposal) }
    }

    @Throws(IllegalStateException::class)
    override fun getPendingAuthenticateRequests(): List<Sign.Model.SessionAuthenticate> {
        checkEngineInitialization()
        return runBlocking { signEngine.getPendingAuthenticateRequests().map { request -> request.toClient() } }
    }

    @Throws(IllegalStateException::class)
    override fun getVerifyContext(id: Long): Sign.Model.VerifyContext? {
        checkEngineInitialization()
        return runBlocking { signEngine.getVerifyContext(id)?.toCore() }
    }

    @Throws(IllegalStateException::class)
    override fun getListOfVerifyContexts(): List<Sign.Model.VerifyContext> {
        checkEngineInitialization()
        return runBlocking { signEngine.getListOfVerifyContexts().map { verifyContext -> verifyContext.toCore() } }
    }

// TODO: Uncomment once reinit scope logic is added
//    fun shutdown() {
//        scope.cancel()
//        wcKoinApp.close()
//    }

    private fun handleConnectionState(onDelegate: (state: Sign.Model.ConnectionState) -> Unit) {
        signEngine.wssConnection.onEach { connectionState ->
            when {
                atomicBoolean == null -> {
                    atomicBoolean = AtomicBoolean()
                    when (connectionState) {
                        is WSSConnectionState.Disconnected.ConnectionFailed ->
                            onDelegate(
                                Sign.Model.ConnectionState(
                                    false,
                                    Sign.Model.ConnectionState.Reason.ConnectionFailed(connectionState.throwable)
                                )
                            )

                        is WSSConnectionState.Disconnected.ConnectionClosed ->
                            onDelegate(
                                Sign.Model.ConnectionState(
                                    false,
                                    Sign.Model.ConnectionState.Reason.ConnectionClosed(connectionState.message ?: "Connection closed")
                                )
                            )

                        else -> onDelegate(Sign.Model.ConnectionState(true))
                    }
                }

                atomicBoolean?.get() == true && connectionState is WSSConnectionState.Disconnected.ConnectionFailed -> {
                    atomicBoolean?.set(false)
                    onDelegate(Sign.Model.ConnectionState(false, Sign.Model.ConnectionState.Reason.ConnectionFailed(connectionState.throwable)))
                }

                atomicBoolean?.get() == true && connectionState is WSSConnectionState.Disconnected.ConnectionClosed -> {
                    atomicBoolean?.set(false)
                    onDelegate(
                        Sign.Model.ConnectionState(
                            false,
                            Sign.Model.ConnectionState.Reason.ConnectionClosed(connectionState.message ?: "Connection closed")
                        )
                    )
                }

                atomicBoolean?.get() == false && connectionState is WSSConnectionState.Connected -> {
                    atomicBoolean?.set(true)
                    onDelegate(Sign.Model.ConnectionState(true))
                }

                else -> Unit
            }
        }.launchIn(scope)
    }

    @Throws(IllegalStateException::class)
    private fun checkEngineInitialization() {
        check(::signEngine.isInitialized) {
            "SignClient needs to be initialized first using the initialize function"
        }
    }
}