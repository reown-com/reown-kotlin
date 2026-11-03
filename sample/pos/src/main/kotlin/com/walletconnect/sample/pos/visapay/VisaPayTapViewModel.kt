package com.walletconnect.sample.pos.visapay

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletconnect.sample.pos.log.PosLogStore
import com.walletconnect.sample.pos.model.Currency
import com.walletconnect.sample.pos.model.formatAmountWithSymbol
import com.walletconnect.sample.pos.nfc.NfcManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

internal data class VisaPayTapState(
    val displayAmount: String,
    val reference: String,
    val stage: VisaPayTapStage = VisaPayTapStage.WAITING_FOR_TAP,
    val isNfcAvailable: Boolean = true,
)

internal class VisaPayTapViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val amountMinor: Long = savedStateHandle.get<String>(ARG_AMOUNT)?.toLongOrNull() ?: 0L
    private val currency: Currency = Currency.fromCode(savedStateHandle.get<String>(ARG_CURRENCY) ?: Currency.USD.code)
    private val amountMajor: String = String.format(Locale.US, "%.2f", amountMinor / 100.0)
    private val reference = "ORDER-${System.currentTimeMillis()}"

    private val _state = MutableStateFlow(
        VisaPayTapState(
            displayAmount = formatAmountWithSymbol(amountMajor, currency),
            reference = reference,
            isNfcAvailable = NfcManager.isAvailable,
        )
    )
    val state: StateFlow<VisaPayTapState> = _state.asStateFlow()

    init {
        val link = VisaPayTapLink.build(reference, amountMajor, currency.code, _state.value.displayAmount)
        NfcManager.updatePaymentUri(link)
        PosLogStore.info("VisaPay tap to pay ready", source = "VisaPayTap", data = link)
        viewModelScope.launch {
            NfcManager.tapEventFlow.collect { onTapped() }
        }
    }

    /** Debug-only shortcut for devices without NFC: behaves exactly like a real tap. */
    fun simulateTap() = onTapped()

    private fun onTapped() {
        if (_state.value.stage != VisaPayTapStage.WAITING_FOR_TAP) return
        // The link has been read; stop serving it so a second tap can't restart the payment.
        NfcManager.clearPaymentUri()
        PosLogStore.info("VisaPay tap received, processing (mocked)", source = "VisaPayTap", data = reference)
        viewModelScope.launch {
            VisaPayTapStage.processingStages.forEach { stage ->
                _state.update { it.copy(stage = stage) }
                delay(stage.durationMs)
            }
            _state.update { it.copy(stage = VisaPayTapStage.APPROVED) }
            PosLogStore.info("VisaPay payment approved (mocked)", source = "VisaPayTap", data = reference)
        }
    }

    override fun onCleared() {
        NfcManager.clearPaymentUri()
    }

    companion object {
        const val ARG_AMOUNT = "amount"
        const val ARG_CURRENCY = "currency"
    }
}
