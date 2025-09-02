package com.reown.sample.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.pos.client.POS
import com.reown.pos.client.POS.Model.PaymentEvent
import com.reown.pos.client.POSClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.net.URI

sealed interface PosNavEvent {
    data object ToStart : PosNavEvent
    data object ToAmount : PosNavEvent
    data object ToSelectToken : PosNavEvent
    data object ToSelectNetwork : PosNavEvent
    data object FlowFinished : PosNavEvent
    data class QrReady(val uri: URI) : PosNavEvent

    data class ToErrorScreen(val error: String) : PosNavEvent
}

sealed interface PosEvent {
    data object Connected : PosEvent
    data object ConnectedRejected : PosEvent
    data object PaymentRequested : PosEvent
    data object PaymentBroadcasted : PosEvent
    data class PaymentRejected(val error: String) : PosEvent
    data class PaymentSuccessful(val txHash: String) : PosEvent
}

class POSViewModel : ViewModel() {

    private val _posNavEventsFlow: MutableSharedFlow<PosNavEvent> = MutableSharedFlow()
    val posNavEventsFlow = _posNavEventsFlow.asSharedFlow()

    private val _posEventsFlow: MutableSharedFlow<PosEvent> = MutableSharedFlow()
    val posEventsFlow = _posEventsFlow.asSharedFlow()

    internal var token: String? = null
    internal var amount: String? = null
    internal var network: String? = null


    init {
        viewModelScope.launch {
            PosSampleDelegate.paymentEventFlow.collect { paymentEvent ->
                when (paymentEvent) {
                    is PaymentEvent.QrReady -> {
                        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.QrReady(uri = paymentEvent.uri)) }
                    }

                    is PaymentEvent.Connected -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.Connected) }
                    }

                    is PaymentEvent.ConnectionFailed -> {
                        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToErrorScreen(error = paymentEvent.error.message ?: "Connection Error")) }
                    }

                    is PaymentEvent.PaymentRequested -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentRequested) }
                    }

                    is PaymentEvent.PaymentBroadcasted -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentBroadcasted) }
                    }

                    is PaymentEvent.PaymentSuccessful -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentSuccessful(paymentEvent.txHash)) }
                    }

                    PaymentEvent.ConnectedRejected -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.ConnectedRejected) }
                    }

                    is PaymentEvent.Error -> {
                        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToErrorScreen(error = paymentEvent.error.message ?: "Connection Error")) }
                    }

                    is PaymentEvent.PaymentRejected -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentRejected(error = paymentEvent.message)) }
                    }
                }
            }
        }
    }

    fun navigateToAmountScreen() {
        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToAmount) }
    }

    fun navigateToTokenScreen(amount: String) {
        this.amount = amount
        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToSelectToken) }
    }

    fun navigateToNetworkScreen(token: String) {
        this.token = token
        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToSelectNetwork) }
    }

    fun createPaymentIntent(chainId: String, name: String) {
        this.network = chainId
        val paymentIntents =
            listOf(
                POS.Model.PaymentIntent(
                    token = POS.Model.Token(
                        network = POS.Model.Network(
                            chainId = chainId,
                            name = name
                        ),
                        standard = "erc20", //TODO: add dynamic values from UI
                        symbol = "USDC", //TODO: add dynamic values from UI
                        address = "0x1c7D4B196Cb0C7B01d743Fbc6116a902379C7238" //USDC Sepolia TODO: add dynamic values from UI
                    ),
                    amount = amount ?: "",
                    recipient = "${chainId}:0x228311b83dAF3FC9a0D0a46c0B329942fc8Cb2eD"
                )
            )
        try {
            POSClient.createPaymentIntent(intents = paymentIntents)
        } catch (e: Exception) {
            println("kobe: createPaymentIntent error: ${e.message}")

            viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToErrorScreen(error = e.message ?: "Create intent error")) }
        }
    }
}
