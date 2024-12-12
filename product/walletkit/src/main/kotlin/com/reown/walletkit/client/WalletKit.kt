package com.reown.walletkit.client

import com.reown.android.Core
import com.reown.android.CoreInterface
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.wcKoinApp
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import com.reown.sign.common.exceptions.SignClientAlreadyInitializedException
import com.reown.walletkit.di.walletKitModule
import com.reown.walletkit.smart_account.Account
import com.reown.walletkit.smart_account.SafeInteractor
import com.reown.walletkit.use_cases.CanFulfilUseCase
import com.reown.walletkit.use_cases.EstimateGasUseCase
import com.reown.walletkit.use_cases.FulfilmentStatusUseCase
import com.reown.walletkit.use_cases.GetERC20TokenBalanceUseCase
import com.reown.walletkit.use_cases.GetTransactionDetailsUseCase
import kotlinx.coroutines.*
import java.util.*

object WalletKit {
    private lateinit var coreClient: CoreInterface
    private lateinit var safeInteractor: SafeInteractor
    private val canFulfilUseCase: CanFulfilUseCase by wcKoinApp.koin.inject()
    private val fulfilmentStatusUseCase: FulfilmentStatusUseCase by wcKoinApp.koin.inject()
    private val estimateGasUseCase: EstimateGasUseCase by wcKoinApp.koin.inject()
    private val getTransactionDetailsUseCase: GetTransactionDetailsUseCase by wcKoinApp.koin.inject()
    private val getERC20TokenBalanceUseCase: GetERC20TokenBalanceUseCase by wcKoinApp.koin.inject()

    interface WalletDelegate {
        fun onSessionProposal(sessionProposal: Wallet.Model.SessionProposal, verifyContext: Wallet.Model.VerifyContext)
        val onSessionAuthenticate: ((Wallet.Model.SessionAuthenticate, Wallet.Model.VerifyContext) -> Unit)? get() = null
        fun onSessionRequest(sessionRequest: Wallet.Model.SessionRequest, verifyContext: Wallet.Model.VerifyContext)
        fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete)
        fun onSessionExtend(session: Wallet.Model.Session)

