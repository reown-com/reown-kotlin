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
    
    // Stored payment options for after information capture
    private var storedPaymentInfo: Pay.PaymentInfo? = null
    private var storedPaymentOptions: List<Pay.PaymentOption> = emptyList()
    
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
                "eip155:8453:${EthAccountDelegate.address}",
                "eip155:10:${EthAccountDelegate.address}"
            )

            val result = WalletConnectPay.getPaymentOptions(
                paymentLink = paymentLink,
                accounts = accounts,
            )
            result.fold(
                onSuccess = { response ->
                    currentPaymentId = response.paymentId
                    // Store collect data fields from response (if any)
                    collectDataFields = response.collectDataAction?.fields ?: emptyList()
                    collectedValues.clear()
                    currentFieldIndex = 0
                    
                    // Store payment options for later use
                    storedPaymentInfo = response.info
                    storedPaymentOptions = response.options
                    
                    if (response.options.isEmpty()) {
                        _uiState.value = PaymentUiState.Error("No payment options available")
                    } else if (collectDataFields.isNotEmpty()) {
                        // Show intro screen only when information capture is required
                        _uiState.value = PaymentUiState.Intro(
                            paymentInfo = response.info,
                            hasInfoCapture = true
                        )
                    } else {
                        // No information capture required, go directly to options
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
     * Proceed from the intro screen to either information capture or payment options.
     */
    fun proceedFromIntro() {
        if (collectDataFields.isNotEmpty()) {
            // Show information capture first
            showCurrentField()
        } else {
            // No fields to collect, show options directly
            proceedToOptions()
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
        // Store WalletRpc actions for signing
        pendingWalletRpcActions = actions.filterIsInstance<Pay.RequiredAction.WalletRpc>()
        
        // Information capture already done before options, proceed directly to payment
        executePayment()
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
     * Proceed to payment options after information capture is complete.
     */
    private fun proceedToOptions() {
        val paymentLink = currentPaymentLink ?: return
        _uiState.value = PaymentUiState.Options(
            paymentLink = paymentLink,
            paymentInfo = storedPaymentInfo,
            options = storedPaymentOptions
        )
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
            // All fields collected, proceed to payment options
            proceedToOptions()
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
            // On first field, go back to intro
            goBackToIntro()
        }
    }
    
    /**
     * Go back to the intro screen.
     */
    fun goBackToIntro() {
        _uiState.value = PaymentUiState.Intro(
            paymentInfo = storedPaymentInfo,
            hasInfoCapture = collectDataFields.isNotEmpty()
        )
    }
    
    /**
     * Execute the payment with collected data and signatures.
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
                    Pay.CollectDataFieldResult(id = id, value = value)
                }
            } else {
                null
            }
            
            // Confirm payment with signatures and collected data
            val confirmResult = WalletConnectPay.confirmPayment(
                paymentId = paymentId,
                optionId = optionId,
                signatures = signatures,
                collectedData = collectedData
            )

            confirmResult.fold(
                onSuccess = { response ->
                    when (response.status) {
                        Pay.PaymentStatus.SUCCEEDED -> {
                            _uiState.value = PaymentUiState.Success(
                                message = "Payment completed successfully!",
                                paymentInfo = storedPaymentInfo
                            )
                        }
                        Pay.PaymentStatus.PROCESSING -> {
                            _uiState.value = PaymentUiState.Success(
                                message = "Payment is being processed...",
                                paymentInfo = storedPaymentInfo
                            )
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
     * Sign a wallet RPC action and return the signature string.
     */
    private fun signWalletRpcAction(action: Pay.WalletRpcAction): String {
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
     * Cancel and reset the payment flow.
     */
    fun cancel() {
        currentPaymentLink = null
        currentPaymentId = null
        selectedOptionId = null
        storedPaymentInfo = null
        storedPaymentOptions = emptyList()
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
    
    /**
     * Intro screen shown before information capture.
     */
    data class Intro(
        val paymentInfo: Pay.PaymentInfo?,
        val hasInfoCapture: Boolean,
        val estimatedTime: String = "~2min"
    ) : PaymentUiState()
    
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
    
    data class Processing(
        val message: String,
        val paymentInfo: Pay.PaymentInfo? = null
    ) : PaymentUiState()
    
    data class Success(
        val message: String,
        val paymentInfo: Pay.PaymentInfo? = null
    ) : PaymentUiState()
    
    data class Error(val message: String) : PaymentUiState()
}


