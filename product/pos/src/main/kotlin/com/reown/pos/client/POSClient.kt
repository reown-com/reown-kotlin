package com.reown.pos.client

import android.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.wcKoinApp
import com.reown.android.relay.ConnectionType
import com.reown.pos.client.service.BlockchainApi
import com.reown.pos.client.service.createBlockchainApiModule
import com.reown.pos.client.service.model.BuildTransactionParams
import com.reown.pos.client.service.model.CheckTransactionParams
import com.reown.pos.client.service.model.JsonRpcBuildTransactionRequest
import com.reown.pos.client.service.model.JsonRpcCheckTransactionRequest
import com.reown.pos.client.use_case.CheckTransactionStatusUseCase
import com.reown.pos.client.use_case.createPOSModule
import com.reown.pos.client.utils.CaipUtil
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.core.qualifier.named
import java.net.URI

object POSClient {
    private lateinit var posDelegate: POSDelegate
    private var sessionNamespaces = mutableMapOf<String, POS.Model.Namespace>()
    private var paymentIntent: POS.Model.PaymentIntent? = null
    private var currentSessionTopic: String? = null
    private var transactionId: String? = null
    private val blockchainApi: BlockchainApi by lazy { wcKoinApp.koin.get() }
    private val checkTransactionStatusUseCase: CheckTransactionStatusUseCase by lazy { wcKoinApp.koin.get() }
    private val moshi: Moshi by lazy { wcKoinApp.koin.get(named(AndroidCommonDITags.MOSHI)) }

    interface POSDelegate {
        fun onEvent(event: POS.Model.PaymentEvent)
    }

    fun initialize(
        initParams: POS.Params.Init,
        onSuccess: () -> Unit = {},
        onError: (POS.Model.Error) -> Unit
    ) {
        try {
            require(initParams.chains.isNotEmpty()) { "Chains list cannot be empty" }
            require(initParams.chains.all { it.startsWith("eip155") }) { "Only EVM chains are supported, please provide eip155 chains" }

            sessionNamespaces["eip155"] = POS.Model.Namespace(
                chains = initParams.chains,
                methods = listOf("eth_sendTransaction"),
                events = listOf("chainChanged", "accountsChanged")
            )

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

            wcKoinApp.modules(
                createBlockchainApiModule(projectId = initParams.projectId, deviceId = initParams.deviceId),
                createPOSModule()
            )
        } catch (e: Exception) {
            onError(POS.Model.Error(e))
        }
    }

