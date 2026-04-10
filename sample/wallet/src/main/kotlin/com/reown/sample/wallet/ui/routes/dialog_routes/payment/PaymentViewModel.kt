package com.reown.sample.wallet.ui.routes.dialog_routes.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.sample.wallet.domain.WalletKitDelegate
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.signer.EthSigner
import com.reown.util.bytesToHex
import com.reown.util.hexToBytes
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.reown.sample.wallet.ui.routes.dialog_routes.payment.PaymentUiState.*
import org.json.JSONArray
import org.json.JSONObject
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.crypto.StructuredDataEncoder

/**
 * ViewModel for handling the payment flow.
 * Collects wallet events and processes payment options when received via onPaymentRequest callback.
 */
class PaymentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Loading)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private var currentPaymentLink: String? = null
    private var currentPaymentId: String? = null
    private var selectedOptionId: String? = null

    // Stored payment options for after information capture
    private var storedPaymentInfo: Wallet.Model.PaymentInfo? = null
    private var storedPaymentOptions: List<Wallet.Model.PaymentOption> = emptyList()

    // Information Capture state
    private var pendingWalletRpcActions: List<Wallet.Model.RequiredAction.WalletRpc> = emptyList()
    private val collectedValues: MutableMap<String, String> = mutableMapOf()

    init {
        // Collect payment options event (has replay=1 to ensure we receive it even if emitted before collecting)
        WalletKitDelegate.paymentOptionsEvent
            .onEach { response -> processPaymentOptionsResponse(response) }
            .launchIn(viewModelScope)
    }

    /**
     * Process payment options response received from onPaymentRequest callback.
     * Always goes directly to Options state (no Intro screen).
     */
    private fun processPaymentOptionsResponse(response: Wallet.Model.PaymentOptionsResponse) {
        // Clear replay cache immediately after consuming to prevent stale data leaking to other ViewModel instances
        WalletKitDelegate.clearPaymentOptions()
        currentPaymentId = response.paymentId
        collectedValues.clear()

        // Store payment options for later use
        storedPaymentInfo = response.info
        storedPaymentOptions = response.options

        // Check payment status from info before showing options
        when (response.info?.status) {
            Wallet.Model.PaymentStatus.EXPIRED -> {
                _uiState.value = PaymentUiState.Error("Payment expired", PaymentErrorType.EXPIRED)
                return
            }
            Wallet.Model.PaymentStatus.CANCELLED -> {
                _uiState.value = PaymentUiState.Error("Payment was cancelled", PaymentErrorType.CANCELLED)
                return
            }
            else -> { /* proceed normally */ }
        }

        if (response.options.isEmpty()) {
            _uiState.value = PaymentUiState.Error("No payment options available", PaymentErrorType.INSUFFICIENT_FUNDS)
        } else if (response.options.size == 1 && response.options[0].collectData == null) {
            // Single option with no IC required — skip directly to Summary
            val option = response.options[0]
            selectedOptionId = option.id
            _uiState.value = PaymentUiState.Summary(
                paymentInfo = storedPaymentInfo,
                selectedOption = option
            )
        } else {
            _uiState.value = PaymentUiState.Options(
                paymentLink = currentPaymentLink ?: "",
                paymentInfo = storedPaymentInfo,
                options = storedPaymentOptions
            )
        }
    }

    /**
     * Set the current payment link and fetch payment options.
     */
    fun setPaymentLink(paymentLink: String) {
        if (currentPaymentLink == paymentLink) return
        currentPaymentLink = paymentLink
        _uiState.value = PaymentUiState.Loading
        fetchPaymentOptions(paymentLink)
    }

    private fun fetchPaymentOptions(paymentLink: String) {
        viewModelScope.launch {
            val accounts = listOf(
                "eip155:1:${EthAccountDelegate.address}",
                "eip155:137:${EthAccountDelegate.address}",
                "eip155:8453:${EthAccountDelegate.address}",
                "eip155:10:${EthAccountDelegate.address}"
            )
            val result = WalletKit.Pay.getPaymentOptions(paymentLink, accounts)
            result.fold(
                onSuccess = { response -> processPaymentOptionsResponse(response) },
                onFailure = { error ->
                    _uiState.value = PaymentUiState.Error(
                        error.message ?: "Failed to load payment options",
                        categorizeError(error.message)
                    )
                }
            )
        }
    }

    /**
     * Called when user taps "Pay" or "Continue" on a selected option.
     * If the option has collectData with a URL, show WebView.
     * If it has collectData with fields only, show field-by-field collection.
     * Otherwise, proceed directly to payment processing.
     */
    fun onOptionSelected(optionId: String) {
        val option = storedPaymentOptions.find { it.id == optionId } ?: return
        selectedOptionId = optionId

        val collectData = option.collectData
        val url = collectData?.url

        if (url != null) {
            // WebView-based IC with per-option URL
            val urlWithPrefill = buildUrlWithPrefill(url)
            _uiState.value = PaymentUiState.WebViewDataCollection(
                url = urlWithPrefill,
                paymentInfo = storedPaymentInfo
            )
        } else {
            // No IC required, show review/summary screen
            _uiState.value = PaymentUiState.Summary(
                paymentInfo = storedPaymentInfo,
                selectedOption = option
            )
        }
    }

    /**
     * Append prefill query parameter to IC URL if user data is available.
     */
    private fun buildUrlWithPrefill(baseUrl: String): String {
        val prefill = buildPrefillParam() ?: return baseUrl

        val uri = Uri.parse(baseUrl)
        return uri.buildUpon()
            .appendQueryParameter("prefill", prefill)
            .build()
            .toString()
    }

    /**
     * Build the prefill query parameter for IC WebView URL.
     * Creates Base64-encoded JSON with all available user data.
     * Sends all known fields unconditionally — the webview will use what it needs.
     */
    private fun buildPrefillParam(): String? {
        return try {
            val prefillData = JSONObject().apply {
                put("fullName", EthAccountDelegate.PREFILL_FULL_NAME)
                put("dob", EthAccountDelegate.PREFILL_DOB)
                put("pobAddress", EthAccountDelegate.PREFILL_POB_ADDRESS)
            }

            // Base64 encode the JSON (NO_WRAP avoids newlines, URL_SAFE for URL compatibility)
            val encoded = Base64.encodeToString(
                prefillData.toString().toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP or Base64.URL_SAFE
            )

            Log.d("PaymentViewModel", "Built prefill param: $prefillData -> $encoded")
            encoded
        } catch (e: Exception) {
            Log.e("PaymentViewModel", "Failed to build prefill param", e)
            null
        }
    }

    /**
     * Process payment with the selected option.
     * Uses WalletKit.Pay for getting required actions.
     */
    fun processPayment(optionId: String) {
        val paymentId = currentPaymentId ?: return
        selectedOptionId = optionId
        _uiState.value = PaymentUiState.Processing("Getting required actions...")

        viewModelScope.launch {
            // Get required payment actions using WalletKit.Pay
            val actionsResult = WalletKit.Pay.getRequiredPaymentActions(
                Wallet.Params.RequiredPaymentActions(
                    paymentId = paymentId,
                    optionId = optionId
                )
            )
            actionsResult.fold(
                onSuccess = { actions ->
                    processActions(paymentId, optionId, actions)
                },
                onFailure = { error ->
                    _uiState.value = PaymentUiState.Error(
                        error.message ?: "Failed to get payment actions",
                        categorizeError(error.message)
                    )
                }
            )
        }
    }

    private suspend fun processActions(
        paymentId: String,
        optionId: String,
        actions: List<Wallet.Model.RequiredAction>
    ) {
        // Store WalletRpc actions for signing
        pendingWalletRpcActions = actions.filterIsInstance<Wallet.Model.RequiredAction.WalletRpc>()

        // Information capture already done before options, proceed directly to payment
        executePayment()
    }

    /**
     * Go back to the options screen.
     */
    fun goBackToOptions() {
        _uiState.value = PaymentUiState.Options(
            paymentLink = currentPaymentLink ?: "",
            paymentInfo = storedPaymentInfo,
            options = storedPaymentOptions
        )
    }

    /**
     * Called when WebView signals IC_COMPLETE.
     * Shows Summary screen instead of going back to options.
     */
    fun onICWebViewComplete() {
        val optionId = selectedOptionId ?: return
        val option = storedPaymentOptions.find { it.id == optionId } ?: return
        _uiState.value = PaymentUiState.Summary(
            paymentInfo = storedPaymentInfo,
            selectedOption = option
        )
    }

    /**
     * Called when WebView signals IC_ERROR.
     */
    fun onICWebViewError(errorMessage: String) {
        _uiState.value = PaymentUiState.Error("Information capture failed: $errorMessage", categorizeError(errorMessage))
    }

    /**
     * Confirm payment from the Summary screen.
     */
    fun confirmFromSummary() {
        val optionId = selectedOptionId ?: return
        processPayment(optionId)
    }

    /**
     * Show the "Why info required?" explanation screen.
     */
    fun showWhyInfoRequired() {
        _uiState.value = PaymentUiState.WhyInfoRequired(
            paymentInfo = storedPaymentInfo
        )
    }

    /**
     * Dismiss the "Why info required?" screen and return to options.
     */
    fun dismissWhyInfoRequired() {
        goBackToOptions()
    }

    /**
     * Execute the payment with collected data and signatures.
     * Uses WalletKit.Pay for confirming payment.
     */
    private suspend fun executePayment() {
        val paymentId = currentPaymentId ?: return
        val optionId = selectedOptionId ?: return

        _uiState.value = PaymentUiState.Processing(
            message = "Confirming your payment...",
            paymentInfo = storedPaymentInfo
        )

        try {
            // Sign all WalletRpc actions and collect signatures
            val signatures = pendingWalletRpcActions.map { action ->
                signWalletRpcAction(action.action)
            }

            // Convert collected data to field results
            val collectedData = if (collectedValues.isNotEmpty()) {
                collectedValues.map { (id, value) ->
                    Wallet.Model.CollectDataFieldResult(id = id, value = value)
                }
            } else {
                null
            }

            // Confirm payment with signatures and collected data using WalletKit.Pay
            val confirmResult = WalletKit.Pay.confirmPayment(
                Wallet.Params.ConfirmPayment(
                    paymentId = paymentId,
                    optionId = optionId,
                    signatures = signatures,
                    collectedData = collectedData
                )
            )

            confirmResult.fold(
                onSuccess = { response ->
                    when (response.status) {
                        Wallet.Model.PaymentStatus.SUCCEEDED -> {
                            Log.d("PaymentViewModel", "Payment SUCCEEDED")
                            _uiState.value = Success(
                                message = "Payment completed successfully!",
                                paymentInfo = storedPaymentInfo,
                                resultInfo = response.info
                            )
                        }
                        Wallet.Model.PaymentStatus.PROCESSING -> {
                            Log.d("PaymentViewModel", "Payment PROCESSING")
                            _uiState.value = Success(
                                message = "Payment is being processed...",
                                paymentInfo = storedPaymentInfo,
                                resultInfo = response.info
                            )
                        }
                        Wallet.Model.PaymentStatus.FAILED -> {
                            _uiState.value = Error("Payment failed", PaymentErrorType.GENERIC)
                        }
                        Wallet.Model.PaymentStatus.EXPIRED -> {
                            _uiState.value = Error("Payment expired", PaymentErrorType.EXPIRED)
                        }
                        Wallet.Model.PaymentStatus.REQUIRES_ACTION -> {
                            _uiState.value = Error("Additional action required", PaymentErrorType.GENERIC)
                        }

                        Wallet.Model.PaymentStatus.CANCELLED -> {
                            _uiState.value = Error("Payment was cancelled", PaymentErrorType.CANCELLED)
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.value = PaymentUiState.Error(
                        error.message ?: "Failed to confirm payment",
                        categorizeError(error.message)
                    )
                }
            )
        } catch (e: Exception) {
            Log.e("PaymentViewModel", "Payment processing failed", e)
            _uiState.value = PaymentUiState.Error(
                e.message ?: "An error occurred during payment",
                categorizeError(e.message)
            )
        }
    }

    /**
     * Sign a wallet RPC action and return the signature string.
     */
    private fun signWalletRpcAction(action: Wallet.Model.WalletRpcAction): String {
        return when (action.method) {
            "eth_signTypedData_v4" -> signTypedDataV4(action.params)
            "personal_sign" -> EthSigner.personalSign(action.params)
            else -> throw UnsupportedOperationException("Unsupported signing method: ${action.method}")
        }
    }

    /**
     * Sign EIP-712 typed data using proper StructuredDataEncoder.
     */
    private fun signTypedDataV4(params: String): String {
        // params is a JSON array: [address, typedData]
        val paramsArray = JSONArray(params)
        val requestedAddress = paramsArray.getString(0)
        val typedData = paramsArray.getString(1)

        // Verify the requested address matches our wallet address
        if (!requestedAddress.equals(EthAccountDelegate.address, ignoreCase = true)) {
            throw IllegalArgumentException("Requested address does not match wallet address")
        }

        // Use StructuredDataEncoder for proper EIP-712 hashing
        val encoder = StructuredDataEncoder(typedData)
        val hash = encoder.hashStructuredData()

        val keyPair = ECKeyPair.create(EthAccountDelegate.privateKey.hexToBytes())
        val signatureData = Sign.signMessage(hash, keyPair, false)

        val rHex = signatureData.r.bytesToHex()
        val sHex = signatureData.s.bytesToHex()
        val v = signatureData.v[0].toInt() and 0xff
        val vHex = v.toString(16).padStart(2, '0')

        return "0x$rHex$sHex$vHex".lowercase()
    }

    /**
     * Categorize an error message into a PaymentErrorType.
     */
    private fun categorizeError(message: String?): PaymentErrorType {
        val msg = message?.lowercase() ?: return PaymentErrorType.GENERIC
        return when {
            msg.contains("insufficient") || msg.contains("not enough") || msg.contains("balance") -> PaymentErrorType.INSUFFICIENT_FUNDS
            msg.contains("expired") || msg.contains("timeout") -> PaymentErrorType.EXPIRED
            msg.contains("cancelled") || msg.contains("canceled") -> PaymentErrorType.CANCELLED
            msg.contains("not found") || msg.contains("404") -> PaymentErrorType.NOT_FOUND
            else -> PaymentErrorType.GENERIC
        }
    }

    /**
     * Cancel and reset the payment flow.
     */
    fun cancel() {
        currentPaymentLink = null
        currentPaymentId = null
        selectedOptionId = null
        storedPaymentInfo = null
        storedPaymentOptions = emptyList()
        pendingWalletRpcActions = emptyList()
        collectedValues.clear()
        // Don't set state to Loading here - we're navigating away anyway
        // and it causes a brief flash of the loading screen
        // Clear replay cache to prevent stale data on next payment
        WalletKitDelegate.clearPaymentOptions()
    }
}

/**
 * UI state for the payment flow.
 * Uses Wallet.Model types from WalletKit.
 */
sealed class PaymentUiState {
    data object Loading : PaymentUiState()

    /**
     * WebView-based Information Capture (replaces CollectingData when URL is available).
     */
    data class WebViewDataCollection(
        val url: String,
        val paymentInfo: Wallet.Model.PaymentInfo?
    ) : PaymentUiState()

    data class Options(
        val paymentLink: String,
        val paymentInfo: Wallet.Model.PaymentInfo?,
        val options: List<Wallet.Model.PaymentOption>
    ) : PaymentUiState()

    /**
     * Summary screen shown after IC completion, before confirming payment.
     */
    data class Summary(
        val paymentInfo: Wallet.Model.PaymentInfo?,
        val selectedOption: Wallet.Model.PaymentOption
    ) : PaymentUiState()

    /**
     * Explanation dialog for why information is required.
     */
    data class WhyInfoRequired(
        val paymentInfo: Wallet.Model.PaymentInfo?
    ) : PaymentUiState()

    data class Processing(
        val message: String,
        val paymentInfo: Wallet.Model.PaymentInfo? = null
    ) : PaymentUiState()

    data class Success(
        val message: String,
        val paymentInfo: Wallet.Model.PaymentInfo? = null,
        val resultInfo: Wallet.Model.PaymentResultInfo? = null
    ) : PaymentUiState()

    data class Error(
        val message: String,
        val errorType: PaymentErrorType = PaymentErrorType.GENERIC
    ) : PaymentUiState()
}

/**
 * Categorized error types for payment failures.
 */
enum class PaymentErrorType {
    INSUFFICIENT_FUNDS,
    EXPIRED,
    CANCELLED,
    NOT_FOUND,
    GENERIC
}
