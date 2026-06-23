package com.reown.sample.wallet.ui.routes.dialog_routes.payment

import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.sample.wallet.domain.ThemeManager
import com.reown.sample.wallet.domain.WalletKitDelegate
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.account.SolanaAccountDelegate
import com.reown.sample.wallet.nfc.PaymentSigner
import com.reown.sample.wallet.ui.routes.dialog_routes.transaction.Chain
import com.reown.sample.wallet.payment.InitialPaymentDestination
import com.reown.sample.wallet.payment.PaymentSelectionResolver
import com.reown.sample.wallet.payment.PaymentTokenPreferenceStore
import com.reown.sample.wallet.payment.PaymentTransactionUtil
import com.reown.sample.wallet.payment.TransactionFeeEstimate
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
import org.json.JSONObject

class PaymentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Loading)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private var currentPaymentLink: String? = null
    private var currentPaymentId: String? = null
    private var selectedOptionId: String? = null

    private var storedPaymentInfo: Wallet.Model.PaymentInfo? = null
    private var storedPaymentOptions: List<Wallet.Model.PaymentOption> = emptyList()

    private val collectedValues: MutableMap<String, String> = mutableMapOf()
    private val optionGasEstimates: MutableMap<String, TransactionFeeEstimate?> = mutableMapOf()
    private val estimatingOptionIds: MutableSet<String> = mutableSetOf()
    private var optionGasEstimateRequestSeq: Long = 0
    private var optionGasEstimateJob: Job? = null

    init {
        WalletKitDelegate.paymentOptionsEvent
            .onEach { response -> processPaymentOptionsResponse(response) }
            .launchIn(viewModelScope)
    }

    private fun processPaymentOptionsResponse(response: Wallet.Model.PaymentOptionsResponse) {
        WalletKitDelegate.clearPaymentOptions()
        invalidateTransientState()
        currentPaymentId = response.paymentId
        collectedValues.clear()
        storedPaymentInfo = response.info
        storedPaymentOptions = response.options

        when (response.info?.status) {
            Wallet.Model.PaymentStatus.EXPIRED -> {
                _uiState.value = PaymentUiState.Error("Payment expired", PaymentErrorType.EXPIRED)
                return
            }

            Wallet.Model.PaymentStatus.CANCELLED -> {
                _uiState.value = PaymentUiState.Error("Payment was cancelled", PaymentErrorType.CANCELLED)
                return
            }

            // Non-payable statuses (e.g. re-scanning an already-completed payment). These come
            // back with an empty options list; without handling them here they would fall through
            // to the empty-options branch below and be mislabelled as INSUFFICIENT_FUNDS.
            Wallet.Model.PaymentStatus.SUCCEEDED,
            Wallet.Model.PaymentStatus.PROCESSING,
            Wallet.Model.PaymentStatus.FAILED -> {
                _uiState.value = PaymentUiState.Error("This payment has already been processed", PaymentErrorType.GENERIC)
                return
            }

            else -> Unit
        }

        if (response.options.isEmpty()) {
            _uiState.value = PaymentUiState.Error("No payment options available", PaymentErrorType.INSUFFICIENT_FUNDS)
            return
        }

        preloadOptionGasEstimates(response.options)

        val initialRoute = PaymentSelectionResolver.resolve(
            options = response.options,
            lastPaidTokenUnit = PaymentTokenPreferenceStore.getLastPaidTokenUnit(),
        )

        when (initialRoute.destination) {
            InitialPaymentDestination.OPTIONS -> showOptions()
            InitialPaymentDestination.WEB_VIEW_DATA_COLLECTION -> {
                initialRoute.selectedOption?.let(::openDataCollection)
            }

            InitialPaymentDestination.SUMMARY -> {
                initialRoute.selectedOption?.let(::showSummary)
            }
        }
    }

    fun setPaymentLink(paymentLink: String) {
        if (currentPaymentLink == paymentLink) return
        invalidateTransientState()
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
                "eip155:10:${EthAccountDelegate.address}",
                "${Chain.SOLANA.id}:${SolanaAccountDelegate.getSolanaPubKeyForKeyPair(SolanaAccountDelegate.keyPair)}",
            )

            WalletKit.Pay.getPaymentOptions(paymentLink, accounts).fold(
                onSuccess = { response -> processPaymentOptionsResponse(response) },
                onFailure = { error ->
                    _uiState.value = PaymentUiState.Error(
                        error.message ?: "Failed to load payment options",
                        categorizeError(error.message),
                    )
                },
            )
        }
    }

    fun onOptionSelected(optionId: String) {
        val option = storedPaymentOptions.find { it.id == optionId } ?: return
        if (option.collectData?.url != null) {
            openDataCollection(option)
        } else {
            showSummary(option)
        }
    }

    fun goBackToOptions() {
        showOptions()
    }

    fun onICWebViewComplete() {
        val option = storedPaymentOptions.find { it.id == selectedOptionId } ?: return
        showSummary(option)
    }

    fun onICWebViewError(errorMessage: String) {
        _uiState.value = PaymentUiState.Error(
            "Information capture failed: $errorMessage",
            categorizeError(errorMessage),
        )
    }

    fun confirmFromSummary() {
        val option = storedPaymentOptions.find { it.id == selectedOptionId } ?: return
        if (isPaymentExpiredLocally()) {
            _uiState.value = PaymentUiState.Error("Payment expired", PaymentErrorType.EXPIRED)
            return
        }
        processPayment(option)
    }

    fun showWhyInfoRequired() {
        _uiState.value = PaymentUiState.WhyInfoRequired(paymentInfo = storedPaymentInfo)
    }

    fun dismissWhyInfoRequired() {
        showOptions()
    }

    fun showGasFeeInfo() {
        val current = _uiState.value as? PaymentUiState.Summary ?: return
        _uiState.value = PaymentUiState.GasFeeInfo(
            paymentInfo = current.paymentInfo,
            selectedOption = current.selectedOption,
            approvalGasEstimate = current.approvalGasEstimate,
        )
    }

    fun dismissGasFeeInfo() {
        val current = _uiState.value as? PaymentUiState.GasFeeInfo ?: return
        showSummary(current.selectedOption)
    }

    private fun showOptions() {
        _uiState.value = PaymentUiState.Options(
            paymentLink = currentPaymentLink ?: "",
            paymentInfo = storedPaymentInfo,
            options = storedPaymentOptions,
            selectedOptionId = selectedOptionId,
            gasEstimates = optionGasEstimates.toMap(),
            estimatingOptionIds = estimatingOptionIds.toSet(),
        )
    }

    private fun showSummary(option: Wallet.Model.PaymentOption) {
        selectedOptionId = option.id
        _uiState.value = PaymentUiState.Summary(
            paymentInfo = storedPaymentInfo,
            selectedOption = option,
            requiresApproval = PaymentUtil.requiresApproval(option.actions),
            approvalGasEstimate = optionGasEstimates[option.id],
            isEstimatingApprovalGas = estimatingOptionIds.contains(option.id),
            canChangeOption = storedPaymentOptions.size > 1,
        )
    }

    private fun openDataCollection(option: Wallet.Model.PaymentOption) {
        selectedOptionId = option.id
        val collectData = option.collectData ?: return
        val url = collectData.url ?: return
        _uiState.value = PaymentUiState.WebViewDataCollection(
            url = buildUrlWithPrefill(url, collectData.schema),
            paymentInfo = storedPaymentInfo,
        )
    }

    private fun preloadOptionGasEstimates(options: List<Wallet.Model.PaymentOption>) {
        optionGasEstimateJob?.cancel()
        optionGasEstimates.clear()
        estimatingOptionIds.clear()
        val requestSeq = ++optionGasEstimateRequestSeq

        val paymentCurrency = storedPaymentInfo?.amount?.display?.assetSymbol
            ?: storedPaymentInfo?.amount?.unit

        optionGasEstimateJob = viewModelScope.launch {
            options.forEach { option ->
                val approvalAction = PaymentUtil.getApprovalAction(option.actions)?.action ?: return@forEach
                estimatingOptionIds.add(option.id)
                syncVisibleState()

                launch {
                    val estimate = runCatching {
                        PaymentTransactionUtil.estimateApprovalFee(approvalAction, paymentCurrency)
                    }.getOrNull()

                    if (requestSeq != optionGasEstimateRequestSeq) return@launch

                    optionGasEstimates[option.id] = estimate
                    estimatingOptionIds.remove(option.id)
                    syncVisibleState()
                }
            }
        }
    }

    private fun syncVisibleState() {
        when (val current = _uiState.value) {
            is PaymentUiState.Options -> {
                _uiState.value = current.copy(
                    gasEstimates = optionGasEstimates.toMap(),
                    estimatingOptionIds = estimatingOptionIds.toSet(),
                )
            }

            is PaymentUiState.Summary -> {
                _uiState.value = current.copy(
                    requiresApproval = PaymentUtil.requiresApproval(current.selectedOption.actions),
                    approvalGasEstimate = optionGasEstimates[current.selectedOption.id],
                    isEstimatingApprovalGas = estimatingOptionIds.contains(current.selectedOption.id),
                    canChangeOption = storedPaymentOptions.size > 1,
                )
            }

            is PaymentUiState.GasFeeInfo -> {
                _uiState.value = current.copy(
                    approvalGasEstimate = optionGasEstimates[current.selectedOption.id],
                )
            }

            else -> Unit
        }
    }

    private fun buildUrlWithPrefill(baseUrl: String, schema: String?): String {
        val theme = if (ThemeManager.isDarkTheme()) "dark" else "light"
        val builder = Uri.parse(baseUrl).buildUpon()
            .appendQueryParameter("theme", theme)
            .appendQueryParameter("themeVariables", THEME_VARIABLES)
        buildPrefillParam(schema)?.let { builder.appendQueryParameter("prefill", it) }
        return builder.build().toString()
    }

    private fun buildPrefillParam(schema: String?): String? {
        if (schema == null) return null

        return try {
            val schemaJson = JSONObject(schema)
            val requiredFields = mutableSetOf<String>()
            val topRequired = schemaJson.optJSONArray("required")
            if (topRequired != null) {
                for (i in 0 until topRequired.length()) {
                    requiredFields.add(topRequired.getString(i))
                }
            }

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

            val fieldValues = mapOf(
                "fullName" to EthAccountDelegate.PREFILL_FULL_NAME,
                "dob" to EthAccountDelegate.PREFILL_DOB,
                "pobAddress" to EthAccountDelegate.PREFILL_POB_ADDRESS,
                "pobCountry" to EthAccountDelegate.PREFILL_POB_COUNTRY,
                "porAddress" to EthAccountDelegate.PREFILL_POR_ADDRESS,
                "porCountry" to EthAccountDelegate.PREFILL_POR_COUNTRY,
            )

            val prefillData = JSONObject()
            for (fieldId in requiredFields) {
                fieldValues[fieldId]?.let { prefillData.put(fieldId, it) }
            }

            if (prefillData.length() == 0) return null

            Base64.encodeToString(
                prefillData.toString().toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP or Base64.URL_SAFE,
            )
        } catch (e: Exception) {
            Log.e("PaymentViewModel", "Failed to build prefill param", e)
            null
        }
    }

    private fun processPayment(option: Wallet.Model.PaymentOption) {
        val paymentId = currentPaymentId ?: return
        selectedOptionId = option.id
        val symbol = option.amount.display?.assetSymbol ?: "token"
        val showSetupLoader = PaymentUtil.shouldShowSetupLoader(option.actions)

        _uiState.value = PaymentUiState.Processing(
            step = LoadingStep.CONFIRMING,
            setupTokenSymbol = if (showSetupLoader) symbol else null,
            paymentInfo = storedPaymentInfo,
        )

        viewModelScope.launch {
            WalletKit.Pay.getRequiredPaymentActions(
                Wallet.Params.RequiredPaymentActions(
                    paymentId = paymentId,
                    optionId = option.id,
                ),
            ).fold(
                onSuccess = { actions ->
                    val walletRpcActions = actions.filterIsInstance<Wallet.Model.RequiredAction.WalletRpc>()
                    executePayment(option, walletRpcActions)
                },
                onFailure = { error ->
                    _uiState.value = PaymentUiState.Error(
                        error.message ?: "Failed to get payment actions",
                        categorizeError(error.message),
                    )
                },
            )
        }
    }

    private suspend fun executePayment(
        option: Wallet.Model.PaymentOption,
        requiredActions: List<Wallet.Model.RequiredAction.WalletRpc>,
    ) {
        val paymentId = currentPaymentId ?: return
        val optionId = selectedOptionId ?: return
        val symbol = option.amount.display?.assetSymbol ?: "token"
        val approvalAction = PaymentUtil.getApprovalAction(requiredActions)
        val showSetupLoader = PaymentUtil.shouldShowSetupLoader(requiredActions)

        if (!showSetupLoader) {
            _uiState.value = PaymentUiState.Processing(
                step = LoadingStep.CONFIRMING,
                paymentInfo = storedPaymentInfo,
            )
        }

        try {
            val signatures = mutableListOf<String>()
            requiredActions.forEach { action ->
                when (action.action.method) {
                    ETH_SEND_TRANSACTION -> {
                        _uiState.value = PaymentUiState.Processing(
                            step = LoadingStep.CONFIRMING,
                            setupTokenSymbol = symbol,
                            paymentInfo = storedPaymentInfo,
                        )

                        val txHash = PaymentTransactionUtil.sendTransactionWithFreshFees(action.action)
                        Log.d("PaymentViewModel", "Approval tx broadcast: $txHash")
                        PaymentTransactionUtil.waitForTransactionConfirmation(action.action.chainId, txHash)
                        signatures.add(txHash)

                        if (showSetupLoader && action == approvalAction) {
                            // Token setup done — drop the setup copy and fall back to the
                            // generic confirming message while the payment finalizes.
                            _uiState.value = PaymentUiState.Processing(
                                step = LoadingStep.CONFIRMING,
                                paymentInfo = storedPaymentInfo,
                            )
                        }
                    }

                    else -> {
                        signatures.add(PaymentSigner.signWalletRpcAction(action.action))
                    }
                }
            }

            val collectedData = collectedValues.takeIf { it.isNotEmpty() }?.map { (id, value) ->
                Wallet.Model.CollectDataFieldResult(id = id, value = value)
            }

            WalletKit.Pay.confirmPayment(
                Wallet.Params.ConfirmPayment(
                    paymentId = paymentId,
                    optionId = optionId,
                    signatures = signatures,
                    collectedData = collectedData,
                ),
            ).fold(
                onSuccess = { response ->
                    when (response.status) {
                        Wallet.Model.PaymentStatus.SUCCEEDED -> {
                            Log.d("PaymentViewModel", "Payment SUCCEEDED")
                            PaymentTokenPreferenceStore.saveLastPaidTokenUnit(option.amount.unit)
                            _uiState.value = PaymentUiState.Success(
                                paymentInfo = storedPaymentInfo,
                                resultInfo = response.info,
                            )
                        }

                        Wallet.Model.PaymentStatus.PROCESSING -> {
                            Log.d("PaymentViewModel", "Payment PROCESSING")
                            _uiState.value = PaymentUiState.Success(
                                paymentInfo = storedPaymentInfo,
                                resultInfo = response.info,
                            )
                        }

                        Wallet.Model.PaymentStatus.FAILED -> {
                            _uiState.value = PaymentUiState.Error("Payment failed", PaymentErrorType.GENERIC)
                        }

                        Wallet.Model.PaymentStatus.EXPIRED -> {
                            _uiState.value = PaymentUiState.Error("Payment expired", PaymentErrorType.EXPIRED)
                        }

                        Wallet.Model.PaymentStatus.REQUIRES_ACTION -> {
                            _uiState.value = PaymentUiState.Error(
                                "Additional action required",
                                PaymentErrorType.GENERIC,
                            )
                        }

                        Wallet.Model.PaymentStatus.CANCELLED -> {
                            _uiState.value = PaymentUiState.Error(
                                "Payment was cancelled",
                                PaymentErrorType.CANCELLED,
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.value = PaymentUiState.Error(
                        error.message ?: "Failed to confirm payment",
                        categorizeError(error.message),
                    )
                },
            )
        } catch (e: Exception) {
            Log.e("PaymentViewModel", "Payment processing failed", e)
            _uiState.value = PaymentUiState.Error(
                e.message ?: "An error occurred during payment",
                categorizeError(e.message),
            )
        }
    }

    private fun isPaymentExpiredLocally(): Boolean {
        val expiresAtSeconds = storedPaymentInfo?.expiresAt ?: return false
        val expiresAtMs = expiresAtSeconds * 1000L
        return expiresAtMs <= System.currentTimeMillis() + PAY_EXPIRY_GUARD_MS
    }

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

    fun cancel() {
        invalidateTransientState()
        currentPaymentLink = null
        currentPaymentId = null
        selectedOptionId = null
        storedPaymentInfo = null
        storedPaymentOptions = emptyList()
        collectedValues.clear()
        WalletKitDelegate.clearPaymentOptions()
    }

    private fun invalidateTransientState() {
        optionGasEstimateJob?.cancel()
        optionGasEstimateJob = null
        optionGasEstimateRequestSeq++
        optionGasEstimates.clear()
        estimatingOptionIds.clear()
    }

    private companion object {
        private const val PAY_EXPIRY_GUARD_MS = 10_000L
        private const val ETH_SEND_TRANSACTION = "eth_sendTransaction"

        // base64url of {"fontFamily":"dm-sans","inputRadius":8,"buttonRadius":8}
        private const val THEME_VARIABLES =
            "eyJmb250RmFtaWx5IjoiZG0tc2FucyIsImlucHV0UmFkaXVzIjo4LCJidXR0b25SYWRpdXMiOjh9"
    }
}

sealed class PaymentUiState {
    data object Loading : PaymentUiState()

    data class WebViewDataCollection(
        val url: String,
        val paymentInfo: Wallet.Model.PaymentInfo?,
    ) : PaymentUiState()

    data class Options(
        val paymentLink: String,
        val paymentInfo: Wallet.Model.PaymentInfo?,
        val options: List<Wallet.Model.PaymentOption>,
        val selectedOptionId: String?,
        val gasEstimates: Map<String, TransactionFeeEstimate?>,
        val estimatingOptionIds: Set<String>,
    ) : PaymentUiState()

    data class Summary(
        val paymentInfo: Wallet.Model.PaymentInfo?,
        val selectedOption: Wallet.Model.PaymentOption,
        val requiresApproval: Boolean = false,
        val approvalGasEstimate: TransactionFeeEstimate? = null,
        val isEstimatingApprovalGas: Boolean = false,
        val canChangeOption: Boolean = false,
    ) : PaymentUiState()

    data class WhyInfoRequired(
        val paymentInfo: Wallet.Model.PaymentInfo?,
    ) : PaymentUiState()

    data class GasFeeInfo(
        val paymentInfo: Wallet.Model.PaymentInfo?,
        val selectedOption: Wallet.Model.PaymentOption,
        val approvalGasEstimate: TransactionFeeEstimate?,
    ) : PaymentUiState()

    data class Processing(
        val step: LoadingStep,
        val setupTokenSymbol: String? = null,
        val paymentInfo: Wallet.Model.PaymentInfo? = null,
    ) : PaymentUiState()

    data class Success(
        val paymentInfo: Wallet.Model.PaymentInfo? = null,
        val resultInfo: Wallet.Model.PaymentResultInfo? = null,
    ) : PaymentUiState()

    data class Error(
        val message: String,
        val errorType: PaymentErrorType = PaymentErrorType.GENERIC,
    ) : PaymentUiState()
}

enum class PaymentErrorType {
    INSUFFICIENT_FUNDS,
    EXPIRED,
    CANCELLED,
    NOT_FOUND,
    GENERIC,
}
