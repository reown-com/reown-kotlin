package com.reown.pos.client

import android.app.Application
import com.reown.android.Core
import com.reown.android.CoreInterface
import com.reown.android.CoreProtocol
import com.reown.android.relay.ConnectionType
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient

object POSClient {
    private lateinit var coreClient: CoreInterface
    private val sessionNamespaces = mutableMapOf<String, POS.Model.Namespace>()
    lateinit private var posDelegate: POSDelegate

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

            coreClient = CoreProtocol.instance
            coreClient.initialize(
                application = init.application as Application,
                projectId = init.projectId,
                metaData = coreMetaData,
                connectionType = ConnectionType.AUTOMATIC,
                onError = { error -> onError(POS.Model.Error(error.throwable)) }
            )

            SignClient.initialize(
                Sign.Params.Init(coreClient),
                onSuccess = { onSuccess() },
                onError = { error -> onError(POS.Model.Error(error.throwable)) })

        } catch (e: Exception) {
            onError(POS.Model.Error(e))
        }
    }

    /**
     * Set the chain to use, EVM only
     */
    fun setChain(chainIds: List<String>) {
        sessionNamespaces["eip155"] = POS.Model.Namespace(
            chains = chainIds,
            methods = listOf("eth_sendTransaction"),
            events = listOf("chainChanged", "accountsChanged")
        )
    }

    /**
     * Create a payment intent
     */
    fun createPaymentIntent(paymentIntents: List<POS.Model.PaymentIntent>) {
//        - Generates the connection URL
//        - Sends the connection proposal to the wallet
//        - Awaits the connection result
//        - Builds and sends the transaction to the wallet
//        - Awaits the transaction result from the wallet
//        - Checks the transaction status
    }

    /**
     * Set the delegate for handling POS events
     */
    fun setDelegate(delegate: POSDelegate) {
        posDelegate = delegate
        val dappDelegate = object : SignClient.DappDelegate {
            override fun onSessionApproved(approvedSession: Sign.Model.ApprovedSession) {
                TODO("Not yet implemented")
            }

            override fun onSessionRejected(rejectedSession: Sign.Model.RejectedSession) {
                TODO("Not yet implemented")
            }

            override fun onSessionRequestResponse(response: Sign.Model.SessionRequestResponse) {
                TODO("Not yet implemented")
            }

            override fun onError(error: Sign.Model.Error) {
                TODO("Not yet implemented")
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
}
