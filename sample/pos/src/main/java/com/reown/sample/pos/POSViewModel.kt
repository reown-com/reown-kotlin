package com.reown.sample.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.pos.client.POS
import com.reown.pos.client.POSClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class POSViewModel : ViewModel() {

    private val _posEventsFlow: MutableSharedFlow<POS.Model.PaymentEvent> = MutableSharedFlow()
    val posEventsFlow = _posEventsFlow.asSharedFlow()

    init {
        POSClient.setDelegate(object : POSClient.POSDelegate {
            override fun onEvent(event: POS.Model.PaymentEvent) {

                viewModelScope.launch { _posEventsFlow.emit(event) }

                when (event) {
                    is POS.Model.PaymentEvent.QrReady -> {

                    }

                    is POS.Model.PaymentEvent.Connected -> {

                    }

                    is POS.Model.PaymentEvent.ConnectionFailed -> {

                    }

                    is POS.Model.PaymentEvent.PaymentRequested -> {

                    }

                    is POS.Model.PaymentEvent.PaymentBroadcasted -> {

                    }

                    is POS.Model.PaymentEvent.PaymentSuccessful -> {
                    }

                    POS.Model.PaymentEvent.ConnectedRejected -> {

                    }

                    is POS.Model.PaymentEvent.Error -> {

                    }

                    is POS.Model.PaymentEvent.PaymentRejected -> {

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
