package com.reown.pos.client

import android.app.Application
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.CoreInterface
import com.reown.android.CoreProtocol
import com.reown.android.internal.common.scope
import com.reown.android.relay.ConnectionType
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

    /**
     * POS delegate interface for handling events
     */
    interface POSDelegate {
        fun onEvent(event: POS.Model.PaymentEvent)
    }

    /**
     * Initialize the POS client
     */
    fun initialize(
        init: POS.Params.Init,
        onSuccess: () -> Unit = {},
        onError: (POS.Model.Error) -> Unit
    ) {
        try {
            val coreMetaData = Core.Model.AppMetaData(
                name = init.metaData.merchantName,
                description = init.metaData.description,
                url = init.metaData.url,
                icons = init.metaData.icons,
                redirect = null
            )

            CoreClient.initialize(
                application = init.application,
                projectId = init.projectId,
                metaData = coreMetaData,
                connectionType = ConnectionType.AUTOMATIC,
                onError = { error -> onError(POS.Model.Error(error.throwable)) }
            )

            SignClient.initialize(
                Sign.Params.Init(core = CoreClient),
                onSuccess = { onSuccess() },
                onError = { error -> onError(POS.Model.Error(error.throwable)) })

        } catch (e: Exception) {
            onError(POS.Model.Error(e))
        }
    }

    /**
     * Set the chain to use, EVM only
     */
    @Throws(IllegalStateException::class)
    fun setChains(chainIds: List<String>) {
        if (chainIds.any { chainId -> chainId.startsWith("eip155") }) {
            sessionNamespaces["eip155"] = POS.Model.Namespace(
                chains = chainIds,
                methods = listOf("eth_sendTransaction"),
                events = listOf("chainChanged", "accountsChanged")
            )
        } else {
            throw IllegalStateException("EVM only")
        }
    }

    /**
     * Create a payment intent
     */
    @Throws(IllegalStateException::class)
    fun createPaymentIntent(intents: List<POS.Model.PaymentIntent>) {

        println("kobe: createPaymentIntent called: $intents")

        //TODO: add intent fields validation
        checkPOSDelegateInitialization()
        if (sessionNamespaces.isEmpty()) throw IllegalStateException("No chain set, call setChains method first")
        if (intents.isEmpty()) throw IllegalStateException("No payment intents provided")
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
                    posDelegate.onEvent(POS.Model.PaymentEvent.ConnectionFailed(error.throwable))
                }
            )
        } else {
            posDelegate.onEvent(POS.Model.PaymentEvent.ConnectionFailed(Throwable("Pairing is null")))
        }
    }

    /**
     * Set the delegate for handling POS events
     */
    fun setDelegate(delegate: POSDelegate) {
        posDelegate = delegate

        val dappDelegate = object : SignClient.DappDelegate {

            override fun onSessionApproved(approvedSession: Sign.Model.ApprovedSession) {
                scope.launch {
                    supervisorScope {
                        posDelegate.onEvent(POS.Model.PaymentEvent.Connected)
                        val method = sessionNamespaces.values.first().methods.first()
                        val chainId = paymentIntents.first().chainId
                        val amount = paymentIntents.first().amount
                        val token = paymentIntents.first().token
                        val recipient = paymentIntents.first().recipient
                        var senderAddress: String? = null

                        //TODO: Build Request using server
                        print("kobe: Building request: $chainId, $amount, $token, $recipient ")
                        delay(2000)
                        print("kobe: Request built success")


                        //TODO: revisit
                        approvedSession.namespaces.forEach { (namespace, session) ->
                            // Check if the namespace key matches the chain ID from payment intent
                            senderAddress = when {
                                // If chains are not null and not empty, find the first account on the same chain as payment intent
                                session.chains != null && session.chains!!.isNotEmpty() -> {
                                    val chains = session.chains
                                    if (chains != null) {
                                        session.accounts.firstOrNull { account ->
                                            chains.any { chain ->
                                                chain == chainId || account.startsWith("$chain:")
                                            }
                                        }
                                    } else {
                                        null
                                    }
                                }

                                namespace == chainId -> {
                                    session.accounts.firstOrNull()
                                }

                                else -> null
                            }
                        }

                        if (senderAddress != null) {
                            val request = Sign.Params.Request(
                                sessionTopic = approvedSession.topic,
                                method = method,
                                params = "[{\"from\":\"$senderAddress\",\"to\":\"$recipient\",\"data\":\"0x\",\"gasLimit\":\"0x5208\",\"gasPrice\":\"0x0649534e00\",\"value\":\"$amount\",\"nonce\":\"0x07\"}]",
                                chainId = chainId
                            )

                            SignClient.request(
                                request = request,
                                onSuccess = { sentRequest -> posDelegate.onEvent(POS.Model.PaymentEvent.PaymentRequested) },
                                onError = { error -> posDelegate.onEvent(POS.Model.PaymentEvent.ConnectionFailed(error.throwable)) }
                            )

                        } else {
                            //TODO: disconnect?
                            posDelegate.onEvent(POS.Model.PaymentEvent.Error(POS.Model.PosError.General(Throwable("No matching account found"))))
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
                                posDelegate.onEvent(POS.Model.PaymentEvent.PaymentBroadcasted)

                                // TODO: Check transaction status on blockchain, handle server errors and timeout
                                print("kobe: Checking payment status..")
                                delay(2000)
                                print("kobe: Payment successful")

                                val txHash = result.toString()
                                //TODO: get txHash and receipt from server
                                posDelegate.onEvent(POS.Model.PaymentEvent.PaymentSuccessful(txHash = txHash, receipt = "test"))


                                SignClient.disconnect(
                                    disconnect = Sign.Params.Disconnect(response.topic),
                                    onSuccess = {
                                        println("kobe: Disconnect Success")
                                    },
                                    onError = {
                                        println("kobe: Disconnect Error")
                                    })
                            }
                        }
                    }

                    is Sign.Model.JsonRpcResponse.JsonRpcError -> {
                        val error = POS.Model.PosError.RejectedByUser(message = result.message)
                        posDelegate.onEvent(POS.Model.PaymentEvent.PaymentRejected(error))

                        SignClient.disconnect(
                            disconnect = Sign.Params.Disconnect(response.topic),
                            onSuccess = {
                                println("kobe: Disconnect Success")
                            },
                            onError = {
                                println("kobe: Disconnect Error")
                            })
                    }
                }
            }

            override fun onError(error: Sign.Model.Error) {
                posDelegate.onEvent(POS.Model.PaymentEvent.Error(POS.Model.PosError.General(error.throwable)))
            }

            override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {}

            override fun onSessionUpdate(updatedSession: Sign.Model.UpdatedSession) {}

            override fun onSessionEvent(sessionEvent: Sign.Model.SessionEvent) {}

            override fun onSessionExtend(session: Sign.Model.Session) {}

            override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {}

            override fun onProposalExpired(proposal: Sign.Model.ExpiredProposal) {}

            override fun onRequestExpired(request: Sign.Model.ExpiredRequest) {}
        }

        SignClient.setDappDelegate(dappDelegate)
    }

    private fun checkPOSDelegateInitialization() {
        check(::posDelegate.isInitialized) {
            "POSDelegate needs to be initialized first"
        }
    }
}
