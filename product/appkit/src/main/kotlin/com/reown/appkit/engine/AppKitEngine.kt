package com.reown.appkit.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.reown.android.cacao.signature.SignatureType
import com.reown.android.internal.common.modal.domain.usecase.EnableAnalyticsUseCaseInterface
import com.reown.android.internal.common.model.ProjectId
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.signing.message.MessageSignatureVerifier
import com.reown.android.internal.common.wcKoinApp
import com.reown.android.pulse.domain.SendEventInterface
import com.reown.android.pulse.model.EventType
import com.reown.android.pulse.model.properties.Properties
import com.reown.android.pulse.model.properties.Props
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import com.reown.appkit.client.models.Account
import com.reown.appkit.client.models.CoinbaseClientAlreadyInitializedException
import com.reown.appkit.client.models.request.Request
import com.reown.appkit.client.models.request.SentRequestResult
import com.reown.appkit.client.models.request.toSentRequest
import com.reown.appkit.client.toCoinbaseSession
import com.reown.appkit.client.toModal
import com.reown.appkit.client.toSession
import com.reown.appkit.client.toSign
import com.reown.appkit.domain.delegate.AppKitDelegate
import com.reown.appkit.domain.model.InvalidSessionException
import com.reown.appkit.domain.model.Session
import com.reown.appkit.domain.usecase.ConnectionEventRepository
import com.reown.appkit.domain.usecase.DeleteSessionDataUseCase
import com.reown.appkit.domain.usecase.GetSelectedChainUseCase
import com.reown.appkit.domain.usecase.GetSessionUseCase
import com.reown.appkit.domain.usecase.SaveSessionUseCase
import com.reown.appkit.engine.coinbase.COINBASE_WALLET_ID
import com.reown.appkit.engine.coinbase.CoinbaseClient
import com.reown.appkit.utils.getSelectedChain
import com.reown.appkit.utils.toAccount
import com.reown.appkit.utils.toChain
import com.reown.appkit.utils.toConnectorType
import com.reown.appkit.utils.toSession
import com.reown.foundation.util.Logger
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import com.reown.util.Empty
import kotlinx.coroutines.launch

