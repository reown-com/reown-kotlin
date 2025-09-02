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

            wcKoinApp.modules(createBlockchainApiModule(projectId = initParams.projectId, deviceId = initParams.deviceId))
        } catch (e: Exception) {
            onError(POS.Model.Error(e))
        }
    }

    @Throws(IllegalStateException::class)
    fun createPaymentIntent(intents: List<POS.Model.PaymentIntent>) {
        checkPOSDelegateInitialization()
        require(sessionNamespaces.isNotEmpty()) { "No chains set during the initialization" }
        require(intents.isNotEmpty()) { "No payment intents provided" }

        //TODO: Validation for chainId CAIP2 and receipient CAIP10
        val paymentIntent = intents.first()

        println("kobe: PaymentIntent: $paymentIntent")
        require(paymentIntent.token.network.chainId.isNotBlank()) { "Chain ID cannot be empty" }
        require(paymentIntent.amount.isNotBlank()) { "Amount cannot be empty" }
        require(paymentIntent.recipient.isNotBlank()) { "Recipient cannot be empty" }
        require(paymentIntent.token.standard.isNotBlank()) { "Recipient cannot be empty" }
        require(paymentIntent.token.symbol.isNotBlank()) { "Recipient cannot be empty" }
        require(paymentIntent.token.network.name.isNotBlank()) { "Recipient cannot be empty" }

        //Only EVM for now
        val availableChains = sessionNamespaces["eip155"]?.chains ?: emptyList()
        val intentChainIds = intents.map { it.token.network.chainId }
        val missingChainIds = intentChainIds.filter { chainId -> !availableChains.contains(chainId) }
        require(missingChainIds.isEmpty()) {
            "Chains [${missingChainIds.joinToString(", ")}] are not available in session namespaces. Available chains: [${
                availableChains.joinToString(", ")
            }]"
        }

        this.paymentIntent = paymentIntent

        val pairing = CoreClient.Pairing.create { error ->
            posDelegate.onEvent(POS.Model.PaymentEvent.ConnectionFailed(error.throwable))
        }

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

        val namespace = sessionNamespaces.values.firstOrNull()
            ?: throw IllegalStateException("No namespace available")

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
//            if (transactionRpc.method != "eth_sendTransaction") {
//                val errorMessage = "Unexpected transaction method: ${transactionRpc.method}, expected: eth_sendTransaction"
//                posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = Exception(errorMessage)))
//                disconnectSession(approvedSession.topic)
//                return
//            }

//            if (transactionRpc.params.isEmpty()) {
//                val errorMessage = "Transaction parameters are empty"
//                posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = Exception(errorMessage)))
//                disconnectSession(approvedSession.topic)
//                return
//            }

//            if (transactionParam.to.isBlank() || transactionParam.from.isBlank() ||
//                transactionParam.gas.isBlank() || transactionParam.value.isBlank() ||
//                transactionParam.data.isBlank() || transactionParam.gasPrice.isBlank()
//            ) {
//                val errorMessage = "Transaction parameters contain empty values"
//                posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = Exception(errorMessage)))
//                disconnectSession(approvedSession.topic)
//                return
//            }


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
            checkTransactionStatusWithPolling(response.result!!, topic)
        } else {
            Log.e("POSClient", "Unexpected result type: ${null}")
            posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = Exception("Invalid transaction result format")))
            disconnectSession(topic)
            return
        }
    }

    private suspend fun checkTransactionStatusWithPolling(sendResult: String, topic: String) {
        val maxAttempts = 10
        var currentAttempt = 0

        while (currentAttempt < maxAttempts) {
            try {
                if (transactionId.isNullOrBlank()) {
                    val errorMessage = "Check transaction status transaction id not defined"
                    Log.e("POSClient", errorMessage)
                    posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = Exception(errorMessage)))
                    disconnectSession(topic)
                    return
                }

                val checkTransactionRequest =
                    JsonRpcCheckTransactionRequest(params = CheckTransactionParams(id = transactionId!!, sendResult = sendResult))
                val checkTransactionResponse = blockchainApi.checkTransactionStatus(checkTransactionRequest)

                if (checkTransactionResponse.error != null) {
                    val errorMessage =
                        "Check transaction status failed: ${checkTransactionResponse.error.message} (code: ${checkTransactionResponse.error.code})"
                    Log.e("POSClient", errorMessage)
                    posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = Exception(errorMessage)))
                    disconnectSession(topic)
                    return
                }

                val result = checkTransactionResponse.result
                if (result == null) {
                    val errorMessage = "Check transaction status response is null"
                    Log.e("POSClient", errorMessage)
                    posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = Exception(errorMessage)))
                    disconnectSession(topic)
                    return
                }

                when (result.status.uppercase()) {
                    "CONFIRMED" -> {
                        Log.d("POSClient", "Transaction confirmed: $sendResult")
                        posDelegate.onEvent(POS.Model.PaymentEvent.PaymentSuccessful(txHash = sendResult))
                        disconnectSession(topic)
                        return
                    }

                    "FAILED" -> {
                        Log.e("POSClient", "Transaction failed: $sendResult")
                        posDelegate.onEvent(
                            POS.Model.PaymentEvent.PaymentRejected(
                                message = checkTransactionResponse.error?.message ?: "Failed transaction error"
                            )
                        )
                        disconnectSession(topic)
                        return
                    }

                    "PENDING" -> {
                        Log.d("POSClient", "Transaction pending, attempt ${currentAttempt + 1}/$maxAttempts")
                        currentAttempt++
                        if (currentAttempt < maxAttempts) {
                            delay(checkTransactionResponse.result.checkIn ?: 3000) // Wait 3 seconds before next attempt
                        }
                    }

                    else -> {
                        val errorMessage = "Unknown transaction status: ${result.status}"
                        Log.e("POSClient", errorMessage)
                        posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = Exception(errorMessage)))
                        disconnectSession(topic)
                        return
                    }
                }
            } catch (e: Exception) {
                val errorMessage = "Failed to check transaction status: ${e.message}"
                Log.e("POSClient", errorMessage, e)
                posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = Exception(errorMessage)))
                disconnectSession(topic)
                return
            }
        }

        // If we reach here, the transaction is still pending after 30 seconds
        Log.w("POSClient", "Transaction still pending after 30 seconds: $sendResult")
        posDelegate.onEvent(POS.Model.PaymentEvent.Error(error = Exception("Transaction still pending after timeout")))
        disconnectSession(topic)
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
