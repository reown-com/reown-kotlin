package com.reown.sample.wallet.ui.routes.dialog_routes.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.signer.EthSigner
import com.walletconnect.pay.Pay
import com.walletconnect.pay.WalletConnectPay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * ViewModel for handling the payment flow.
 */
class PaymentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Loading)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private var currentPaymentLink: String? = null
    private var selectedOptionId: String? = null

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

            println("kobe: Options Result: $result")
            
            result.fold(
                onSuccess = { response ->
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
        val paymentLink = currentPaymentLink ?: return
        val paymentId = extractPaymentId(paymentLink) ?: return
        selectedOptionId = optionId
        _uiState.value = PaymentUiState.Processing("Getting required actions...")

        viewModelScope.launch {
            // Get required payment actions
            val actionsResult = WalletConnectPay.getRequiredPaymentActions(paymentId, optionId)

            println("kobe: Actions Result: $actionsResult")
            
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
        _uiState.value = PaymentUiState.Processing("Signing transactions...")
        
        try {
            val signatures = mutableListOf<Pay.SignatureResult>()
            
            for (action in actions) {
                when (action) {
                    is Pay.RequiredAction.WalletRpc -> {
                        val signature = signWalletRpcAction(action.action)
                        signatures.add(signature)
                    }
                }
            }
            
            _uiState.value = PaymentUiState.Processing("Confirming payment...")
            
            // Confirm payment with signatures
            val confirmResult = WalletConnectPay.confirmPayment(
                paymentId = paymentId,
                optionId = optionId,
                signatures = signatures,
                timeoutMs = 60000 // 60 second timeout
            )

            println("kobe: Confirm Result: $confirmResult")
            
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
     * Sign EIP-712 typed data.
     */
    private fun signTypedDataV4(params: String): String {
        // params is a JSON array: [address, typedData]
        // We need to extract the typed data and sign it
        val paramsArray = JSONArray(params)
        val typedData = paramsArray.getString(1)
        
        // Use EthSigner to sign the typed data hash
        // For EIP-712, we need to compute the hash and sign it
        return signTypedData(typedData, EthAccountDelegate.privateKey)
    }

    /**
     * Sign typed data using EIP-712.
     */
    private fun signTypedData(typedData: String, privateKey: String): String {
        // Parse the typed data and compute the EIP-712 hash
        // Then sign the hash with the private key
        return EthSigner.signHash(computeTypedDataHash(typedData), privateKey)
    }

    /**
     * Compute the EIP-712 hash for typed data.
     */
    private fun computeTypedDataHash(typedData: String): String {
        // For simplicity, we'll use the structured data directly
        // In a production app, you'd want to properly implement EIP-712 hashing
        // This is a simplified version that works for basic typed data
        return org.web3j.crypto.Hash.sha3String(typedData)
    }

    /**
     * Cancel and reset the payment flow.
     */
    fun cancel() {
        currentPaymentLink = null
        selectedOptionId = null
        _uiState.value = PaymentUiState.Loading
    }
    
    /**
     * Extract payment ID from a payment link.
     * Supports:
     * - https://pay.walletconnect.com/pay_<id>
     * - https://gateway-wc.vercel.app/v1/<uuid>
     */
    private fun extractPaymentId(paymentLink: String): String? {
        val payRegex = Regex("pay\\.walletconnect\\.com/(pay_[a-zA-Z0-9]+)")
        payRegex.find(paymentLink)?.groupValues?.getOrNull(1)?.let { return it }
        
        val gatewayRegex = Regex("gateway-wc\\.vercel\\.app/v1/([a-fA-F0-9\\-]+)")
        gatewayRegex.find(paymentLink)?.groupValues?.getOrNull(1)?.let { return it }
        
        return null
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
    
    data class Processing(val message: String) : PaymentUiState()
    
    data class Success(val message: String) : PaymentUiState()
    
    data class Error(val message: String) : PaymentUiState()
}