internal class AppKitEngine(
    private val getSessionUseCase: GetSessionUseCase,
    private val getSelectedChainUseCase: GetSelectedChainUseCase,
    private val saveSessionUseCase: SaveSessionUseCase,
    private val deleteSessionDataUseCase: DeleteSessionDataUseCase,
    private val sendEventUseCase: SendEventInterface,
    private val connectionEventRepository: ConnectionEventRepository,
    private val enableAnalyticsUseCase: EnableAnalyticsUseCaseInterface,
    private val logger: Logger
) : SendEventInterface by sendEventUseCase,
    EnableAnalyticsUseCaseInterface by enableAnalyticsUseCase {
    internal var excludedWalletsIds: MutableList<String> = mutableListOf()
    internal var includeWalletsIds: MutableList<String> = mutableListOf()
    internal var recommendedWalletsIds: MutableList<String> = mutableListOf()
    internal var siweRequestIdWithMessage: Pair<Long, String>? = null
    private lateinit var coinbaseClient: CoinbaseClient
    internal var shouldDisconnect: Boolean = true
    private val projectId by lazy { wcKoinApp.koin.get<ProjectId>() }
    private val signatureVerifier by lazy { MessageSignatureVerifier(projectId) }

    fun setup(
        init: Modal.Params.Init,
        onError: (Modal.Model.Error) -> Unit
    ) {
        excludedWalletsIds.addAll(init.excludedWalletIds)
        includeWalletsIds.addAll(init.includeWalletIds)
        recommendedWalletsIds.addAll(init.recommendedWalletsIds)
        setupCoinbase(init, onError)
    }

    private fun setupCoinbase(init: Modal.Params.Init, onError: (Modal.Model.Error) -> Unit) {
        if (init.coinbaseEnabled) {
            if (!::coinbaseClient.isInitialized) {
                coinbaseClient = CoinbaseClient(context = wcKoinApp.koin.get(), appMetaData = wcKoinApp.koin.get())
            } else {
                onError(Modal.Model.Error(CoinbaseClientAlreadyInitializedException()))
            }
        } else {
            excludedWalletsIds.add(COINBASE_WALLET_ID)
        }
    }

    fun connectWC(
        name: String, method: String,
        connect: Modal.Params.ConnectParams,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        connectionEventRepository.saveEvent(name, method)
        SignClient.connect(connect.toSign(), onSuccess) { onError(it.throwable) }
    }

    @Deprecated("This method is deprecated. Use connect with authenticate instead.")
    fun authenticate(
        name: String, method: String,
        authenticate: Modal.Params.Authenticate,
        walletAppLink: String? = null,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        connectionEventRepository.saveEvent(name, method)
        SignClient.authenticate(authenticate.toSign(), walletAppLink, onSuccess) { onError(it.throwable) }
    }

    fun connectCoinbase(
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        checkEngineInitialization()
        coinbaseClient.connect(
            onSuccess = {
                scope.launch {
                    val chain = AppKit.chains.getSelectedChain(AppKit.selectedChain?.id)
                    saveSessionUseCase(it.toSession(chain))
                    AppKitDelegate.emit(it)
                    onSuccess()
                }

            }, onError = {
                onError(it)
            }
        )
    }

    fun getSelectedChain() = getSelectedChainUseCase()?.toChain()

    fun getActiveSession() = getSessionUseCase()?.isSessionActive()

    private fun Session.isSessionActive() = when (this) {
        is Session.Coinbase -> if (coinbaseClient.isConnected()) this else null
        is Session.WalletConnect -> SignClient.getActiveSessionByTopic(topic)?.let { this }
    }

    fun getConnectorType() = getSessionUseCase()?.toConnectorType()

    internal fun getSelectedChainOrFirst() = getSelectedChain() ?: AppKit.chains.first()

    fun formatSIWEMessage(authParams: Modal.Model.AuthPayloadParams, issuer: String): String {
        return SignClient.formatAuthMessage(authParams.toSign(issuer))
    }

    fun request(request: Request, onSuccess: (SentRequestResult) -> Unit, onError: (Throwable) -> Unit) {
        val session = getActiveSession()
        val selectedChain = getSelectedChain()
        val chainId = request.chainId ?: selectedChain?.id

        if (session == null || chainId == null) {
            onError(InvalidSessionException)
            return
        }

        when (session) {
            is Session.Coinbase -> {
                checkEngineInitialization()
                coinbaseClient.request(
                    request,
                    { onSuccess(SentRequestResult.Coinbase(request.method, request.params, chainId, it)) },
                    onError
                )
            }

            is Session.WalletConnect ->
                SignClient.request(
                    request.toSign(session.topic, chainId),
                    {
                        onSuccess(it.toSentRequest())
                        openWalletApp(session.topic, onError)
                    },
                    { onError(it.throwable) }
                )
        }
    }

    fun extend(onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit) {
        val session = getSessionUseCase()?.isSessionActive()

        if (session == null) {
            onError(InvalidSessionException)
            return
        }

        when (session) {
            is Session.WalletConnect ->
                SignClient.extend(
                    extend = Sign.Params.Extend(session.topic),
                    onSuccess = { onSuccess() },
                    onError = { onError(it.throwable) }
                )

            else -> onError(UnsupportedOperationException("Extend is only supported for WalletConnect sessions"))
        }
    }


    private fun openWalletApp(topic: String, onError: (RedirectMissingThrowable) -> Unit) {
        val redirect = SignClient.getActiveSessionByTopic(topic)?.redirect ?: String.Empty
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(redirect))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            wcKoinApp.koin.get<Context>().startActivity(intent)
        } catch (e: Throwable) {
            onError(RedirectMissingThrowable("Please redirect to a wallet manually"))
        }
    }

    fun ping(sessionPing: Modal.Listeners.SessionPing?) {
        when (val session = getSessionUseCase()) {
            is Session.WalletConnect -> SignClient.ping(Sign.Params.Ping(session.topic), sessionPing?.toSign())
            else -> sessionPing?.onError(Modal.Model.Ping.Error(InvalidSessionException))
        }
    }

    fun disconnect(onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit) {
        val session = getSessionUseCase()

        if (session == null) {
            onError(InvalidSessionException)
            return
        }

        when (session) {
            is Session.Coinbase -> {
                checkEngineInitialization()
                coinbaseClient.disconnect()
                scope.launch { deleteSessionDataUseCase() }
                onSuccess()
            }

            is Session.WalletConnect -> {
                SignClient.disconnect(
                    Sign.Params.Disconnect(session.topic),
                    onSuccess = {
                        sendEventUseCase.send(Props(EventType.TRACK, EventType.Track.DISCONNECT_SUCCESS))
                        scope.launch {
                            deleteSessionDataUseCase()
                            shouldDisconnect = true
                            onSuccess()
                        }
                    },
                    onError = {
                        sendEventUseCase.send(Props(EventType.TRACK, EventType.Track.DISCONNECT_ERROR))
                        onError(it.throwable)
                    })
            }
        }
    }

    fun clearSession() {
        scope.launch { deleteSessionDataUseCase() }
    }

    fun getAccount(): Account? = getActiveSession()?.let { session ->
        when (session) {
            is Session.Coinbase -> coinbaseClient.getAccount(session)
            is Session.WalletConnect -> SignClient.getActiveSessionByTopic(session.topic)?.toAccount(session)
        }
    }

    fun getSession() = getSessionUseCase()?.let { session ->
        when (session) {
            is Session.Coinbase -> coinbaseClient.getAccount(session)?.toCoinbaseSession()
            is Session.WalletConnect -> SignClient.getActiveSessionByTopic(session.topic)?.toSession()
        }
    }

    @Throws(IllegalStateException::class)
    fun setInternalDelegate(delegate: AppKitDelegate) {
        val signDelegate = object : SignClient.DappDelegate {
            override fun onSessionApproved(approvedSession: Sign.Model.ApprovedSession) {
                try {
                    val (name, method) = connectionEventRepository.getEvent()
                    sendEventUseCase.send(Props(EventType.TRACK, EventType.Track.CONNECT_SUCCESS, Properties(name = name, method = method)))
                    connectionEventRepository.deleteEvent()
                } catch (e: Exception) {
                    logger.error(e)
                }

                delegate.onSessionApproved(approvedSession.toModal())
            }

            override fun onSessionRejected(rejectedSession: Sign.Model.RejectedSession) {
                try {
                    connectionEventRepository.deleteEvent()
                } catch (e: Exception) {
                    logger.error(e)
                }
                sendEventUseCase.send(Props(EventType.TRACK, EventType.Track.CONNECT_ERROR, Properties(message = rejectedSession.reason)))
                delegate.onSessionRejected(rejectedSession.toModal())
            }

            override fun onSessionUpdate(updatedSession: Sign.Model.UpdatedSession) {
                delegate.onSessionUpdate(updatedSession.toModal())
            }

            override fun onSessionEvent(sessionEvent: Sign.Model.SessionEvent) {
                delegate.onSessionEvent(sessionEvent.toModal())
            }

            override fun onSessionEvent(sessionEvent: Sign.Model.Event) {
                delegate.onSessionEvent(sessionEvent.toModal())
            }

            override fun onSessionExtend(session: Sign.Model.Session) {
                delegate.onSessionExtend(session.toModal())
            }

            override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
                clearSession()
                delegate.onSessionDelete(deletedSession.toModal())
            }

            override fun onSessionRequestResponse(response: Sign.Model.SessionRequestResponse) {
                try {
                    if (response.result.id == siweRequestIdWithMessage?.first) {
                        if (response.result is Sign.Model.JsonRpcResponse.JsonRpcResult) {
                            val siweResponse = Modal.Model.SIWEAuthenticateResponse.Result(
                                id = response.result.id,
                                message = siweRequestIdWithMessage!!.second,
                                signature = (response.result as Sign.Model.JsonRpcResponse.JsonRpcResult).result as? String ?: ""
                            )
                            siweRequestIdWithMessage = null
                            val account = getAccount() ?: throw IllegalStateException("Account is null")
                            val isValid = signatureVerifier.verify(
                                signature = siweResponse.signature,
                                originalMessage = siweResponse.message,
                                address = account.address,
                                type = SignatureType.EIP191
                            )
                            if (isValid) {
                                delegate.onSIWEAuthenticationResponse(siweResponse)
                            } else {
                                emitSIWEErrorResponse(response, delegate)
                            }
                        } else if (response.result is Sign.Model.JsonRpcResponse.JsonRpcError) {
                            emitSIWEErrorResponse(response, delegate)
                        }
                    } else {
                        delegate.onSessionRequestResponse(response.toModal())
                    }
                } catch (e: Exception) {
                    logger.error(e)
                    emitSIWEErrorResponse(response, delegate)
                }
            }

            override fun onSessionAuthenticateResponse(sessionAuthenticateResponse: Sign.Model.SessionAuthenticateResponse) {
                delegate.onSessionAuthenticateResponse(sessionAuthenticateResponse.toModal())
            }

            override fun onProposalExpired(proposal: Sign.Model.ExpiredProposal) {
                delegate.onProposalExpired(proposal.toModal())
            }

            override fun onRequestExpired(request: Sign.Model.ExpiredRequest) {
                delegate.onRequestExpired(request.toModal())
            }

            override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {
                delegate.onConnectionStateChange(state.toModal())
            }

            override fun onError(error: Sign.Model.Error) {
                delegate.onError(error.toModal())
            }
        }
        SignClient.setDappDelegate(signDelegate)
    }

    private fun emitSIWEErrorResponse(response: Sign.Model.SessionRequestResponse, delegate: AppKitDelegate) {
        val siweResponse = Modal.Model.SIWEAuthenticateResponse.Error(
            id = response.result.id,
            message = (response.result as? Sign.Model.JsonRpcResponse.JsonRpcError)?.message ?: "Invalid signature, try again",
            code = (response.result as? Sign.Model.JsonRpcResponse.JsonRpcError)?.code ?: 1000
        )
        siweRequestIdWithMessage = null
        delegate.onSIWEAuthenticationResponse(siweResponse)
    }

    fun registerCoinbaseLauncher(activity: ComponentActivity) {
        if (::coinbaseClient.isInitialized) {
            coinbaseClient.setLauncher(activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                result.data?.data?.let {
                    coinbaseClient.handleResponse(
                        it
                    )
                }
            })
        }
    }

    fun unregisterCoinbase() {
        if (::coinbaseClient.isInitialized) {
            coinbaseClient.unregister()
        }
    }

    fun coinbaseIsEnabled() = ::coinbaseClient.isInitialized && coinbaseClient.isInstalled() && coinbaseClient.isLauncherSet()

    @Throws(IllegalStateException::class)
    private fun checkEngineInitialization() {
        check(::coinbaseClient.isInitialized) {
            "Coinbase Client needs to be initialized first using the initialize function"
        }
    }

    internal class RedirectMissingThrowable(message: String) : Throwable(message)
}
