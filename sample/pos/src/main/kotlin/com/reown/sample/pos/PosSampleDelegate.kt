package com.reown.sample.pos

import androidx.lifecycle.viewModelScope
import com.reown.pos.client.POS
import com.reown.pos.client.POS.Model.PaymentEvent
import com.reown.pos.client.POSClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object PosSampleDelegate : POSClient.POSDelegate {
    private val posScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _paymentEventFlow: MutableSharedFlow<PaymentEvent> = MutableSharedFlow()
    val paymentEventFlow = _paymentEventFlow.asSharedFlow()

    override fun onEvent(event: PaymentEvent) {
        posScope.launch {
            _paymentEventFlow.emit(event)
        }
    }
}

