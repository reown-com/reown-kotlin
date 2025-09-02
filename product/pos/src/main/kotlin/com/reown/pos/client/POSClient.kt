package com.reown.pos.client

import android.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.wcKoinApp
import com.reown.android.relay.ConnectionType
import com.reown.pos.client.service.createBlockchainApiModule
import com.reown.pos.client.use_case.BuildTransactionUseCase
import com.reown.pos.client.use_case.CheckTransactionStatusUseCase
import com.reown.pos.client.use_case.createPOSModule
import com.reown.pos.client.utils.CaipUtil
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.net.URI

/**
 * Main POS (Point of Sale) client for handling blockchain payment transactions.
 * Manages wallet connections, transaction building, and payment processing.
 */
object POSClient {
    private lateinit var posDelegate: POSDelegate
    private var sessionNamespaces = mutableMapOf<String, POS.Model.Namespace>()
    private var paymentIntent: POS.Model.PaymentIntent? = null
    private var currentSessionTopic: String? = null
    private var transactionId: String? = null

    private val checkTransactionStatusUseCase: CheckTransactionStatusUseCase by lazy { wcKoinApp.koin.get() }
    private val buildTransactionUseCase: BuildTransactionUseCase by lazy { wcKoinApp.koin.get() }

    interface POSDelegate {
        fun onEvent(event: POS.Model.PaymentEvent)
    }

    /**
     * Initializes the POS client with the provided configuration.
     *
     * @param initParams Configuration parameters for initialization
     * @param onSuccess Callback for successful initialization
     * @param onError Callback for handling initialization errors
     */
    fun initialize(
        initParams: POS.Params.Init,
        onSuccess: () -> Unit = {},
        onError: (POS.Model.Error) -> Unit
    ) {
        try {
            validateInitParams(initParams)
            setupSessionNamespaces(initParams.chains)
            initializeClients(initParams, onSuccess, onError)
            setupDependencyInjection(initParams)
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
            onError(POS.Model.Error(e))
        }
    }

    private fun validateInitParams(initParams: POS.Params.Init) {
        require(initParams.chains.isNotEmpty()) { "Chains list cannot be empty" }
        require(initParams.chains.all { it.startsWith(EIP155_NAMESPACE) }) {
            "Only EVM chains are supported, please provide eip155 chains"
        }
    }

    private fun setupSessionNamespaces(chains: List<String>) {
        sessionNamespaces[EIP155_NAMESPACE] = POS.Model.Namespace(
            chains = chains,
            methods = listOf(ETH_SEND_TRANSACTION_METHOD),
            events = listOf(CHAIN_CHANGED_EVENT, ACCOUNTS_CHANGED_EVENT)
        )
    }

    private fun initializeClients(
        initParams: POS.Params.Init,
        onSuccess: () -> Unit,
        onError: (POS.Model.Error) -> Unit
    ) {
        val coreMetaData = Core.Model.AppMetaData(
            name = initParams.metaData.merchantName,
            description = initParams.metaData.description,
            url = initParams.metaData.url,
            icons = initParams.metaData.icons,
            redirect = null
        )

        CoreClient.initialize(
            application = initParams.application,
            projectId = initParams.projectId,
            metaData = coreMetaData,
            connectionType = ConnectionType.AUTOMATIC,
            onError = { error -> onError(POS.Model.Error(error.throwable)) }
        )

        SignClient.initialize(
            Sign.Params.Init(core = CoreClient),
            onSuccess = { onSuccess() },
            onError = { error -> onError(POS.Model.Error(error.throwable)) }
        )
    }

    private fun setupDependencyInjection(initParams: POS.Params.Init) {
        wcKoinApp.modules(
            createBlockchainApiModule(
                projectId = initParams.projectId,
                deviceId = initParams.deviceId
            ),
            createPOSModule()
        )
    }

    /**
     * Creates a payment intent and initiates wallet connection.
     *
     * @param intents List of payment intents (only the first one is used)
     * @throws IllegalStateException if delegate is not initialized or validation fails
     */
    @Throws(IllegalStateException::class)
    fun createPaymentIntent(intents: List<POS.Model.PaymentIntent>) {
        checkPOSDelegateInitialization()
        validatePaymentIntents(intents)

        val paymentIntent = intents.first()
        validatePaymentIntent(paymentIntent)
        validateChainCompatibility(intents)

        this.paymentIntent = paymentIntent
        initiateWalletConnection()
    }

