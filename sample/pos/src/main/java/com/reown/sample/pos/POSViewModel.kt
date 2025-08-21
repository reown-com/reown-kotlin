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
}

sealed interface PosEvent {
    data object Connected : PosEvent
    data object ConnectedRejected : PosEvent
    data class ConnectionFailed(val error: String) : PosEvent
    data object PaymentRequested : PosEvent
    data object PaymentBroadcasted : PosEvent
    data class PaymentRejected(val error: String) : PosEvent
    data class PaymentSuccessful(val txHash: String, val receipt: String) : PosEvent
    data class Error(val error: String) : PosEvent
}

class POSViewModel : ViewModel() {

    private val _posNavEventsFlow: MutableSharedFlow<PosNavEvent> = MutableSharedFlow()
    val posNavEventsFlow = _posNavEventsFlow.asSharedFlow()

    private val _posEventsFlow: MutableSharedFlow<PosEvent> = MutableSharedFlow()
    val posEventsFlow = _posEventsFlow.asSharedFlow()

    init {
        POSClient.setDelegate(object : POSClient.POSDelegate {
            override fun onEvent(event: PaymentEvent) {
                println("kobe: Event: $event")

                when (event) {
                    is PaymentEvent.QrReady -> {
                        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.QrReady(event.uri)) }
                    }

                    is PaymentEvent.Connected -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.Connected) }
                    }

                    is PaymentEvent.ConnectionFailed -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.ConnectionFailed(event.error.message ?: "Connection Error")) }
                    }

                    is PaymentEvent.PaymentRequested -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentRequested) }
                    }

                    is PaymentEvent.PaymentBroadcasted -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentBroadcasted) }
                    }

                    is PaymentEvent.PaymentSuccessful -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentSuccessful(event.txHash, event.receipt)) }
                    }

                    PaymentEvent.ConnectedRejected -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.ConnectedRejected) }
                    }

                    is PaymentEvent.Error -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.Error(event.error.cause?.message ?: "Payment Error")) }
                    }

                    is PaymentEvent.PaymentRejected -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentRejected(event.error.message)) }
                    }
                }
            }
        })
    }

    fun createPaymentIntent(paymentIntents: List<POS.Model.PaymentIntent>) {
        try {
            POSClient.createPaymentIntent(intents = paymentIntents)
        } catch (e: Exception) {
            println("kobe: createPaymentIntent error: ${e.message}")
        }
    }
}