        //Responses
        fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse)
        fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse)

        //Utils
        fun onProposalExpired(proposal: Wallet.Model.ExpiredProposal) {
            //override me
        }

        fun onRequestExpired(request: Wallet.Model.ExpiredRequest) {
            //override me
        }

        fun onConnectionStateChange(state: Wallet.Model.ConnectionState)
        fun onError(error: Wallet.Model.Error)
    }

    @Throws(IllegalStateException::class)
    fun setWalletDelegate(delegate: WalletDelegate) {
        val isSessionAuthenticateImplemented = delegate.onSessionAuthenticate != null

        val signWalletDelegate = object : SignClient.WalletDelegate {
            override fun onSessionProposal(sessionProposal: Sign.Model.SessionProposal, verifyContext: Sign.Model.VerifyContext) {
                delegate.onSessionProposal(sessionProposal.toWallet(), verifyContext.toWallet())
            }

            override val onSessionAuthenticate: ((Sign.Model.SessionAuthenticate, Sign.Model.VerifyContext) -> Unit)?
                get() = if (isSessionAuthenticateImplemented) {
                    { sessionAuthenticate, verifyContext ->
                        delegate.onSessionAuthenticate?.invoke(sessionAuthenticate.toWallet(), verifyContext.toWallet())
                    }
                } else {
                    null
                }

            override fun onSessionRequest(sessionRequest: Sign.Model.SessionRequest, verifyContext: Sign.Model.VerifyContext) {
                delegate.onSessionRequest(sessionRequest.toWallet(), verifyContext.toWallet())
            }

            override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
                delegate.onSessionDelete(deletedSession.toWallet())
            }

            override fun onSessionExtend(session: Sign.Model.Session) {
                delegate.onSessionExtend(session.toWallet())
            }

            override fun onSessionSettleResponse(settleSessionResponse: Sign.Model.SettledSessionResponse) {
                delegate.onSessionSettleResponse(settleSessionResponse.toWallet())
            }

            override fun onSessionUpdateResponse(sessionUpdateResponse: Sign.Model.SessionUpdateResponse) {
                delegate.onSessionUpdateResponse(sessionUpdateResponse.toWallet())
            }

            override fun onProposalExpired(proposal: Sign.Model.ExpiredProposal) {
                delegate.onProposalExpired(proposal.toWallet())
            }

            override fun onRequestExpired(request: Sign.Model.ExpiredRequest) {
                delegate.onRequestExpired(request.toWallet())
            }

            override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {
                delegate.onConnectionStateChange(Wallet.Model.ConnectionState(state.isAvailable, state.reason?.toWallet()))
            }

            override fun onError(error: Sign.Model.Error) {
                delegate.onError(Wallet.Model.Error(error.throwable))
            }
        }

        SignClient.setWalletDelegate(signWalletDelegate)
    }

    @Throws(IllegalStateException::class)
    fun initialize(params: Wallet.Params.Init, onSuccess: () -> Unit = {}, onError: (Wallet.Model.Error) -> Unit) {
        wcKoinApp.modules(walletKitModule())
        coreClient = params.core
        if (params.pimlicoApiKey != null) {
            safeInteractor = SafeInteractor(params.pimlicoApiKey)
        }

        SignClient.initialize(Sign.Params.Init(params.core), onSuccess = onSuccess) { error ->
            if (error.throwable is SignClientAlreadyInitializedException) {
                onSuccess()
            } else {
                onError(Wallet.Model.Error(error.throwable))
            }
        }
    }

    @Throws(IllegalStateException::class)
    fun registerDeviceToken(firebaseAccessToken: String, enableEncrypted: Boolean = false, onSuccess: () -> Unit, onError: (Wallet.Model.Error) -> Unit) {
        coreClient.Echo.register(firebaseAccessToken, enableEncrypted, onSuccess) { error -> onError(Wallet.Model.Error(error)) }
    }

    @Throws(IllegalStateException::class)
    fun decryptMessage(params: Wallet.Params.DecryptMessage, onSuccess: (Wallet.Model.Message) -> Unit, onError: (Wallet.Model.Error) -> Unit) {
        scope.launch {
            SignClient.decryptMessage(
                Sign.Params.DecryptMessage(params.topic, params.encryptedMessage),
                onSuccess = { message ->
                    when (message) {
                        is Sign.Model.Message.SessionRequest -> onSuccess(message.toWallet())
                        is Sign.Model.Message.SessionProposal -> onSuccess(message.toWallet())
                        else -> { /*Ignore*/
                        }
                    }
                },
                onError = { signError -> onError(Wallet.Model.Error(signError.throwable)) })
        }
    }

    @Throws(IllegalStateException::class)
    fun dispatchEnvelope(urlWithEnvelope: String, onError: (Wallet.Model.Error) -> Unit) {
        scope.launch {
            try {
                SignClient.dispatchEnvelope(urlWithEnvelope) { error -> onError(Wallet.Model.Error(error.throwable)) }
            } catch (error: Exception) {
                onError(Wallet.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    fun pair(params: Wallet.Params.Pair, onSuccess: (Wallet.Params.Pair) -> Unit = {}, onError: (Wallet.Model.Error) -> Unit = {}) {
        coreClient.Pairing.pair(Core.Params.Pair(params.uri), { onSuccess(params) }, { error -> onError(Wallet.Model.Error(error.throwable)) })
    }

    @Throws(IllegalStateException::class)
    fun approveSession(
        params: Wallet.Params.SessionApprove,
        onSuccess: (Wallet.Params.SessionApprove) -> Unit = {},
        onError: (Wallet.Model.Error) -> Unit,
    ) {
        val signParams = Sign.Params.Approve(params.proposerPublicKey, params.namespaces.toSign(), params.properties, params.relayProtocol)
        SignClient.approveSession(signParams, { onSuccess(params) }, { error -> onError(Wallet.Model.Error(error.throwable)) })
    }

    @Throws(Exception::class)
    fun generateApprovedNamespaces(sessionProposal: Wallet.Model.SessionProposal, supportedNamespaces: Map<String, Wallet.Model.Namespace.Session>): Map<String, Wallet.Model.Namespace.Session> {
        return com.reown.sign.client.utils.generateApprovedNamespaces(sessionProposal.toSign(), supportedNamespaces.toSign()).toWallet()
    }

    @Throws(IllegalStateException::class)
    fun rejectSession(
        params: Wallet.Params.SessionReject,
        onSuccess: (Wallet.Params.SessionReject) -> Unit = {},
        onError: (Wallet.Model.Error) -> Unit,
    ) {
        val signParams = Sign.Params.Reject(params.proposerPublicKey, params.reason)
        SignClient.rejectSession(signParams, { onSuccess(params) }, { error -> onError(Wallet.Model.Error(error.throwable)) })
    }

    @Throws(IllegalStateException::class)
    fun approveSessionAuthenticate(
        params: Wallet.Params.ApproveSessionAuthenticate,
        onSuccess: (Wallet.Params.ApproveSessionAuthenticate) -> Unit = {},
        onError: (Wallet.Model.Error) -> Unit,
    ) {
        val signParams = Sign.Params.ApproveAuthenticate(params.id, params.auths.toSign())
        SignClient.approveAuthenticate(signParams, { onSuccess(params) }, { error -> onError(Wallet.Model.Error(error.throwable)) })
    }

    @Throws(IllegalStateException::class)
    fun rejectSessionAuthenticate(
        params: Wallet.Params.RejectSessionAuthenticate,
        onSuccess: (Wallet.Params.RejectSessionAuthenticate) -> Unit = {},
        onError: (Wallet.Model.Error) -> Unit,
    ) {
        val signParams = Sign.Params.RejectAuthenticate(params.id, params.reason)
        SignClient.rejectAuthenticate(signParams, { onSuccess(params) }, { error -> onError(Wallet.Model.Error(error.throwable)) })
    }

    @Throws(Exception::class)
    fun generateAuthObject(payloadParams: Wallet.Model.PayloadAuthRequestParams, issuer: String, signature: Wallet.Model.Cacao.Signature): Wallet.Model.Cacao {
        return com.reown.sign.client.utils.generateAuthObject(payloadParams.toSign(), issuer, signature.toSign()).toWallet()
    }

    @Throws(Exception::class)
    fun generateAuthPayloadParams(payloadParams: Wallet.Model.PayloadAuthRequestParams, supportedChains: List<String>, supportedMethods: List<String>): Wallet.Model.PayloadAuthRequestParams {
        return com.reown.sign.client.utils.generateAuthPayloadParams(payloadParams.toSign(), supportedChains, supportedMethods).toWallet()
    }

    @Throws(IllegalStateException::class)
    fun updateSession(
        params: Wallet.Params.SessionUpdate,
        onSuccess: (Wallet.Params.SessionUpdate) -> Unit = {},
        onError: (Wallet.Model.Error) -> Unit,
    ) {
        val signParams = Sign.Params.Update(params.sessionTopic, params.namespaces.toSign())
        SignClient.update(signParams, { onSuccess(params) }, { error -> onError(Wallet.Model.Error(error.throwable)) })
    }

    @Throws(IllegalStateException::class)
    fun extendSession(
        params: Wallet.Params.SessionExtend,
        onSuccess: (Wallet.Params.SessionExtend) -> Unit = {},
        onError: (Wallet.Model.Error) -> Unit,
    ) {
        val signParams = Sign.Params.Extend(params.topic)
        SignClient.extend(signParams, { onSuccess(params) }, { error -> onError(Wallet.Model.Error(error.throwable)) })
    }

    @Throws(IllegalStateException::class)
    fun respondSessionRequest(
        params: Wallet.Params.SessionRequestResponse,
        onSuccess: (Wallet.Params.SessionRequestResponse) -> Unit = {},
        onError: (Wallet.Model.Error) -> Unit,
    ) {
        val signParams = Sign.Params.Response(params.sessionTopic, params.jsonRpcResponse.toSign())
        SignClient.respond(signParams, { onSuccess(params) }, { error -> onError(Wallet.Model.Error(error.throwable)) })
    }


    @Throws(IllegalStateException::class)
    fun emitSessionEvent(
        params: Wallet.Params.SessionEmit,
        onSuccess: (Wallet.Params.SessionEmit) -> Unit = {},
        onError: (Wallet.Model.Error) -> Unit,
    ) {
        val signParams = Sign.Params.Emit(params.topic, params.event.toSign(), params.chainId)
        SignClient.emit(signParams, { onSuccess(params) }, { error -> onError(Wallet.Model.Error(error.throwable)) })
    }

    @Throws(IllegalStateException::class)
    fun disconnectSession(
        params: Wallet.Params.SessionDisconnect,
        onSuccess: (Wallet.Params.SessionDisconnect) -> Unit = {},
        onError: (Wallet.Model.Error) -> Unit,
    ) {
        val signParams = Sign.Params.Disconnect(params.sessionTopic)
        SignClient.disconnect(signParams, { onSuccess(params) }, { error -> onError(Wallet.Model.Error(error.throwable)) })
    }

    @Throws(IllegalStateException::class)
    fun pingSession(
        params: Wallet.Params.Ping,
        sessionPing: Wallet.Listeners.SessionPing?
    ) {
        val signParams = Sign.Params.Ping(params.sessionTopic)
        val signPingLister = object : Sign.Listeners.SessionPing {
            override fun onSuccess(pingSuccess: Sign.Model.Ping.Success) {
                sessionPing?.onSuccess(Wallet.Model.Ping.Success(pingSuccess.topic))
            }

            override fun onError(pingError: Sign.Model.Ping.Error) {
                sessionPing?.onError(Wallet.Model.Ping.Error(pingError.error))
            }
        }

        SignClient.ping(signParams, signPingLister)
    }

    //Yttrium

    @Throws(Throwable::class)
    @SmartAccountExperimentalApi
    fun getSmartAccount(params: Wallet.Params.GetSmartAccountAddress): String {
        check(::safeInteractor.isInitialized) { "Smart Accounts are not enabled" }

        val client = safeInteractor.getOrCreate(Account(params.owner.address))
        return runBlocking { client.getAddress() }
    }

    @Throws(Throwable::class)
    @SmartAccountExperimentalApi
    fun prepareSendTransactions(params: Wallet.Params.PrepareSendTransactions, onSuccess: (Wallet.Params.PrepareSendTransactionsResult) -> Unit) {
        check(::safeInteractor.isInitialized) { "Smart Accounts are not enabled" }

        val client = safeInteractor.getOrCreate(Account(params.owner.address))
        scope.launch {
            async { client.prepareSendTransactions(params.transactions.map { it.toYttrium() }).toWallet() }
                .await()
                .let(onSuccess)
        }
    }

    @Throws(Throwable::class)
    @SmartAccountExperimentalApi
    fun doSendTransactions(params: Wallet.Params.DoSendTransactions, onSuccess: (Wallet.Params.DoSendTransactionsResult) -> Unit) {
        check(::safeInteractor.isInitialized) { "Smart Accounts are not enabled" }

        val client = safeInteractor.getOrCreate(Account(params.owner.address))
        scope.launch {
            async { client.doSendTransactions(params.signatures.map { it.toYttrium() }, params.doSendTransactionParams) }
                .await()
                .let { userOpHash -> onSuccess(Wallet.Params.DoSendTransactionsResult(userOpHash)) }
        }
    }

    @Throws(Throwable::class)
    @SmartAccountExperimentalApi
    fun waitForUserOperationReceipt(params: Wallet.Params.WaitForUserOperationReceipt, onSuccess: (String) -> Unit) {
        check(::safeInteractor.isInitialized) { "Smart Accounts are not enabled" }

        val client = safeInteractor.getOrCreate(Account(params.owner.address))
        scope.launch {
            async { client.waitForUserOperationReceipt(params.userOperationHash) }
                .await()
                .let(onSuccess)
        }
    }

    //Chain Abstraction
    @ChainAbstractionExperimentalApi
    fun canFulfil(transaction: Wallet.Model.Transaction, onSuccess: (Wallet.Model.FulfilmentSuccess) -> Unit, onError: (Wallet.Model.FulfilmentError) -> Unit) {
        try {
            canFulfilUseCase(transaction, onSuccess, onError)
        } catch (e: Exception) {
            onError(Wallet.Model.FulfilmentError.Unknown(e.message ?: "Unknown error"))
        }
    }

    @ChainAbstractionExperimentalApi
    fun fulfillmentStatus(fulfilmentId: String, checkIn: Long, onSuccess: (Wallet.Model.FulfilmentStatus.Completed) -> Unit, onError: (Wallet.Model.FulfilmentStatus.Error) -> Unit) {
        try {
            fulfilmentStatusUseCase(fulfilmentId, checkIn, onSuccess, onError)
        } catch (e: Exception) {
            println("kobe: API error: $e")
            onError(Wallet.Model.FulfilmentStatus.Error(e.message ?: "Unknown error"))
        }
    }

    @Throws(Exception::class)
    @ChainAbstractionExperimentalApi
    fun estimateFees(chainId: String): Wallet.Model.EstimatedFees {
        return estimateGasUseCase(chainId)
    }

    @Throws(Exception::class)
    @ChainAbstractionExperimentalApi
    fun getERC20Balance(chainId: String, tokenAddress: String, ownerAddress: String): String {
        return getERC20TokenBalanceUseCase(chainId, tokenAddress, ownerAddress)
    }

    @ChainAbstractionExperimentalApi
    fun getFulfilmentDetails(
        available: Wallet.Model.FulfilmentSuccess.Available,
        initTransaction: Wallet.Model.Transaction,
        currency: Wallet.Model.Currency,
        onSuccess: (Wallet.Model.FulfilmentDetails) -> Unit,
        onError: (Wallet.Model.Error) -> Unit
    ) {
        try {
            getTransactionDetailsUseCase(available, initTransaction, currency, onSuccess, onError)
        } catch (e: Exception) {
            onError(Wallet.Model.Error(e))
        }
    }

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    @Throws(IllegalStateException::class)
    fun formatAuthMessage(params: Wallet.Params.FormatAuthMessage): String {
        val signParams = Sign.Params.FormatMessage(params.payloadParams.toSign(), params.issuer)
        return SignClient.formatAuthMessage(signParams)
    }

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    @Throws(IllegalStateException::class)
    @JvmStatic
    fun getListOfActiveSessions(): List<Wallet.Model.Session> {
        return SignClient.getListOfActiveSessions().map(Sign.Model.Session::toWallet)
    }

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    @Throws(IllegalStateException::class)
    fun getActiveSessionByTopic(topic: String): Wallet.Model.Session? {
        return SignClient.getActiveSessionByTopic(topic)?.toWallet()
    }


    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */

    @Throws(IllegalStateException::class)
    fun getPendingListOfSessionRequests(topic: String): List<Wallet.Model.SessionRequest> {
        return SignClient.getPendingSessionRequests(topic).mapToPendingSessionRequests()
    }


    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    @Throws(IllegalStateException::class)
    fun getSessionProposals(): List<Wallet.Model.SessionProposal> {
        return SignClient.getSessionProposals().map(Sign.Model.SessionProposal::toWallet)
    }

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    @Throws(IllegalStateException::class)
    fun getVerifyContext(id: Long): Wallet.Model.VerifyContext? {
        return SignClient.getVerifyContext(id)?.toWallet()
    }

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    @Throws(IllegalStateException::class)
    fun getListOfVerifyContexts(): List<Wallet.Model.VerifyContext> {
        return SignClient.getListOfVerifyContexts().map { verifyContext -> verifyContext.toWallet() }
    }
}