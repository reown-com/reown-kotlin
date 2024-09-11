package com.reown.appkit.client

import androidx.activity.ComponentActivity
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.wcKoinApp
import com.reown.android.pulse.model.EventType
import com.reown.android.pulse.model.properties.Props
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import com.reown.sign.common.exceptions.SignClientAlreadyInitializedException
import com.reown.util.Empty
import com.reown.appkit.client.models.Account
import com.reown.appkit.client.models.Session
import com.reown.appkit.client.models.AppKitClientAlreadyInitializedException
import com.reown.appkit.client.models.request.Request
import com.reown.appkit.client.models.request.SentRequestResult
import com.reown.appkit.di.appKitModule
import com.reown.appkit.domain.delegate.AppKitDelegate
import com.reown.appkit.domain.model.Session.WalletConnect
import com.reown.appkit.domain.model.toModalError
import com.reown.appkit.engine.AppKitEngine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.annotations.ApiStatus.Experimental
import org.koin.core.qualifier.named
import org.koin.dsl.module

object AppKit {

    internal var chains: List<Modal.Model.Chain> = listOf()

    internal var sessionProperties: Map<String, String>? = null

    internal var selectedChain: Modal.Model.Chain? = null

    internal var authPayloadParams: Modal.Model.AuthPayloadParams? = null

    private lateinit var appKitEngine: AppKitEngine

    interface ModalDelegate {
        fun onSessionApproved(approvedSession: Modal.Model.ApprovedSession)
        fun onSessionRejected(rejectedSession: Modal.Model.RejectedSession)
        fun onSessionUpdate(updatedSession: Modal.Model.UpdatedSession)

        @Deprecated(
            message = "Use onSessionEvent(Modal.Model.Event) instead. Using both will result in duplicate events.",
            replaceWith = ReplaceWith(expression = "onEvent(event)")
        )
        fun onSessionEvent(sessionEvent: Modal.Model.SessionEvent)
        fun onSessionEvent(sessionEvent: Modal.Model.Event) {}
        fun onSessionExtend(session: Modal.Model.Session)
        fun onSessionDelete(deletedSession: Modal.Model.DeletedSession)

        //Responses
        fun onSessionRequestResponse(response: Modal.Model.SessionRequestResponse)
        fun onSessionAuthenticateResponse(sessionAuthenticateResponse: Modal.Model.SessionAuthenticateResponse) {}
        fun onSIWEAuthenticationResponse(response: Modal.Model.SIWEAuthenticateResponse) {}

        // Utils
        fun onProposalExpired(proposal: Modal.Model.ExpiredProposal)
        fun onRequestExpired(request: Modal.Model.ExpiredRequest)
        fun onConnectionStateChange(state: Modal.Model.ConnectionState)
        fun onError(error: Modal.Model.Error)
    }

    interface ComponentDelegate {
        fun onModalExpanded()

        fun onModalHidden()

    }

    fun initialize(
        init: Modal.Params.Init,
        onSuccess: () -> Unit = {},
        onError: (Modal.Model.Error) -> Unit,
    ) {
        SignClient.initialize(
            init = Sign.Params.Init(init.core),
            onSuccess = {
                onInitializedClient(init, onSuccess, onError)
            },
            onError = { error ->
                if (error.throwable is SignClientAlreadyInitializedException) {
                    onInitializedClient(init, onSuccess, onError)
                } else {
                    return@initialize onError(Modal.Model.Error(error.throwable))
                }
            }
        )
    }

    @Experimental
    fun register(activity: ComponentActivity) {
        checkEngineInitialization()
        appKitEngine.registerCoinbaseLauncher(activity)
    }

    @Experimental
    fun unregister() {
        checkEngineInitialization()
        appKitEngine.unregisterCoinbase()
    }

    @Throws(IllegalStateException::class)
    private fun checkEngineInitialization() {
        check(::appKitEngine.isInitialized) {
            "AppKit needs to be initialized first using the initialize function"
        }
    }

    private fun onInitializedClient(
        init: Modal.Params.Init,
        onSuccess: () -> Unit = {},
        onError: (Modal.Model.Error) -> Unit,
    ) {
        if (!::appKitEngine.isInitialized) {
            runCatching {
                wcKoinApp.modules(appKitModule())
                appKitEngine = wcKoinApp.koin.get()
                appKitEngine.setup(init, onError)
                appKitEngine.setInternalDelegate(AppKitDelegate)
                wcKoinApp.modules(
                    module { single(named(AndroidCommonDITags.ENABLE_WEB_3_MODAL_ANALYTICS)) { init.enableAnalytics ?: appKitEngine.fetchAnalyticsConfig() } }
                )
            }
                .onFailure { error -> return@onInitializedClient onError(Modal.Model.Error(error)) }
                .onSuccess {
                    onSuccess()
                    appKitEngine.send(Props(event = EventType.TRACK, type = EventType.Track.MODAL_LOADED))
                }
        } else {
            onError(Modal.Model.Error(AppKitClientAlreadyInitializedException()))
        }
    }