    private fun validatePaymentIntents(intents: List<POS.Model.PaymentIntent>) {
        require(sessionNamespaces.isNotEmpty()) { "No chains set during the initialization" }
        require(intents.isNotEmpty()) { "No payment intents provided" }
    }

    private fun validatePaymentIntent(paymentIntent: POS.Model.PaymentIntent) {
        require(paymentIntent.token.network.chainId.isNotBlank()) { "Chain ID cannot be empty" }
        require(CaipUtil.isValidChainId(paymentIntent.token.network.chainId)) {
            "Chain ID is not CAIP-2 compliant"
        }
        require(CaipUtil.isValidAccountId(paymentIntent.recipient)) {
            "Recipient address is not CAIP-10 compliant"
        }
        require(paymentIntent.amount.isNotBlank()) { "Amount cannot be empty" }
        require(paymentIntent.recipient.isNotBlank()) { "Recipient cannot be empty" }
        require(paymentIntent.token.standard.isNotBlank()) { "Token standard cannot be empty" }
        require(paymentIntent.token.symbol.isNotBlank()) { "Token symbol cannot be empty" }
        require(paymentIntent.token.network.name.isNotBlank()) { "Network name cannot be empty" }
    }

    private fun validateChainCompatibility(intents: List<POS.Model.PaymentIntent>) {
        val availableChains = sessionNamespaces[EIP155_NAMESPACE]?.chains ?: emptyList()
        val intentChainIds = intents.map { it.token.network.chainId }
        val missingChainIds = intentChainIds.filter { chainId -> !availableChains.contains(chainId) }

        require(missingChainIds.isEmpty()) {
            "Chains [${missingChainIds.joinToString(", ")}] are not available in session namespaces. " +
                    "Available chains: [${availableChains.joinToString(", ")}]"
        }
    }

    private fun initiateWalletConnection() {
        val pairing = CoreClient.Pairing.create { error ->
            posDelegate.onEvent(POS.Model.PaymentEvent.ConnectionFailed(error.throwable))
        }

        if (pairing != null) {
            connectToWallet(pairing)
        } else {
            val errorMessage = "Failed to create pairing, please try again"
            Log.e(TAG, errorMessage)
            posDelegate.onEvent(POS.Model.PaymentEvent.ConnectionFailed(Throwable(errorMessage)))
        }
    }

    private fun connectToWallet(pairing: Core.Model.Pairing) {
        val signNamespaces = sessionNamespaces.mapValues { (_, namespace) ->
            Sign.Model.Namespace.Proposal(
                chains = namespace.chains,
                methods = namespace.methods,
                events = namespace.events
            )
        }

        val connectParams = Sign.Params.ConnectParams(
            sessionNamespaces = signNamespaces,
            pairing = pairing
        )

        SignClient.connect(
            connectParams = connectParams,
            onSuccess = { _ ->
                posDelegate.onEvent(POS.Model.PaymentEvent.QrReady(URI(pairing.uri)))
            },
            onError = { error ->
                Log.e(TAG, "Wallet connection failed", error.throwable)
                posDelegate.onEvent(POS.Model.PaymentEvent.ConnectionFailed(error.throwable))
            }
        )
    }

    /**
     * Sets the delegate for handling POS events.
     *
     * @param delegate The delegate to receive POS events
     */
    fun setDelegate(delegate: POSDelegate) {
        posDelegate = delegate
        SignClient.setDappDelegate(createDappDelegate())
    }

