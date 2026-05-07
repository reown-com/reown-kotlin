package com.reown.sample.wallet.ui.routes.dialog_routes.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.sample.wallet.domain.WalletKitDelegate
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.nfc.PaymentSigner
import com.reown.sample.wallet.payment.PaymentTransactionUtil
import com.reown.sample.wallet.payment.PaymentUtil
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.Job
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
import org.json.JSONObject

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
    private var pendingWalletRpcActionsKey: RequiredActionsKey? = null
    private val collectedValues: MutableMap<String, String> = mutableMapOf()

    // Race-safe sequencing for background action fetch + gas estimate
    private var paymentActionsRequestSeq: Long = 0
    private var pendingActionsJob: Job? = null

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
        invalidateRequiredActionsState()
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
            fetchPaymentActionsInBackground(option)
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
        invalidateRequiredActionsState()
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
        val schema = collectData?.schema

        if (url != null) {
            clearRequiredActionsCache()
            // WebView-based IC with per-option URL
            val urlWithPrefill = buildUrlWithPrefill(url, schema)
            _uiState.value = PaymentUiState.WebViewDataCollection(
                url = urlWithPrefill,
                paymentInfo = storedPaymentInfo
            )
        } else {
            // No IC required — render Summary immediately, fetch actions in background
            _uiState.value = PaymentUiState.Summary(
                paymentInfo = storedPaymentInfo,
                selectedOption = option
            )
            fetchPaymentActionsInBackground(option)
        }
    }

    /**
     * Fetch required payment actions asynchronously and, if an approval action is
     * present, kick off a gas estimate so the Summary screen can show the
     * one-time fee. A request sequence + Job cancellation guard prevents stale
     * results from leaking into a different option selection.
     */
    private fun fetchPaymentActionsInBackground(option: Wallet.Model.PaymentOption) {
        pendingActionsJob?.cancel()
        val seq = ++paymentActionsRequestSeq
        val paymentId = currentPaymentId ?: return
        val requestKey = RequiredActionsKey(paymentId, option.id)
        clearRequiredActionsCache()
        pendingActionsJob = viewModelScope.launch {
            val result = WalletKit.Pay.getRequiredPaymentActions(
                Wallet.Params.RequiredPaymentActions(
                    paymentId = paymentId,
                    optionId = option.id
                )
            )
            if (!isCurrentRequest(seq, option.id)) return@launch
            result.fold(
                onSuccess = { actions ->
                    pendingWalletRpcActions = actions.filterIsInstance<Wallet.Model.RequiredAction.WalletRpc>()
                    pendingWalletRpcActionsKey = requestKey
                    val ctx = PaymentUtil.getPaymentContext(actions)
                    updateSummary { it.copy(requiresApproval = ctx.requiresApproval) }

                    if (ctx.approvalAction != null) {
                        updateSummary { it.copy(isEstimatingApprovalGas = true) }
                        val estimate = runCatching {
                            PaymentTransactionUtil.estimateApprovalFee(ctx.approvalAction.action)
                        }.getOrNull()
                        if (isCurrentRequest(seq, option.id)) {
                            updateSummary {
                                it.copy(
                                    approvalGasEstimate = estimate,
                                    isEstimatingApprovalGas = false
                                )
                            }
                        }
                    }
                },
                onFailure = { error ->
                    clearRequiredActionsCache()
                    Log.w("PaymentViewModel", "Background action fetch failed: ${error.message}")
                    // Don't surface the error yet — confirmFromSummary will retry and
                    // show an Error state if the fetch still fails at confirmation time.
                }
            )
        }
    }

    private fun isCurrentRequest(seq: Long, optionId: String): Boolean =
        seq == paymentActionsRequestSeq && selectedOptionId == optionId

    private inline fun updateSummary(transform: (PaymentUiState.Summary) -> PaymentUiState.Summary) {
        val current = _uiState.value as? PaymentUiState.Summary ?: return
        _uiState.value = transform(current)
    }

    /**
     * Append prefill query parameter to IC URL if user data is available.
     */
    private fun buildUrlWithPrefill(baseUrl: String, schema: String?): String {
        val prefill = buildPrefillParam(schema) ?: return baseUrl

        val uri = Uri.parse(baseUrl)
        return uri.buildUpon()
            .appendQueryParameter("prefill", prefill)
            .build()
            .toString()
    }

    /**
     * Build the prefill query parameter for IC WebView URL.
     * Parses the schema to find all required fields (top-level + anyOf conditions)
     * and only includes fields that are required.
     */
    private fun buildPrefillParam(schema: String?): String? {
        if (schema == null) return null

        return try {
            val schemaJson = JSONObject(schema)

            // Collect required fields from top-level "required" array
            val requiredFields = mutableSetOf<String>()
            val topRequired = schemaJson.optJSONArray("required")
            if (topRequired != null) {
                for (i in 0 until topRequired.length()) {
                    requiredFields.add(topRequired.getString(i))
                }
            }

            // Collect required fields from "anyOf" conditional groups
            val anyOfArray = schemaJson.optJSONArray("anyOf")
            if (anyOfArray != null) {
                for (i in 0 until anyOfArray.length()) {
                    val group = anyOfArray.getJSONObject(i)
                    val groupRequired = group.optJSONArray("required")
                    if (groupRequired != null) {
                        for (j in 0 until groupRequired.length()) {
                            requiredFields.add(groupRequired.getString(j))
                        }
                    }
                }
            }

            // Map of field id -> prefill value
            val fieldValues = mapOf(
                "fullName" to EthAccountDelegate.PREFILL_FULL_NAME,
                "dob" to EthAccountDelegate.PREFILL_DOB,
                "pobAddress" to EthAccountDelegate.PREFILL_POB_ADDRESS,
                "pobCountry" to EthAccountDelegate.PREFILL_POB_COUNTRY,
                "porAddress" to EthAccountDelegate.PREFILL_POR_ADDRESS,
                "porCountry" to EthAccountDelegate.PREFILL_POR_COUNTRY
            )

            // Build prefill JSON with only required fields
            val prefillData = JSONObject()
            for (fieldId in requiredFields) {
                fieldValues[fieldId]?.let { prefillData.put(fieldId, it) }
            }

            if (prefillData.length() == 0) return null

            val encoded = Base64.encodeToString(
                prefillData.toString().toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP or Base64.URL_SAFE
            )

            Log.d("PaymentViewModel", "Built prefill param for ${requiredFields.size} required field(s)")
            encoded
        } catch (e: Exception) {
            Log.e("PaymentViewModel", "Failed to build prefill param", e)
            null
        }
    }

    /**
     * Process payment with the selected option. If a background action fetch was
     * already kicked off (from `onOptionSelected` / `onICWebViewComplete`), we
     * wait for it to finish so the Summary's gas estimate and `pendingWalletRpcActions`
     * are already populated. Otherwise we fetch actions synchronously here.
     */
    fun processPayment(optionId: String) {
        val paymentId = currentPaymentId ?: return
        val requestKey = RequiredActionsKey(paymentId, optionId)
        selectedOptionId = optionId
        _uiState.value = PaymentUiState.Processing(
            message = "Getting required actions...",
            paymentInfo = storedPaymentInfo
        )

        viewModelScope.launch {
            // If a background fetch is in flight, wait for it before executing.
            pendingActionsJob?.join()

            if (pendingWalletRpcActionsKey == requestKey) {
                executePayment()
                return@launch
            }

            val actionsResult = WalletKit.Pay.getRequiredPaymentActions(
                Wallet.Params.RequiredPaymentActions(
                    paymentId = paymentId,
                    optionId = optionId
                )
            )
            actionsResult.fold(
                onSuccess = { actions ->
                    pendingWalletRpcActions = actions.filterIsInstance<Wallet.Model.RequiredAction.WalletRpc>()
                    pendingWalletRpcActionsKey = requestKey
                    executePayment()
                },
                onFailure = { error ->
                    clearRequiredActionsCache()
                    _uiState.value = PaymentUiState.Error(
                        error.message ?: "Failed to get payment actions",
                        categorizeError(error.message)
                    )
                }
            )
        }
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
        fetchPaymentActionsInBackground(option)
    }

    /**
     * Called when WebView signals IC_ERROR.
     */
    fun onICWebViewError(errorMessage: String) {
        _uiState.value = PaymentUiState.Error("Information capture failed: $errorMessage", categorizeError(errorMessage))
    }

    /**
     * Confirm payment from the Summary screen. Runs a local expiry guard with a
     * 10s safety margin so we don't racily submit an effectively-expired payment.
     */
    fun confirmFromSummary() {
        val optionId = selectedOptionId ?: return
        if (isPaymentExpiredLocally()) {
            _uiState.value = PaymentUiState.Error("Payment expired", PaymentErrorType.EXPIRED)
            return
        }
        processPayment(optionId)
    }

    private fun isPaymentExpiredLocally(): Boolean {
        val expiresAtSeconds = storedPaymentInfo?.expiresAt ?: return false
        val expiresAtMs = expiresAtSeconds * 1000L
        return expiresAtMs <= System.currentTimeMillis() + PAY_EXPIRY_GUARD_MS
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
     *
     * Iterates `pendingWalletRpcActions` in order and dispatches each one:
     *  - `eth_sendTransaction` → broadcast via `PaymentTransactionUtil` with fresh
     *    gas fees, then wait for confirmation. Not added to `signatures`.
     *  - `eth_signTypedData_*` / `personal_sign` → signed via `PaymentSigner`
     *    and appended to `signatures`.
     * Only typed-data signatures are passed to `confirmPayment`.
     */
    private suspend fun executePayment() {
        val paymentId = currentPaymentId ?: return
        val optionId = selectedOptionId ?: return
        val symbol = storedPaymentOptions.find { it.id == optionId }?.amount?.display?.assetSymbol ?: "token"

        _uiState.value = PaymentUiState.Processing(
            message = "Confirming your payment...",
            paymentInfo = storedPaymentInfo
        )

        try {
            val signatures = mutableListOf<String>()
            for (action in pendingWalletRpcActions) {
                when (action.action.method) {
                    ETH_SEND_TRANSACTION -> {
                        _uiState.value = PaymentUiState.Processing(
                            message = "Setting up $symbol for the first time...",
                            paymentInfo = storedPaymentInfo
                        )
                        val txHash = PaymentTransactionUtil.sendTransactionWithFreshFees(action.action)
                        Log.d("PaymentViewModel", "Approval tx broadcast: $txHash")
                        PaymentTransactionUtil.waitForTransactionConfirmation(action.action.chainId, txHash)
                        signatures.add(txHash)
                        _uiState.value = PaymentUiState.Processing(
                            message = "Finalizing your payment...",
                            paymentInfo = storedPaymentInfo
                        )
                    }
                    else -> {
                        signatures.add(PaymentSigner.signWalletRpcAction(action.action))
                    }
                }
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
        invalidateRequiredActionsState()
        currentPaymentLink = null
        currentPaymentId = null
        selectedOptionId = null
        storedPaymentInfo = null
        storedPaymentOptions = emptyList()
        collectedValues.clear()
        // Don't set state to Loading here - we're navigating away anyway
        // and it causes a brief flash of the loading screen
        // Clear replay cache to prevent stale data on next payment
        WalletKitDelegate.clearPaymentOptions()
    }

    private companion object {
        private const val PAY_EXPIRY_GUARD_MS = 10_000L
        private const val ETH_SEND_TRANSACTION = "eth_sendTransaction"
    }

    private data class RequiredActionsKey(
        val paymentId: String,
        val optionId: String,
    )

    private fun invalidateRequiredActionsState() {
        pendingActionsJob?.cancel()
        pendingActionsJob = null
        paymentActionsRequestSeq++
        clearRequiredActionsCache()
    }

    private fun clearRequiredActionsCache() {
        pendingWalletRpcActions = emptyList()
        pendingWalletRpcActionsKey = null
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
        val selectedOption: Wallet.Model.PaymentOption,
        val requiresApproval: Boolean = false,
        val approvalGasEstimate: String? = null,
        val isEstimatingApprovalGas: Boolean = false
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
