package com.reown.sample.wallet.ui.routes.composable_routes.visapay

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Stages of a card payment. AUTHORIZING waits for vpay-server's real answer; the others pace the animation.
 * Their durations add up to [VisaPayPaymentViewModel.PROCESSING_DURATION_MS].
 */
enum class VisaPayPaymentStage(val title: String, val durationMs: Long) {
    READING_REQUEST("Reading payment request", 1_000L),
    AUTHORIZING("Authorizing with Visa", 1_500L),
    PULLING_FUNDS("Collecting USDC via Permit2", 1_500L),
    SETTLING("Settling with the merchant", 1_000L),
    APPROVED("Payment approved", 0L),
    DECLINED("Payment declined", 0L);

    val isFinal: Boolean get() = this == APPROVED || this == DECLINED
}

data class VisaPayPaymentState(
    val request: VisaPayPaymentRequest,
    val card: VisaPayCard?,
    val stage: VisaPayPaymentStage = VisaPayPaymentStage.READING_REQUEST,
    /** Visa response code from vpay-server, once known. */
    val responseCode: String? = null,
    /** Why the payment didn't go through: a decline reason or a service error. */
    val failure: String? = null,
) {
    val isApproved: Boolean get() = stage == VisaPayPaymentStage.APPROVED
    val isDeclined: Boolean get() = stage == VisaPayPaymentStage.DECLINED
}

class VisaPayPaymentViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _state = MutableStateFlow(
        VisaPayPaymentState(
            // A tap on the sample POS carries the real merchant and amount; a QR scan picks a demo merchant.
            request = savedStateHandle.get<String>(ARG_TAP_LINK)?.let(VisaPayTapLink::parse)
                ?: VisaPayRepository.createPaymentRequest(),
            card = VisaPayRepository.card.value,
        )
    )
    val state: StateFlow<VisaPayPaymentState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            setStage(VisaPayPaymentStage.READING_REQUEST)
            // Start the real authorization right away; the animation keeps its pace around it.
            val authorization = async { runCatching { VisaPayRepository.pay(_state.value.request) } }
            delay(VisaPayPaymentStage.READING_REQUEST.durationMs)

            setStage(VisaPayPaymentStage.AUTHORIZING)
            delay(VisaPayPaymentStage.AUTHORIZING.durationMs)
            val result = authorization.await()
            val responseCode = result.getOrNull()?.responseCode
            if (responseCode != RESPONSE_APPROVED) {
                val failure = result.exceptionOrNull()?.message ?: declineReason(responseCode.orEmpty())
                _state.update {
                    it.copy(stage = VisaPayPaymentStage.DECLINED, responseCode = responseCode, failure = failure, card = VisaPayRepository.card.value)
                }
                return@launch
            }

            setStage(VisaPayPaymentStage.PULLING_FUNDS)
            delay(VisaPayPaymentStage.PULLING_FUNDS.durationMs)
            setStage(VisaPayPaymentStage.SETTLING)
            delay(VisaPayPaymentStage.SETTLING.durationMs)
            _state.update {
                it.copy(stage = VisaPayPaymentStage.APPROVED, responseCode = responseCode, card = VisaPayRepository.card.value)
            }
        }
    }

    private fun setStage(stage: VisaPayPaymentStage) = _state.update { it.copy(stage = stage) }

    companion object {
        const val ARG_TAP_LINK = "tap"
        val PROCESSING_DURATION_MS: Int = VisaPayPaymentStage.entries.sumOf { it.durationMs }.toInt()
    }
}
