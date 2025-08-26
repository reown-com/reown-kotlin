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
        POSClient.setDelegate(object : POSClient.POSDelegate {
            override fun onEvent(event: PaymentEvent) {
                println("kobe: Event: $event")

                when (event) {
                    is PaymentEvent.QrReady -> {
                        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.QrReady(uri = event.uri)) }
                    }

                    is PaymentEvent.Connected -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.Connected) }
                    }

                    is PaymentEvent.ConnectionFailed -> {
                        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToErrorScreen(error = event.error.message ?: "Connection Error")) }
                    }

                    is PaymentEvent.PaymentRequested -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentRequested) }
                    }

                    is PaymentEvent.PaymentBroadcasted -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentBroadcasted) }
                    }

                    is PaymentEvent.PaymentSuccessful -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentSuccessful(event.txHash)) }
                    }

                    PaymentEvent.ConnectedRejected -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.ConnectedRejected) }
                    }

                    is PaymentEvent.Error -> {
                        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToErrorScreen(error = event.error.message ?: "Connection Error")) }
                    }

                    is PaymentEvent.PaymentRejected -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentRejected(error = event.message)) }
                    }
                }
            }
        })
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

    fun createPaymentIntent(network: String) {
        this.network = network
        val paymentIntents =
            listOf(
                POS.Model.PaymentIntent(
                    chainId = network,
                    amount = amount ?: "",
                    token = token ?: "",
                    recipient = "${network}:0x228311b83dAF3FC9a0D0a46c0B329942fc8Cb2eD"
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
