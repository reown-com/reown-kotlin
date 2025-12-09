package com.reown.sample.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletconnect.pos.Pos
import com.walletconnect.pos.PosClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URI

sealed interface PosNavEvent {
    data object ToStart : PosNavEvent
    data object ToAmount : PosNavEvent
    data object FlowFinished : PosNavEvent
    data class QrReady(val uri: URI, val amount: Pos.Model.Amount, val paymentId: String) : PosNavEvent
    data class ToErrorScreen(val error: String) : PosNavEvent
    data class PaymentSuccessScreen(val paymentId: String) : PosNavEvent
}

sealed interface PosEvent {
    data object PaymentRequested : PosEvent
    data object PaymentProcessing : PosEvent
    data class PaymentSuccess(val paymentId: String) : PosEvent
    data class PaymentError(val error: String) : PosEvent
}

class POSViewModel : ViewModel() {

    private val _posNavEventsFlow: MutableSharedFlow<PosNavEvent> = MutableSharedFlow()
    val posNavEventsFlow = _posNavEventsFlow.asSharedFlow()

    private val _posEventsFlow: MutableSharedFlow<PosEvent> = MutableSharedFlow()
    val posEventsFlow = _posEventsFlow.asSharedFlow()

    // Current payment info
    private var currentAmount: Pos.Model.Amount? = null
    private var currentPaymentId: String? = null

    // Loading state for "Start Payment" button
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            PosSampleDelegate.paymentEventFlow.collect { paymentEvent ->
                handlePaymentEvent(paymentEvent)
            }
        }
    }

    private suspend fun handlePaymentEvent(paymentEvent: Pos.Model.PaymentEvent) {
        when (paymentEvent) {
            is Pos.Model.PaymentEvent.PaymentCreated -> {
                _isLoading.value = false
                currentAmount = paymentEvent.amount
                currentPaymentId = paymentEvent.paymentId
                _posNavEventsFlow.emit(
                    PosNavEvent.QrReady(
                        uri = paymentEvent.uri,
                        amount = paymentEvent.amount,
                        paymentId = paymentEvent.paymentId
                    )
                )
            }

            is Pos.Model.PaymentEvent.PaymentRequested -> {
                _posEventsFlow.emit(PosEvent.PaymentRequested)
            }

            is Pos.Model.PaymentEvent.PaymentProcessing -> {
                _posEventsFlow.emit(PosEvent.PaymentProcessing)
            }

            is Pos.Model.PaymentEvent.PaymentSuccess -> {
                _posEventsFlow.emit(PosEvent.PaymentSuccess(paymentEvent.paymentId))
                _posNavEventsFlow.emit(PosNavEvent.PaymentSuccessScreen(paymentEvent.paymentId))
            }

            is Pos.Model.PaymentEvent.PaymentError -> {
                _isLoading.value = false
                val errorMessage = when (val error = paymentEvent.error) {
                    is Pos.Model.PaymentError.CreatePaymentFailed -> "Failed to create payment: ${error.message}"
                    is Pos.Model.PaymentError.PaymentFailed -> "Payment failed: ${error.message}"
                    is Pos.Model.PaymentError.PaymentNotFound -> "Payment not found: ${error.message}"
                    is Pos.Model.PaymentError.PaymentExpired -> "Payment expired: ${error.message}"
                    is Pos.Model.PaymentError.InvalidPaymentRequest -> "Invalid request: ${error.message}"
                    is Pos.Model.PaymentError.Generic -> "Error: ${error.message}"
                }
                _posEventsFlow.emit(PosEvent.PaymentError(errorMessage))
                _posNavEventsFlow.emit(PosNavEvent.ToErrorScreen(error = errorMessage))
            }
        }
    }

    fun navigateToAmountScreen() {
        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToAmount) }
    }

    /**
     * Creates a payment intent with the specified amount.
     *
     * @param amountValue Amount in minor units (cents for USD)
     * @param currency Currency code (e.g., "USD", "EUR")
     */
    fun createPayment(amountValue: String, currency: String = "USD") {
        try {
            val referenceId = "ORDER-${System.currentTimeMillis()}"
            _isLoading.value = true

            PosClient.createPaymentIntent(
                amount = Pos.Model.Amount(
                    unit = "iso4217/$currency",
                    value = amountValue
                ),
                referenceId = referenceId
            )
        } catch (e: Exception) {
            _isLoading.value = false
            viewModelScope.launch {
                _posNavEventsFlow.emit(
                    PosNavEvent.ToErrorScreen(error = e.message ?: "Create payment error")
                )
            }
        }
    }

    fun cancelPayment() {
        PosClient.cancelPayment()
        _isLoading.value = false
    }

    fun resetForNewPayment() {
        currentAmount = null
        currentPaymentId = null
        _isLoading.value = false
    }

    fun getDisplayAmount(): String {
        val amount = currentAmount ?: return ""
        val valueInCents = amount.value.toLongOrNull() ?: 0L
        val dollars = valueInCents / 100.0
        val currency = amount.unit.substringAfter("/", "USD")
        return String.format("$%.2f %s", dollars, currency)
    }
}