    fun setChains(chains: List<Modal.Model.Chain>) {
        this.chains = chains
    }

    fun setAuthRequestParams(authParams: Modal.Model.AuthPayloadParams) {
        authPayloadParams = authParams
    }

    fun setSessionProperties(properties: Map<String, String>) {
        sessionProperties = properties
    }

    @Throws(IllegalStateException::class)
    fun setDelegate(delegate: ModalDelegate) {
        AppKitDelegate.connectionState.onEach { connectionState ->
            delegate.onConnectionStateChange(connectionState)
        }.launchIn(scope)

        AppKitDelegate.wcEventModels.onEach { event ->
            when (event) {
                is Modal.Model.ApprovedSession -> delegate.onSessionApproved(event)
                is Modal.Model.DeletedSession.Success -> delegate.onSessionDelete(event)
                is Modal.Model.Error -> delegate.onError(event)
                is Modal.Model.RejectedSession -> delegate.onSessionRejected(event)
                is Modal.Model.Session -> delegate.onSessionExtend(event)
                //todo: how to notify developer to not us both at the same time
                is Modal.Model.SessionEvent -> delegate.onSessionEvent(event)
                is Modal.Model.Event -> delegate.onSessionEvent(event)
                is Modal.Model.SessionRequestResponse -> delegate.onSessionRequestResponse(event)
                is Modal.Model.UpdatedSession -> delegate.onSessionUpdate(event)
                is Modal.Model.ExpiredRequest -> delegate.onRequestExpired(event)
                is Modal.Model.ExpiredProposal -> delegate.onProposalExpired(event)
                is Modal.Model.SessionAuthenticateResponse -> delegate.onSessionAuthenticateResponse(event)
                is Modal.Model.SIWEAuthenticateResponse -> delegate.onSIWEAuthenticationResponse(event)
                else -> Unit
            }
        }.launchIn(scope)
    }

    fun connect(
        connect: Modal.Params.Connect,
        onSuccess: (String) -> Unit,
        onError: (Modal.Model.Error) -> Unit
    ) {
        SignClient.connect(
            connect = connect.toSign(),
            onSuccess = { url -> onSuccess(url) },
            onError = { onError(it.toModal()) }
        )
    }

    fun authenticate(
        authenticate: Modal.Params.Authenticate,
        walletAppLink: String? = null,
        onSuccess: (String) -> Unit,
        onError: (Modal.Model.Error) -> Unit,
    ) {

        SignClient.authenticate(authenticate.toSign(), walletAppLink,
            onSuccess = { url -> onSuccess(url) },
            onError = { onError(it.toModal()) })
    }

    fun handleDeepLink(url: String, onError: (Modal.Model.Error) -> Unit) {
        SignClient.dispatchEnvelope(url) {
            onError(it.toModal())
        }
    }

    fun request(
        request: Request,
        onSuccess: (SentRequestResult) -> Unit = {},
        onError: (Throwable) -> Unit,
    ) {
        checkEngineInitialization()
        appKitEngine.request(request, onSuccess, onError)
    }

    private fun SentRequestResult.sentRequestToModal() = when (this) {
        is SentRequestResult.Coinbase -> Modal.Model.SentRequest(Long.MIN_VALUE, String.Empty, method, params, chainId)
        is SentRequestResult.WalletConnect -> Modal.Model.SentRequest(requestId, sessionTopic, method, params, chainId)
    }

    fun request(
        request: Request,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        checkEngineInitialization()
        appKitEngine.request(request, { onSuccess() }, onError)
    }

    fun ping(sessionPing: Modal.Listeners.SessionPing? = null) = appKitEngine.ping(sessionPing)

    fun disconnect(
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        checkEngineInitialization()
        appKitEngine.disconnect(onSuccess, onError)
    }

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    fun getSelectedChain() = selectedChain

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    @Deprecated(
        message = "Getting active session is replaced with getAccount()",
        replaceWith = ReplaceWith("com.reown.appkit.client.AppKit.getAccount()"),
        level = DeprecationLevel.WARNING
    )
    internal fun getActiveSessionByTopic(topic: String) = SignClient.getActiveSessionByTopic(topic)?.toModal()

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    @Deprecated(
        message = "Getting active session is replaced with getAccount()",
        replaceWith = ReplaceWith("com.reown.appkit.client.AppKit.getAccount()"),
        level = DeprecationLevel.WARNING
    )
    fun getActiveSession(): Modal.Model.Session? {
        checkEngineInitialization()
        return (appKitEngine.getActiveSession() as? WalletConnect)?.topic?.let { SignClient.getActiveSessionByTopic(it)?.toModal() }
    }

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    fun getAccount(): Account? {
        checkEngineInitialization()
        return appKitEngine.getAccount()
    }

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    fun getSession(): Session? {
        checkEngineInitialization()
        return appKitEngine.getSession()
    }

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    fun getConnectorType(): Modal.ConnectorType? {
        checkEngineInitialization()
        return appKitEngine.getConnectorType()
    }
}