    private fun createDappDelegate() = object : SignClient.DappDelegate {
        override fun onSessionApproved(approvedSession: Sign.Model.ApprovedSession) {
            currentSessionTopic = approvedSession.topic
            scope.launch {
                supervisorScope {
                    try {
                        handleSessionApproved(approvedSession)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling session approval", e)
                        posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = e))
                        disconnectSession(approvedSession.topic)
                    }
                }
            }
        }

        override fun onSessionRejected(rejectedSession: Sign.Model.RejectedSession) {
            Log.d(TAG, "Session rejected")
            posDelegate.onEvent(POS.Model.PaymentEvent.ConnectionRejected)
        }

        override fun onSessionRequestResponse(response: Sign.Model.SessionRequestResponse) {
            when (val result = response.result) {
                is Sign.Model.JsonRpcResponse.JsonRpcResult -> {
                    scope.launch {
                        supervisorScope {
                            try {
                                handleSessionRequestResult(result, response.topic)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error handling session request result", e)
                                posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = e))
                                disconnectSession(response.topic)
                            }
                        }
                    }
                }

                is Sign.Model.JsonRpcResponse.JsonRpcError -> {
                    Log.e(TAG, "Session request error: ${result.message}")
                    posDelegate.onEvent(POS.Model.PaymentEvent.PaymentRejected(message = result.message))
                    disconnectSession(response.topic)
                }
            }
        }

        override fun onError(error: Sign.Model.Error) {
            Log.e(TAG, "Sign client error", error.throwable)
            posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = error.throwable))
            currentSessionTopic?.let { disconnectSession(it) }
        }

        override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {
            Log.d(TAG, "Connection state changed: $state")
        }

        override fun onSessionUpdate(updatedSession: Sign.Model.UpdatedSession) {
            Log.d(TAG, "Session updated")
        }

        override fun onSessionEvent(sessionEvent: Sign.Model.SessionEvent) {
            Log.d(TAG, "Session event: ${sessionEvent.name}")
        }

        override fun onSessionExtend(session: Sign.Model.Session) {
            Log.d(TAG, "Session extended")
        }

        override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
            Log.d(TAG, "Session deleted")
            posDelegate.onEvent(POS.Model.PaymentEvent.ConnectionRejected)
        }

        override fun onProposalExpired(proposal: Sign.Model.ExpiredProposal) {
            Log.d(TAG, "Proposal expired")
        }

        override fun onRequestExpired(request: Sign.Model.ExpiredRequest) {
            Log.d(TAG, "Request expired")
        }
    }

    private suspend fun handleSessionApproved(approvedSession: Sign.Model.ApprovedSession) {
        Log.d(TAG, "Session approved, building transaction")
        posDelegate.onEvent(POS.Model.PaymentEvent.Connected)

        buildTransactionUseCase.build(
            paymentIntent,
            approvedSession,
            onSuccess = { (event, transactionId) ->
                this.transactionId = transactionId
                posDelegate.onEvent(event)
            },
            onError = { event ->
                posDelegate.onEvent(event)
                disconnectSession(approvedSession.topic)
            }
        )
    }

    private suspend fun handleSessionRequestResult(
        response: Sign.Model.JsonRpcResponse.JsonRpcResult,
        topic: String
    ) {
        val transactionHash = response.result
        if (transactionHash != null) {
            Log.d(TAG, "Transaction broadcasted: $transactionHash")
            posDelegate.onEvent(POS.Model.PaymentEvent.PaymentBroadcasted)

            checkTransactionStatusUseCase.checkTransactionStatusWithPolling(
                sendResult = transactionHash,
                transactionId = transactionId ?: run {
                    Log.e(TAG, "Transaction ID is null")
                    posDelegate.onEvent(
                        POS.Model.PaymentEvent.Error(
                            error = Exception("Transaction ID is null")
                        )
                    )
                    disconnectSession(topic)
                    return
                }
            ) { paymentEvent ->
                Log.d(TAG, "Transaction status check result: $paymentEvent")
                posDelegate.onEvent(paymentEvent)
                disconnectSession(topic)
            }
        } else {
            Log.e(TAG, "Transaction result is null")
            posDelegate.onEvent(
                POS.Model.PaymentEvent.Error(
                    error = Exception("Invalid transaction result format")
                )
            )
            disconnectSession(topic)
        }
    }

    private fun disconnectSession(topic: String) {
        Log.d(TAG, "Disconnecting session: $topic")
        SignClient.disconnect(
            disconnect = Sign.Params.Disconnect(topic),
            onSuccess = { Log.d(TAG, "Session disconnected successfully") },
            onError = { e -> Log.e(TAG, "Session disconnect error", e.throwable) }
        )
        clear()
    }

    private fun clear() {
        Log.d(TAG, "Clearing client state")
        currentSessionTopic = null
        sessionNamespaces = mutableMapOf()
        paymentIntent = null
        transactionId = null
    }

    private fun checkPOSDelegateInitialization() {
        check(::posDelegate.isInitialized) {
            "POSDelegate needs to be initialized first"
        }
    }

    private const val TAG = "POSClient"
    private const val EIP155_NAMESPACE = "eip155"
    private const val ETH_SEND_TRANSACTION_METHOD = "eth_sendTransaction"
    private const val CHAIN_CHANGED_EVENT = "chainChanged"
    private const val ACCOUNTS_CHANGED_EVENT = "accountsChanged"
}
