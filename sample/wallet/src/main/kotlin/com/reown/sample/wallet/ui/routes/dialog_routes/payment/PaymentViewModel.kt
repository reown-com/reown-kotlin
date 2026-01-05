package com.reown.sample.wallet.ui.routes.dialog_routes.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.signer.EthSigner
import com.reown.util.bytesToHex
import com.reown.util.hexToBytes
import com.walletconnect.pay.Pay
import com.walletconnect.pay.WalletConnectPay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.crypto.StructuredDataEncoder

/**
 * ViewModel for handling the payment flow.
 */
class PaymentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Loading)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private var currentPaymentLink: String? = null
    private var currentPaymentId: String? = null
    private var selectedOptionId: String? = null
    
    // Information Capture state
    private var pendingWalletRpcActions: List<Pay.RequiredAction.WalletRpc> = emptyList()
    private var collectDataFields: List<Pay.CollectDataField> = emptyList()
    private val collectedValues: MutableMap<String, String> = mutableMapOf()
    private var currentFieldIndex: Int = 0

    /**
     * Load payment options for the given payment link.
     * The payment ID is extracted internally from the link.
     */
    fun loadPaymentOptions(paymentLink: String) {
        currentPaymentLink = paymentLink
        _uiState.value = PaymentUiState.Loading

        viewModelScope.launch {
            // Get user's accounts in CAIP-10 format
            val accounts = listOf(
                "eip155:1:${EthAccountDelegate.address}",
                "eip155:137:${EthAccountDelegate.address}",
                "eip155:8453:${EthAccountDelegate.address}"
            )

            val result = WalletConnectPay.getPaymentOptions(
                paymentLink = paymentLink,
                accounts = accounts,
            )
            result.fold(
                onSuccess = { response ->
                    currentPaymentId = response.paymentId
                    if (response.options.isEmpty()) {
                        _uiState.value = PaymentUiState.Error("No payment options available")
                    } else {
                        _uiState.value = PaymentUiState.Options(
                            paymentLink = paymentLink,
                            paymentInfo = response.info,
                            options = response.options
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = PaymentUiState.Error(error.message ?: "Failed to load payment options")
                }
            )
        }
    }

    /**
     * Process payment with the selected option.
     */
    fun processPayment(optionId: String) {
        val paymentId = currentPaymentId ?: return
        selectedOptionId = optionId
        _uiState.value = PaymentUiState.Processing("Getting required actions...")

        viewModelScope.launch {
            // Get required payment actions
            val actionsResult = WalletConnectPay.getRequiredPaymentActions(paymentId, optionId)
            actionsResult.fold(
                onSuccess = { actions ->
                    processActions(paymentId, optionId, actions)
                },
                onFailure = { error ->
                    _uiState.value = PaymentUiState.Error(error.message ?: "Failed to get payment actions")
                }
            )
        }
    }

    private suspend fun processActions(
        paymentId: String,
        optionId: String,
        actions: List<Pay.RequiredAction>
    ) {
        // Separate WalletRpc and CollectData actions
        pendingWalletRpcActions = actions.filterIsInstance<Pay.RequiredAction.WalletRpc>()
        val collectDataActions = actions.filterIsInstance<Pay.RequiredAction.CollectData>()
        
        // Collect all fields from CollectData actions
        collectDataFields = collectDataActions.flatMap { it.action.fields }
        collectedValues.clear()
        currentFieldIndex = 0
        
        // If there are fields to collect, show the first field
        if (collectDataFields.isNotEmpty()) {
            showCurrentField()
        } else {
            // No data to collect, proceed directly to payment
            executePayment()
        }
    }
    
    /**
     * Show the current field for data collection.
     */
    private fun showCurrentField() {
        if (currentFieldIndex < collectDataFields.size) {
            val field = collectDataFields[currentFieldIndex]
            _uiState.value = PaymentUiState.CollectingData(
                currentStepIndex = currentFieldIndex,
                totalSteps = collectDataFields.size,
                currentField = field,
                currentValue = collectedValues[field.id] ?: "",
                allFields = collectDataFields
            )
        }
    }
    
    /**
     * Submit the value for the current field and move to the next step.
     */
    fun submitFieldValue(fieldId: String, value: String) {
        collectedValues[fieldId] = value
        currentFieldIndex++
        
        if (currentFieldIndex < collectDataFields.size) {
            // Show next field
            showCurrentField()
        } else {
            // All fields collected, execute payment
            viewModelScope.launch {
                executePayment()
            }
        }
    }
    
    /**
     * Go back to the previous field.
     */
    fun goBackToPreviousField() {
        if (currentFieldIndex > 0) {
            currentFieldIndex--
            showCurrentField()
        } else {
            // Go back to options
            currentPaymentLink?.let { loadPaymentOptions(it) }
        }
    }
    
    /**
     * Execute the payment with collected data and signatures.
     */
    private suspend fun executePayment() {
        val paymentId = currentPaymentId ?: return
        val optionId = selectedOptionId ?: return
        
        _uiState.value = PaymentUiState.Processing("Signing transactions...")
        
        try {
            val results = mutableListOf<Pay.ConfirmPaymentResult>()

            // Add collected data if any
            if (collectedValues.isNotEmpty()) {
                results.add(
                    Pay.ConfirmPaymentResult.CollectData(
                        Pay.CollectDataResult(
                            fields = collectedValues.map { (id, value) ->
                                Pay.CollectDataFieldResult(id = id, value = value)
                            }
                        )
                    )
                )
            }

            // Add WalletRpc signatures
            for (action in pendingWalletRpcActions) {
                val signature = signWalletRpcAction(action.action)
                results.add(
                    Pay.ConfirmPaymentResult.WalletRpc(
                        Pay.WalletRpcResult(
                            method = action.action.method,
                            data = listOf(signature.signature.value)
                        )
                    )
                )
            }

            _uiState.value = PaymentUiState.Processing("Confirming payment...")
            
            // Confirm payment with results
            val confirmResult = WalletConnectPay.confirmPayment(
                paymentId = paymentId,
                optionId = optionId,
                results = results
            )

            confirmResult.fold(
                onSuccess = { response ->
                    when (response.status) {
                        Pay.PaymentStatus.SUCCEEDED -> {
                            _uiState.value = PaymentUiState.Success("Payment completed successfully!")
                        }
                        Pay.PaymentStatus.PROCESSING -> {
                            _uiState.value = PaymentUiState.Success("Payment is being processed...")
                        }
                        Pay.PaymentStatus.FAILED -> {
                            _uiState.value = PaymentUiState.Error("Payment failed")
                        }
                        Pay.PaymentStatus.EXPIRED -> {
                            _uiState.value = PaymentUiState.Error("Payment expired")
                        }
                        Pay.PaymentStatus.REQUIRES_ACTION -> {
                            _uiState.value = PaymentUiState.Error("Additional action required")
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.value = PaymentUiState.Error(error.message ?: "Failed to confirm payment")
                }
            )
        } catch (e: Exception) {
            _uiState.value = PaymentUiState.Error(e.message ?: "An error occurred during payment")
        }
    }

    /**
     * Sign a wallet RPC action.
     */
    private fun signWalletRpcAction(action: Pay.WalletRpcAction): Pay.SignatureResult {
        return when (action.method) {
            "eth_signTypedData_v4" -> {
                val signature = signTypedDataV4(action.params)
                Pay.SignatureResult(Pay.SignatureValue(signature))
            }
            "personal_sign" -> {
                val signature = EthSigner.personalSign(action.params)
                Pay.SignatureResult(Pay.SignatureValue(signature))
            }
            else -> {
                throw UnsupportedOperationException("Unsupported signing method: ${action.method}")
            }
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
     * Cancel and reset the payment flow.
     */
    fun cancel() {
        currentPaymentLink = null
        currentPaymentId = null
        selectedOptionId = null
        pendingWalletRpcActions = emptyList()
        collectDataFields = emptyList()
        collectedValues.clear()
        currentFieldIndex = 0
        _uiState.value = PaymentUiState.Loading
    }
}

/**
 * UI state for the payment flow.
 */
sealed class PaymentUiState {
    data object Loading : PaymentUiState()
    
    data class Options(
        val paymentLink: String,
        val paymentInfo: Pay.PaymentInfo?,
        val options: List<Pay.PaymentOption>
    ) : PaymentUiState()
    
    /**
     * State for collecting user data (Information Capture).
     */
    data class CollectingData(
        val currentStepIndex: Int,
        val totalSteps: Int,
        val currentField: Pay.CollectDataField,
        val currentValue: String,
        val allFields: List<Pay.CollectDataField>
    ) : PaymentUiState()
    
    data class Processing(val message: String) : PaymentUiState()
    
    data class Success(val message: String) : PaymentUiState()
    
    data class Error(val message: String) : PaymentUiState()
}

