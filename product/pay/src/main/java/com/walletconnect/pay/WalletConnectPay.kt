package com.walletconnect.pay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uniffi.yttrium_wcpay.WalletConnectPay as YttriumWalletConnectPay
import uniffi.yttrium_wcpay.SdkConfig as YttriumSdkConfig

/**
 * WalletConnectPay SDK client for handling payments.
 */
object WalletConnectPay {

    private var client: YttriumWalletConnectPay? = null

    /**
     * Initializes the WalletConnectPay SDK.
     *
     * @param config SDK configuration
     * @throws IllegalStateException if already initialized
     */
    @Throws(IllegalStateException::class)
    fun initialize(config: Pay.SdkConfig) {
        check(client == null) { "WalletConnectPay is already initialized" }

        val yttriumConfig = YttriumSdkConfig(
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            sdkName = config.sdkName,
            sdkVersion = config.sdkVersion,
            sdkPlatform = config.sdkPlatform
        )

        client = YttriumWalletConnectPay(yttriumConfig)
    }

    /**
     * Checks if the SDK is initialized.
     */
    val isInitialized: Boolean
        get() = client != null

    /**
     * Gets available payment options for a payment.
     *
     * @param paymentId The payment ID
     * @param accounts List of account addresses (e.g., "eip155:1:0x...")
     * @return Result containing payment options or an error
     */
    suspend fun getPaymentOptions(
        paymentId: String,
        accounts: List<String>
    ): Result<Pay.PaymentOptionsResponse> = withContext(Dispatchers.IO) {
        val yttriumClient = client ?: return@withContext Result.failure(
            IllegalStateException("WalletConnectPay not initialized. Call initialize() first.")
        )

        println("kobe: accounts: $accounts")

        try {
            val response = yttriumClient.getPaymentOptions(paymentId, accounts)
            Result.success(Mappers.mapPaymentOptionsResponse(response))
        } catch (e: uniffi.yttrium_wcpay.GetPaymentOptionsException) {
            Result.failure(Mappers.mapGetPaymentOptionsError(e))
        } catch (e: Exception) {
            Result.failure(Pay.GetPaymentOptionsError.InternalError(e.message ?: "Unknown error"))
        }
    }

    /**
     * Gets required payment actions for a selected payment option.
     *
     * @param paymentId The payment ID
     * @param optionId The selected payment option ID
     * @return Result containing list of required actions or an error
     */
    suspend fun getRequiredPaymentActions(
        paymentId: String,
        optionId: String
    ): Result<List<Pay.RequiredAction>> = withContext(Dispatchers.IO) {
        val yttriumClient = client ?: return@withContext Result.failure(
            IllegalStateException("WalletConnectPay not initialized. Call initialize() first.")
        )

        try {
            val actions = yttriumClient.getRequiredPaymentActions(paymentId, optionId)
            Result.success(actions.map { Mappers.mapRequiredAction(it) })
        } catch (e: uniffi.yttrium_wcpay.GetPaymentRequestException) {
            Result.failure(Mappers.mapGetPaymentRequestError(e))
        } catch (e: Exception) {
            Result.failure(Pay.GetPaymentRequestError.InternalError(e.message ?: "Unknown error"))
        }
    }

    /**
     * Confirms a payment with signatures.
     *
     * @param paymentId The payment ID
     * @param optionId The selected payment option ID
     * @param signatures List of signature results from signing the required actions
     * @param timeoutMs Optional timeout in milliseconds for polling (default: 30000)
     * @return Result containing confirm payment response or an error
     */
    suspend fun confirmPayment(
        paymentId: String,
        optionId: String,
        signatures: List<Pay.SignatureResult>,
        timeoutMs: Long? = null
    ): Result<Pay.ConfirmPaymentResponse> = withContext(Dispatchers.IO) {
        val yttriumClient = client ?: return@withContext Result.failure(
            IllegalStateException("WalletConnectPay not initialized. Call initialize() first.")
        )

        try {
            val yttriumSignatures = signatures.map { Mappers.mapSignatureResultToYttrium(it) }
            val response = yttriumClient.confirmPayment(paymentId, optionId, yttriumSignatures, timeoutMs)
            Result.success(Mappers.mapConfirmPaymentResponse(response))
        } catch (e: uniffi.yttrium_wcpay.ConfirmPaymentException) {
            Result.failure(Mappers.mapConfirmPaymentError(e))
        } catch (e: uniffi.yttrium_wcpay.PayException) {
            Result.failure(Mappers.mapPayError(e))
        } catch (e: Exception) {
            Result.failure(Pay.ConfirmPaymentError.InternalError(e.message ?: "Unknown error"))
        }
    }

    /**
     * Shuts down the SDK and releases resources.
     */
    fun shutdown() {
        client = null
    }
}