    @Throws(IllegalStateException::class)
    fun createPaymentIntent(intents: List<POS.Model.PaymentIntent>) {
        checkPOSDelegateInitialization()
        require(sessionNamespaces.isNotEmpty()) { "No chains set during the initialization" }
        require(intents.isNotEmpty()) { "No payment intents provided" }
        val paymentIntent = intents.first()
        require(paymentIntent.token.network.chainId.isNotBlank()) { "Chain ID cannot be empty" }
        require(CaipUtil.isValidChainId(paymentIntent.token.network.chainId)) { "Chain ID is not CAIP-2 compliant" }
        require(CaipUtil.isValidAccountId(paymentIntent.recipient)) { "Recipient address is not CAIP-10 compliant" }
        require(paymentIntent.amount.isNotBlank()) { "Amount cannot be empty" }
        require(paymentIntent.recipient.isNotBlank()) { "Recipient cannot be empty" }
        require(paymentIntent.token.standard.isNotBlank()) { "Recipient cannot be empty" }
        require(paymentIntent.token.symbol.isNotBlank()) { "Recipient cannot be empty" }
        require(paymentIntent.token.network.name.isNotBlank()) { "Recipient cannot be empty" }

        val availableChains = sessionNamespaces["eip155"]?.chains ?: emptyList()
        val intentChainIds = intents.map { it.token.network.chainId }
        val missingChainIds = intentChainIds.filter { chainId -> !availableChains.contains(chainId) }
        require(missingChainIds.isEmpty()) {
            "Chains [${missingChainIds.joinToString(", ")}] are not available in session namespaces. Available chains: [${
                availableChains.joinToString(", ")
            }]"
        }

        this.paymentIntent = paymentIntent
        val pairing = CoreClient.Pairing.create { error -> posDelegate.onEvent(POS.Model.PaymentEvent.ConnectionFailed(error.throwable)) }

        if (pairing != null) {
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
                onSuccess = { url ->
                    posDelegate.onEvent(POS.Model.PaymentEvent.QrReady(URI(pairing.uri)))
                },
                onError = { error ->
                    println("kobe: connect error: $error")
                    posDelegate.onEvent(POS.Model.PaymentEvent.ConnectionFailed(error.throwable))
                }
            )
        } else {
            posDelegate.onEvent(POS.Model.PaymentEvent.ConnectionFailed(Throwable("Failed to create pairing, please try again")))
        }
    }

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
                        posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = e))
                        disconnectSession(approvedSession.topic)
                    }
                }
            }
        }

        override fun onSessionRejected(rejectedSession: Sign.Model.RejectedSession) {
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
                                posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = e))
                                disconnectSession(response.topic)
                            }
                        }
                    }
                }

                is Sign.Model.JsonRpcResponse.JsonRpcError -> {
                    posDelegate.onEvent(POS.Model.PaymentEvent.PaymentRejected(message = result.message))
                    disconnectSession(response.topic)
                }
            }
        }

        override fun onError(error: Sign.Model.Error) {
            posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = error.throwable))
            currentSessionTopic?.let { disconnectSession(it) }
        }

        override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {}
        override fun onSessionUpdate(updatedSession: Sign.Model.UpdatedSession) {}
        override fun onSessionEvent(sessionEvent: Sign.Model.SessionEvent) {}
        override fun onSessionExtend(session: Sign.Model.Session) {}
        override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
            posDelegate.onEvent(POS.Model.PaymentEvent.ConnectionRejected)
        }

        override fun onProposalExpired(proposal: Sign.Model.ExpiredProposal) {}
        override fun onRequestExpired(request: Sign.Model.ExpiredRequest) {}
    }

    private suspend fun handleSessionApproved(approvedSession: Sign.Model.ApprovedSession) {
        posDelegate.onEvent(POS.Model.PaymentEvent.Connected)
        if (paymentIntent == null) {
            val errorMessage = "PaymentIntent undefined, please try again"
            posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = Exception(errorMessage)))
            disconnectSession(approvedSession.topic)
            return
        }

        val senderAddress = findSenderAddress(approvedSession, paymentIntent!!.token.network.chainId)
            ?: throw IllegalStateException("No matching account found for chain ${paymentIntent!!.token.network.chainId}")

        val buildTransactionRequest = JsonRpcBuildTransactionRequest(
            params = BuildTransactionParams(
                asset = paymentIntent!!.caip19Token,
                recipient = paymentIntent!!.recipient,
                sender = senderAddress,
                amount = paymentIntent!!.amount
            )
        )

        try {
            val buildTransactionResponse = blockchainApi.buildTransaction(buildTransactionRequest)

            if (buildTransactionResponse.error != null) {
                val errorMessage =
                    "Build transaction failed: ${buildTransactionResponse.error.message} (code: ${buildTransactionResponse.error.code})"
                posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = Exception(errorMessage)))
                disconnectSession(approvedSession.topic)
                return
            }

            val result = buildTransactionResponse.result
            if (result == null) {
                val errorMessage = "Build transaction response is null"
                posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = Exception(errorMessage)))
                disconnectSession(approvedSession.topic)
                return
            }

            transactionId = buildTransactionResponse.result.id
            val transactionRpc = result.transactionRpc
            val paramsJson = moshi.adapter(Any::class.java).toJson(transactionRpc.params)
            val request = Sign.Params.Request(
                sessionTopic = approvedSession.topic,
                method = transactionRpc.method,
                params = paramsJson,
                chainId = paymentIntent!!.token.network.chainId
            )

            SignClient.request(
                request = request,
                onSuccess = { sentRequest ->
                    posDelegate.onEvent(POS.Model.PaymentEvent.PaymentRequested)
                },
                onError = { error ->
                    posDelegate.onEvent(POS.Model.PaymentEvent.ConnectionFailed(error.throwable))
                    disconnectSession(approvedSession.topic)
                }
            )

        } catch (e: Exception) {
            val errorMessage = "Failed to build transaction: ${e.message}"
            posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = Exception(errorMessage)))
            disconnectSession(approvedSession.topic)
        }
    }

    private suspend fun handleSessionRequestResult(
        response: Sign.Model.JsonRpcResponse.JsonRpcResult,
        topic: String
    ) {
        if (response.result != null) {
            posDelegate.onEvent(POS.Model.PaymentEvent.PaymentBroadcasted)
            checkTransactionStatusUseCase.checkTransactionStatusWithPolling(
                sendResult = response.result!!,
                transactionId = transactionId!!
            ) { paymentEvent ->
                Log.e("POSClient", "Checking transaction status result: $paymentEvent")
                posDelegate.onEvent(paymentEvent)
                disconnectSession(topic)
            }
        } else {
            Log.e("POSClient", "Unexpected result type: ${null}")
            posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = Exception("Invalid transaction result format")))
            disconnectSession(topic)
            return
        }
    }

    private fun findSenderAddress(
        approvedSession: Sign.Model.ApprovedSession,
        chainId: String
    ): String? {
        println("kobe: Sender ChainID: $chainId")

        return approvedSession.namespaces.values
            .flatMap { session -> session.accounts }
            .firstOrNull { account ->
                account.startsWith(chainId)
            }
    }

    private fun disconnectSession(topic: String) {
        SignClient.disconnect(
            disconnect = Sign.Params.Disconnect(topic),
            onSuccess = { Log.d("POSClient", "Session disconnected") },
            onError = { e -> Log.d("POSClient", "Session disconnected error: $e") }
        )
        clear()
    }

    private fun clear() {
        currentSessionTopic = null
        sessionNamespaces = mutableMapOf()
        paymentIntent = null
        currentSessionTopic = null
        transactionId = null
    }

    private fun checkPOSDelegateInitialization() {
        check(::posDelegate.isInitialized) {
            "POSDelegate needs to be initialized first"
        }
    }
}
