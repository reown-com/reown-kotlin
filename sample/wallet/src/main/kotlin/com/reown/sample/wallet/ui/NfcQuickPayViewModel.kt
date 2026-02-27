@file:JvmSynthetic

package com.reown.sample.wallet.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.nfc.PaymentSigner
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Auto-pay pipeline ViewModel for NFC quick-pay overlay.
 *
 * Executes the entire payment flow without user interaction:
 * 1. getPaymentOptions() with the payment URL
 * 2. Auto-select first option without collectData
 * 3. getRequiredPaymentActions()
 * 4. Sign all WalletRpc actions
 * 5. confirmPayment()
 * 6. Show success or error
 */
class NfcQuickPayViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<QuickPayState>(QuickPayState.Initializing)
    val uiState: StateFlow<QuickPayState> = _uiState.asStateFlow()

    fun startPayment(paymentUrl: String) {
        viewModelScope.launch {
            try {
                execute(paymentUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Quick pay failed", e)
                _uiState.value = QuickPayState.Error(e.message ?: "Payment failed")
            }
        }
    }

    private suspend fun execute(paymentUrl: String) {
        _uiState.value = QuickPayState.Initializing

        val accounts = listOf(
            "eip155:1:${EthAccountDelegate.address}",
            "eip155:137:${EthAccountDelegate.address}",
            "eip155:8453:${EthAccountDelegate.address}",
            "eip155:10:${EthAccountDelegate.address}"
        )

        // 1. Fetch payment options
        _uiState.value = QuickPayState.FetchingOptions
        val optionsResult = WalletKit.Pay.getPaymentOptions(paymentUrl, accounts)
        val response = optionsResult.getOrElse { error ->
            _uiState.value = QuickPayState.Error(error.message ?: "Failed to get payment options")
            return
        }

        val paymentId = response.paymentId
        val paymentInfo = response.info
        val merchantName = paymentInfo?.merchant?.name
        val amount = paymentInfo?.amount?.let { amt ->
            formatDisplayAmount(
                value = amt.value,
                decimals = amt.display?.decimals ?: 2,
                symbol = amt.display?.assetSymbol ?: extractCurrencyCode(amt.unit)
            )
        }

        // 2. Auto-select first option without collectData
        val selectedOption = response.options.firstOrNull { it.collectData == null }
        if (selectedOption == null) {
            // All options require collectData — fall back to full app
            _uiState.value = QuickPayState.FallbackToFullApp(paymentUrl)
            return
        }

        _uiState.value = QuickPayState.Processing(
            merchantName = merchantName,
            amount = amount,
            optionSummary = buildOptionSummary(selectedOption)
        )

        // 3. Get required payment actions
        val actionsResult = WalletKit.Pay.getRequiredPaymentActions(
            Wallet.Params.RequiredPaymentActions(
                paymentId = paymentId,
                optionId = selectedOption.id
            )
        )
        val actions = actionsResult.getOrElse { error ->
            _uiState.value = QuickPayState.Error(error.message ?: "Failed to get payment actions")
            return
        }

        // 4. Sign all WalletRpc actions
        val walletRpcActions = actions.filterIsInstance<Wallet.Model.RequiredAction.WalletRpc>()
        val signatures = walletRpcActions.map { action ->
            PaymentSigner.signWalletRpcAction(action.action)
        }

        // 5. Confirm payment
        _uiState.value = QuickPayState.Processing(
            merchantName = merchantName,
            amount = amount,
            optionSummary = buildOptionSummary(selectedOption),
            message = "Confirming payment..."
        )

        val confirmResult = WalletKit.Pay.confirmPayment(
            Wallet.Params.ConfirmPayment(
                paymentId = paymentId,
                optionId = selectedOption.id,
                signatures = signatures,
                collectedData = null
            )
        )

        confirmResult.fold(
            onSuccess = { confirmResponse ->
                when (confirmResponse.status) {
                    Wallet.Model.PaymentStatus.SUCCEEDED,
                    Wallet.Model.PaymentStatus.PROCESSING -> {
                        _uiState.value = QuickPayState.Success(
                            merchantName = merchantName,
                            amount = amount,
                            optionSummary = buildOptionSummary(selectedOption)
                        )
                        // Auto-dismiss after 3s
                        delay(3000)
                        _uiState.value = QuickPayState.Done
                    }
                    Wallet.Model.PaymentStatus.FAILED -> {
                        _uiState.value = QuickPayState.Error("Payment failed")
                    }
                    Wallet.Model.PaymentStatus.EXPIRED -> {
                        _uiState.value = QuickPayState.Error("Payment expired")
                    }
                    Wallet.Model.PaymentStatus.REQUIRES_ACTION -> {
                        _uiState.value = QuickPayState.Error("Additional action required")
                    }
                }
            },
            onFailure = { error ->
                _uiState.value = QuickPayState.Error(error.message ?: "Failed to confirm payment")
            }
        )
    }

    private fun buildOptionSummary(option: Wallet.Model.PaymentOption): String {
        val display = option.amount.display
        val symbol = display?.assetSymbol ?: option.amount.unit
        val decimals = display?.decimals ?: 6
        val network = display?.networkName ?: ""
        val formattedAmount = formatTokenAmount(option.amount.value, decimals, symbol)
        return if (network.isNotEmpty()) "$formattedAmount on $network" else formattedAmount
    }

    /** Format fiat display amount: "1" + decimals=2 + "USD" → "$1.00" */
    private fun formatDisplayAmount(value: String, decimals: Int, symbol: String): String {
        return try {
            val raw = BigDecimal(value)
            val divisor = BigDecimal.TEN.pow(decimals.coerceIn(0, 18))
            val formatted = raw.divide(divisor, 2, RoundingMode.HALF_UP)
            val numberFormat = java.text.NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 0
                maximumFractionDigits = 2
            }
            "${getCurrencySymbol(symbol)}${numberFormat.format(formatted)}"
        } catch (_: Exception) {
            "${getCurrencySymbol(symbol)}$value"
        }
    }

    /** Format token amount: "10000" + decimals=6 + "USDC" → "0.01 USDC" */
    private fun formatTokenAmount(value: String, decimals: Int, symbol: String): String {
        return try {
            val raw = BigDecimal(value)
            val divisor = BigDecimal.TEN.pow(decimals.coerceIn(0, 18))
            val formatted = raw.divide(divisor, 4, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString()
            val display = java.text.NumberFormat.getNumberInstance(Locale.US).format(BigDecimal(formatted))
            "$display $symbol"
        } catch (_: Exception) {
            "$value $symbol"
        }
    }

    /** Extract currency code from unit string: "iso4217/USD" → "USD" */
    private fun extractCurrencyCode(unit: String): String {
        return if (unit.contains("/")) unit.substringAfterLast("/") else unit
    }

    private fun getCurrencySymbol(code: String): String = when (code.uppercase()) {
        "USD" -> "$"
        "EUR" -> "\u20AC"
        "GBP" -> "\u00A3"
        "JPY", "CNY" -> "\u00A5"
        "KRW" -> "\u20A9"
        "INR" -> "\u20B9"
        "BRL" -> "R$"
        "CHF" -> "CHF "
        "CAD" -> "CA$"
        "AUD" -> "A$"
        else -> "$code "
    }

    companion object {
        private const val TAG = "NfcQuickPay"
    }
}

sealed class QuickPayState {
    data object Initializing : QuickPayState()
    data object FetchingOptions : QuickPayState()

    data class Processing(
        val merchantName: String? = null,
        val amount: String? = null,
        val optionSummary: String? = null,
        val message: String? = null
    ) : QuickPayState()

    data class Success(
        val merchantName: String? = null,
        val amount: String? = null,
        val optionSummary: String? = null
    ) : QuickPayState()

    data class Error(val message: String) : QuickPayState()

    /** All options require collectData — caller should open full WalletKitActivity */
    data class FallbackToFullApp(val paymentUrl: String) : QuickPayState()

    /** Payment complete, activity should finish */
    data object Done : QuickPayState()
}
