package com.reown.sample.wallet.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.android.Core
import com.reown.android.internal.common.exception.InvalidProjectIdException
import com.reown.android.internal.common.exception.ProjectIdDoesNotExistException
import com.reown.sample.wallet.domain.WalletKitDelegate
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.chain_abstraction.emitSessionRequest
import com.reown.sample.wallet.domain.payment.PaymentRepository
import com.reown.sample.wallet.ui.state.ConnectionState
import com.reown.sample.wallet.ui.state.PairingEvent
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class Web3WalletViewModel : ViewModel() {
    private val connectivityStateFlow: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Idle)
    val connectionState = connectivityStateFlow.asStateFlow()

    private val _eventsSharedFlow: MutableSharedFlow<PairingEvent> = MutableSharedFlow()
    val eventsSharedFlow = _eventsSharedFlow.asSharedFlow()

    private val _isLoadingFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoadingFlow = _isLoadingFlow.asSharedFlow()

    private val _isRequestLoadingFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRequestLoadingFlow = _isRequestLoadingFlow.asSharedFlow()

    private val _timerFlow: MutableStateFlow<String> = MutableStateFlow("0")
    val timerFlow = _timerFlow.asStateFlow()

    private val _sessionRequestStateFlow: MutableSharedFlow<SignEvent.SessionRequest> = MutableSharedFlow()
    val sessionRequestStateFlow = _sessionRequestStateFlow.asSharedFlow()

    init {
        WalletKitDelegate.coreEvents.onEach { coreEvent ->
            _isLoadingFlow.value = (coreEvent as? Core.Model.PairingState)?.isPairingState ?: false
        }.launchIn(viewModelScope)

        flow {
            while (true) {
                emit(Unit)
                delay(1000)
            }
        }.onEach {
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            _timerFlow.value = dateFormat.format(timestamp)
        }.launchIn(viewModelScope)

        WalletKitDelegate.connectionState.onEach {
            val connectionState = if (it.isAvailable) {
                ConnectionState.Ok
            } else {
                val message = when (it.reason) {
                    is Wallet.Model.ConnectionState.Reason.ConnectionFailed -> {
                        if ((it.reason as Wallet.Model.ConnectionState.Reason.ConnectionFailed).throwable is ProjectIdDoesNotExistException ||
                            (it.reason as Wallet.Model.ConnectionState.Reason.ConnectionFailed).throwable is InvalidProjectIdException
                        ) "Invalid Project Id" else "Connection failed"
                    }

                    else -> "Connection closed"
                }

                ConnectionState.Error(message)
            }
            connectivityStateFlow.value = connectionState
        }.launchIn(viewModelScope)

    }

    val walletEvents = WalletKitDelegate.walletEvents.map { wcEvent ->
        Log.d("Web3Wallet", "VM: $wcEvent")

        when (wcEvent) {
            is Wallet.Model.ExpiredProposal -> {
                viewModelScope.launch {
                    _eventsSharedFlow.emit(PairingEvent.ProposalExpired("Proposal expired, please pair again"))
                }
            }

            is Wallet.Model.ExpiredRequest -> SignEvent.ExpiredRequest
            is Wallet.Model.SessionRequest -> {
                val topic = wcEvent.topic
                val icon = wcEvent.peerMetaData?.icons?.firstOrNull()
                val peerName = wcEvent.peerMetaData?.name
                val requestId = wcEvent.request.id.toString()
                val params = wcEvent.request.params
                val chain = wcEvent.chainId
                val method = wcEvent.request.method
                val arrayOfArgs: ArrayList<String?> = arrayListOf(topic, icon, peerName, requestId, params, chain, method)
                if (WalletKitDelegate.currentId != WalletKitDelegate.sessionRequestEvent?.first?.request?.id) {
                    _sessionRequestStateFlow.emit(SignEvent.SessionRequest(arrayOfArgs, arrayOfArgs.size))
                } else {
                    println("wallet request already there: ${wcEvent.request.id}")
                }
            }

            is Wallet.Model.PrepareSuccess.Available -> SignEvent.Fulfilment(isError = false)
            is Wallet.Model.PrepareError -> SignEvent.Fulfilment(isError = true)

            is Wallet.Model.SessionAuthenticate -> {
                _isLoadingFlow.value = false
                SignEvent.SessionAuthenticate
            }

            is Wallet.Model.SessionDelete -> SignEvent.Disconnect
            is Wallet.Model.SessionProposal -> {
                _isLoadingFlow.value = false
                SignEvent.SessionProposal
            }

            is Wallet.Model.Error -> {
                if (wcEvent.throwable.message?.contains("No proposal or pending session authenticate request for pairing topic") == true) {
                    viewModelScope.launch {
                        _isLoadingFlow.value = false
                        _eventsSharedFlow.emit(PairingEvent.ProposalExpired("No proposal or pending session authenticate request for pairing topic: Proposal already consumed"))
                    }
                } else {
                    println(wcEvent.throwable)
                }
            }

            else -> NoAction
        }
    }.shareIn(viewModelScope, SharingStarted.Eagerly)

    fun showLoader(isLoading: Boolean) {
        _isLoadingFlow.value = isLoading
    }

    fun showRequestLoader(isLoading: Boolean) {
        if (_isRequestLoadingFlow.value != isLoading) {
            _isRequestLoadingFlow.value = isLoading
        }
    }

    fun pair(pairingUri: String) {
        _isLoadingFlow.value = true

        if (isPaymentUri(pairingUri)) {
            Log.d("Web3WalletVM", "Detected payment uri: $pairingUri")
            viewModelScope.launch {
                PaymentRepository.clearPayment()
                try {
                    val paymentId = extractPaymentId(pairingUri) ?: throw IllegalArgumentException("Invalid payment link")
                    Log.d("Web3WalletVM", "Extracted paymentId=$paymentId")
                    // Format address as CAIP-10 for Base mainnet (eip155:8453:0x...)
                    val caip10Account = "eip155:8453:${EthAccountDelegate.address}"
                    val paymentSession = withContext(Dispatchers.IO) {
                        PaymentRepository.preparePayment(paymentId, caip10Account)
                    }
                    Log.d("Web3WalletVM", "Prepared payment option=${paymentSession.selectedOption.symbol} chain=${paymentSession.selectedOption.chain} method=${paymentSession.selectedOption.signingRequest?.method}")
                    val requestId = System.currentTimeMillis()
                    val signingRequest = paymentSession.selectedOption.signingRequest
                        ?: throw IllegalStateException("No signing request available for selected option")
                    val sessionRequest = Wallet.Model.SessionRequest(
                        topic = "payment-$paymentId",
                        chainId = paymentSession.selectedOption.chainId,
                        peerMetaData = Core.Model.AppMetaData(
                            name = "WalletConnect Pay",
                            description = "Payment for ${paymentSession.info.amount / 100.0} ${paymentSession.info.currency}",
                            url = "https://walletconnect.com/pay",
                            icons = listOf("https://walletconnect.com/walletconnect-logo.png"),
                            redirect = null,
                        ),
                        request = Wallet.Model.SessionRequest.JSONRPCRequest(
                            id = requestId,
                            method = signingRequest.method,
                            params = paymentSession.typedDataJson ?: signingRequest.params.toString()
                        )
                    )
                    val verifyContext = Wallet.Model.VerifyContext(
                        id = requestId,
                        origin = "https://walletconnect.com/pay",
                        validation = Wallet.Model.Validation.VALID,
                        verifyUrl = "https://walletconnect.com/pay",
                        isScam = false
                    )
                    emitSessionRequest(sessionRequest, verifyContext)
                    Log.d("Web3WalletVM", "Emitted synthetic payment session request id=$requestId")
                    _isLoadingFlow.value = false
                } catch (error: Exception) {
                    Log.e("Web3WalletVM", "Payment flow failed: ${error.message}", error)
                    _isLoadingFlow.value = false
                    _eventsSharedFlow.emit(PairingEvent.Error(error.message ?: "Unexpected error happened, please contact support"))
                }
            }

        } else {
            try {
                val pairingParams = Wallet.Params.Pair(pairingUri.removePrefix("kotlin-web3wallet://wc?uri="))
                WalletKit.pair(pairingParams) { error ->
                    Firebase.crashlytics.recordException(error.throwable)
                    viewModelScope.launch {
                        _isLoadingFlow.value = false
                        _eventsSharedFlow.emit(PairingEvent.Error(error.throwable.message ?: "Unexpected error happened, please contact support"))
                    }
                }
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
                viewModelScope.launch {
                    _isLoadingFlow.value = false
                    _eventsSharedFlow.emit(PairingEvent.Error(e.message ?: "Unexpected error happened, please contact support"))
                }
            }
        }
    }

    private fun extractPaymentId(uri: String): String? {
        return try {
            val parsed = Uri.parse(uri)
            parsed.getQueryParameter("paymentId")
                ?: parsed.getQueryParameter("paymentID")
                ?: if (parsed.host?.contains("gateway-wc.vercel.app", ignoreCase = true) == true) {
                    parsed.pathSegments.lastOrNull { it.isNotBlank() && it != "v1" }
                } else {
                    null
                }
        } catch (e: Exception) {
            null
        }
    }

    private fun isPaymentUri(uri: String): Boolean {
        return uri.contains("paymentId", ignoreCase = true) ||
            uri.contains("gateway-wc.vercel.app", ignoreCase = true)
    }
}
