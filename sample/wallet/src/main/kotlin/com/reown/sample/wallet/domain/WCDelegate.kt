package com.reown.sample.wallet.domain

import android.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.sample.wallet.domain.model.Transaction.getInitialTransaction
import com.reown.walletkit.client.ChainAbstractionExperimentalApi
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

object WCDelegate : WalletKit.WalletDelegate, CoreClient.CoreDelegate {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _coreEvents: MutableSharedFlow<Core.Model> = MutableSharedFlow()
    val coreEvents: SharedFlow<Core.Model> = _coreEvents.asSharedFlow()

    internal val _walletEvents: MutableSharedFlow<Wallet.Model> = MutableSharedFlow()
    val walletEvents: SharedFlow<Wallet.Model> = _walletEvents.asSharedFlow()
    private val _connectionState: MutableSharedFlow<Wallet.Model.ConnectionState> = MutableSharedFlow(replay = 1)
    val connectionState: SharedFlow<Wallet.Model.ConnectionState> = _connectionState.asSharedFlow()
    var sessionProposalEvent: Pair<Wallet.Model.SessionProposal, Wallet.Model.VerifyContext>? = null
    var sessionAuthenticateEvent: Pair<Wallet.Model.SessionAuthenticate, Wallet.Model.VerifyContext>? = null
    var sessionRequestEvent: Pair<Wallet.Model.SessionRequest, Wallet.Model.VerifyContext>? = null
    var currentId: Long? = null

    //CA
    var prepareAvailable: Wallet.Model.PrepareSuccess.Available? = null
    var prepareError: Wallet.Model.PrepareError? = null


    init {
        CoreClient.setDelegate(this)
        WalletKit.setWalletDelegate(this)
    }

    override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {
        scope.launch {
            _connectionState.emit(state)
        }
    }

    override fun onError(error: Wallet.Model.Error) {
        mixPanel.track("error", JSONObject().put("error", error.throwable.stackTraceToString()))
        scope.launch {
            _walletEvents.emit(error)
        }
    }

    override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
        scope.launch {
            _walletEvents.emit(sessionDelete)
        }
    }

    override fun onSessionExtend(session: Wallet.Model.Session) {
        Log.d("Session Extend", "${session.expiry}")
    }

    override fun onSessionProposal(sessionProposal: Wallet.Model.SessionProposal, verifyContext: Wallet.Model.VerifyContext) {
        sessionProposalEvent = Pair(sessionProposal, verifyContext)

        scope.launch {
            _walletEvents.emit(sessionProposal)
        }
    }

    override val onSessionAuthenticate: (Wallet.Model.SessionAuthenticate, Wallet.Model.VerifyContext) -> Unit
        get() = { sessionAuthenticate, verifyContext ->
            sessionAuthenticateEvent = Pair(sessionAuthenticate, verifyContext)

            scope.launch {
                _walletEvents.emit(sessionAuthenticate)
            }
        }

    @OptIn(ChainAbstractionExperimentalApi::class)
    override fun onSessionRequest(sessionRequest: Wallet.Model.SessionRequest, verifyContext: Wallet.Model.VerifyContext) {
        emitSessionRequest(sessionRequest, verifyContext)

//        if (sessionRequest.request.method == "eth_sendTransaction") {
//            try {
//                val initTx = getInitialTransaction(sessionRequest)
//                println("Init TX: $initTx")
//                WalletKit.ChainAbstraction.prepare(
//                    initTx,
//                    listOf(SolanaAccountDelegate.keys.third),
//                    onSuccess = { result ->
//                        when (result) {
//                            is Wallet.Model.PrepareSuccess.Available -> {
//                                println("Prepare success available: $result")
//                                emitChainAbstractionRequest(sessionRequest, result, verifyContext)
//                            }
//
//                            is Wallet.Model.PrepareSuccess.NotRequired -> {
//                                println("Prepare success not required: $result")
//                                emitSessionRequest(sessionRequest, verifyContext)
//                            }
//                        }
//                    },
//                    onError = { error ->
//                        println("Prepare error: $error")
//                        emitSessionRequest(sessionRequest, verifyContext)
////                        if (error is Wallet.Model.PrepareError.Unknown) {
////                            emitSessionRequest(sessionRequest, verifyContext)
////                        } else {
////                            respondWithError(getErrorMessage(), sessionRequest)
////                            emitChainAbstractionError(sessionRequest, error, verifyContext)
////                        }
//                    }
//                )
//            } catch (e: Exception) {
//                println("Prepare: Unknown error: $e")
//                respondWithError(e.message ?: "Prepare: Unknown error", sessionRequest)
//                emitSessionRequest(sessionRequest, verifyContext)
//            }
//        } else {
//            emitSessionRequest(sessionRequest, verifyContext)
//        }
    }

    override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
        scope.launch {
            _walletEvents.emit(settleSessionResponse)
        }
    }

    override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
        scope.launch {
            _walletEvents.emit(sessionUpdateResponse)
        }
    }

    override fun onProposalExpired(proposal: Wallet.Model.ExpiredProposal) {
        scope.launch {
            _walletEvents.emit(proposal)
        }
    }

    override fun onRequestExpired(request: Wallet.Model.ExpiredRequest) {
        scope.launch {
            _walletEvents.emit(request)
        }
    }

    override fun onPairingDelete(deletedPairing: Core.Model.DeletedPairing) {
        //Deprecated - pairings are automatically deleted
    }

    override fun onPairingExpired(expiredPairing: Core.Model.ExpiredPairing) {
        //Deprecated - pairings are automatically expired
    }

    override fun onPairingState(pairingState: Core.Model.PairingState) {
        scope.launch {
            _coreEvents.emit(pairingState)
        }
    }
}