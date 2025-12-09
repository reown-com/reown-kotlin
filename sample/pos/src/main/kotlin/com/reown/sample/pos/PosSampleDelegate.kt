package com.reown.sample.pos

import com.walletconnect.pos.Pos
import com.walletconnect.pos.POSDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object PosSampleDelegate : POSDelegate {
    private val posScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _paymentEventFlow: MutableSharedFlow<Pos.Model.PaymentEvent> = MutableSharedFlow()
    val paymentEventFlow = _paymentEventFlow.asSharedFlow()

    override fun onEvent(event: Pos.Model.PaymentEvent) {
        posScope.launch {
            _paymentEventFlow.emit(event)
        }
    }
}
