package com.reown.pos.client

import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.wcKoinApp
import com.reown.android.relay.ConnectionType
import com.reown.pos.client.service.BlockchainApi
import com.reown.pos.client.service.createBlockchainApiModule
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.net.URI

object POSClient {
    private lateinit var posDelegate: POSDelegate
    private val sessionNamespaces = mutableMapOf<String, POS.Model.Namespace>()
    private var paymentIntents: List<POS.Model.PaymentIntent> = emptyList()
    private var currentSessionTopic: String? = null
    private val blockchainApi: BlockchainApi by lazy { wcKoinApp.koin.get() }

    interface POSDelegate {
        fun onEvent(event: POS.Model.PaymentEvent)
    }

    fun initialize(
        initParams: POS.Params.Init,
        onSuccess: () -> Unit = {},
        onError: (POS.Model.Error) -> Unit
    ) {
        try {
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
    fun setChains(chainIds: List<String>) {
        require(chainIds.isNotEmpty()) { "Chain IDs list cannot be empty" }
        require(chainIds.all { it.startsWith("eip155") }) { "Only EVM chains are supported, please provide eip155 chains" }

        sessionNamespaces["eip155"] = POS.Model.Namespace(
            chains = chainIds,
            methods = listOf("eth_sendTransaction"),
            events = listOf("chainChanged", "accountsChanged")
        )
    }

    @Throws(IllegalStateException::class)
    fun createPaymentIntent(intents: List<POS.Model.PaymentIntent>) {
        checkPOSDelegateInitialization()
        require(sessionNamespaces.isNotEmpty()) { "No chains set, call setChains method first" }
        require(intents.isNotEmpty()) { "No payment intents provided" }

        intents.forEach { intent ->
            require(intent.chainId.isNotBlank()) { "Chain ID cannot be empty" }
            require(intent.amount.isNotBlank()) { "Amount cannot be empty" }
            require(intent.token.isNotBlank()) { "Token cannot be empty" }
            require(intent.recipient.isNotBlank()) { "Recipient cannot be empty" }
        }

        //Only EVM for now
        val availableChains = sessionNamespaces["eip155"]?.chains ?: emptyList()
        val intentChainIds = intents.map { it.chainId }
        val missingChainIds = intentChainIds.filter { chainId -> !availableChains.contains(chainId) }
        require(missingChainIds.isEmpty()) {
            "Chain IDs [${missingChainIds.joinToString(", ")}] are not available in session namespaces. Available chains: [${
                availableChains.joinToString(", ")
            }]"
        }

        paymentIntents = intents

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
            posDelegate.onEvent(POS.Model.PaymentEvent.ConnectedRejected)
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
        override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {}
        override fun onProposalExpired(proposal: Sign.Model.ExpiredProposal) {}
        override fun onRequestExpired(request: Sign.Model.ExpiredRequest) {}
    }

    private suspend fun handleSessionApproved(approvedSession: Sign.Model.ApprovedSession) {
        posDelegate.onEvent(POS.Model.PaymentEvent.Connected)

        val paymentIntent = paymentIntents.firstOrNull()
            ?: throw IllegalStateException("No payment intent available")

        val namespace = sessionNamespaces.values.firstOrNull()
            ?: throw IllegalStateException("No namespace available")

        val method = namespace.methods.firstOrNull()
            ?: throw IllegalStateException("No method available")

        //TODO: Mocking endpoint to build the transaction
        delay(3000)
//        val buildRequest = JsonRpcRequest(
//
//        )
//        blockchainApi.sendJsonRpcRequest()

        val senderAddress = findSenderAddress(approvedSession, paymentIntent.chainId)
            ?: throw IllegalStateException("No matching account found for chain ${paymentIntent.chainId}")

        val request = Sign.Params.Request(
            sessionTopic = approvedSession.topic,
            method = method,
            params = buildTransactionParams(senderAddress, paymentIntent),
            chainId = paymentIntent.chainId
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
    }

    private suspend fun handleSessionRequestResult(
        result: Sign.Model.JsonRpcResponse.JsonRpcResult,
        topic: String
    ) {
        posDelegate.onEvent(POS.Model.PaymentEvent.PaymentBroadcasted)

        // TODO: Mocking transaction status check on server
        delay(4000)

        //TODO: get txHash and receipt from server
        val txHash = result.toString()
        posDelegate.onEvent(POS.Model.PaymentEvent.PaymentSuccessful(txHash = txHash, receipt = "tx_hash_test"))

        disconnectSession(topic)
    }

    private fun findSenderAddress(
        approvedSession: Sign.Model.ApprovedSession,
        chainId: String
    ): String? {
        return approvedSession.namespaces.entries.firstNotNullOfOrNull { (namespace, session) ->
            when {
                session.chains?.isNotEmpty() == true -> {
                    session.accounts.firstOrNull { account ->
                        session.chains!!.any { chain ->
                            chain == chainId || account.startsWith("$chain:")
                        }
                    }
                }

                namespace == chainId -> session.accounts.firstOrNull()
                else -> null
            }
        }
    }

    //TODO: Get from the server
    private fun buildTransactionParams(senderAddress: String, paymentIntent: POS.Model.PaymentIntent): String {
        return """[{"from":"$senderAddress","to":"${paymentIntent.recipient}","data":"0x","gasLimit":"0x5208","gasPrice":"0x0649534e00","value":"${paymentIntent.amount}","nonce":"0x07"}]"""
    }

    private fun disconnectSession(topic: String) {
        SignClient.disconnect(
            disconnect = Sign.Params.Disconnect(topic),
            onSuccess = { /* Session disconnected successfully */ },
            onError = { /* Log disconnect error if needed */ }
        )
        currentSessionTopic = null
    }

    private fun checkPOSDelegateInitialization() {
        check(::posDelegate.isInitialized) {
            "POSDelegate needs to be initialized first"
        }
    }
}
