package com.reown.sample.wallet.ui.routes.composable_routes.visapay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Mocked stages of a card payment. Their durations add up to [VisaPayPaymentViewModel.PROCESSING_DURATION_MS]. */
enum class VisaPayPaymentStage(val title: String, val durationMs: Long) {
    READING_REQUEST("Reading payment request", 1_000L),
    AUTHORIZING("Authorizing with Visa", 1_500L),
    PULLING_FUNDS("Collecting USDC via Permit2", 1_500L),
    SETTLING("Settling with the merchant", 1_000L),
    APPROVED("Payment approved", 0L),
}

data class VisaPayPaymentState(
    val request: VisaPayPaymentRequest,
    val card: VisaPayCard?,
    val stage: VisaPayPaymentStage = VisaPayPaymentStage.READING_REQUEST,
) {
    val isApproved: Boolean get() = stage == VisaPayPaymentStage.APPROVED
}

class VisaPayPaymentViewModel : ViewModel() {
    private val _state = MutableStateFlow(
        VisaPayPaymentState(
            request = VisaPayMockRepository.createPaymentRequest(),
            card = VisaPayMockRepository.card.value,
        )
    )
    val state: StateFlow<VisaPayPaymentState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            VisaPayPaymentStage.entries
                .filter { it != VisaPayPaymentStage.APPROVED }
                .forEach { stage ->
                    _state.update { it.copy(stage = stage) }
                    delay(stage.durationMs)
                }
            VisaPayMockRepository.recordPayment(_state.value.request)
            _state.update { it.copy(stage = VisaPayPaymentStage.APPROVED, card = VisaPayMockRepository.card.value) }
        }
    }

    companion object {
        val PROCESSING_DURATION_MS: Int = VisaPayPaymentStage.entries.sumOf { it.durationMs }.toInt()
    }
}
