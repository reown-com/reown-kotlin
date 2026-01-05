package com.walletconnect.pay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uniffi.yttrium_wcpay.Logger
import uniffi.yttrium_wcpay.registerLogger
import uniffi.yttrium_wcpay.WalletConnectPay as YttriumWalletConnectPay
import uniffi.yttrium_wcpay.SdkConfig as YttriumSdkConfig

/**
 * WalletConnectPay SDK client for handling payments.
 */
object WalletConnectPay {

    //TODO: only for DEBUG
    class AndroidLogger : Logger {
        override fun log(message: String) {
            println("WalletConnectPay: $message")
        }
    }

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

        registerLogger(AndroidLogger())

        val yttriumConfig = YttriumSdkConfig(
            baseUrl = "https://api.pay.walletconnect.com",
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
     * @param paymentLink The payment link URL (e.g., "https://pay.walletconnect.com/pay_xxx")
     * @param accounts List of account addresses (e.g., "eip155:1:0x...")
     * @return Result containing payment options or an error
     */
    suspend fun getPaymentOptions(
        paymentLink: String,
        accounts: List<String>,
    ): Result<Pay.PaymentOptionsResponse> = withContext(Dispatchers.IO) {
        val yttriumClient = client ?: return@withContext Result.failure(
            IllegalStateException("WalletConnectPay not initialized. Call initialize() first.")
        )

        try {
            val response = yttriumClient.getPaymentOptions(paymentLink = paymentLink, accounts = accounts, includePaymentInfo = true)
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
            val actions = yttriumClient.getRequiredPaymentActions(paymentId = paymentId, optionId = optionId)
            Result.success(actions.map { Mappers.mapRequiredAction(it) })
        } catch (e: uniffi.yttrium_wcpay.GetPaymentRequestException) {
            Result.failure(Mappers.mapGetPaymentRequestError(e))
        } catch (e: Exception) {
            Result.failure(Pay.GetPaymentRequestError.InternalError(e.message ?: "Unknown error"))
        }
    }

    /**
     * Confirms a payment with action results.
     *
     * @param paymentId The payment ID
     * @param optionId The selected payment option ID
     * @param results List of results from completing the required actions (wallet RPC or collect data)
     * @return Result containing confirm payment response or an error
     */
    suspend fun confirmPayment(
        paymentId: String,
        optionId: String,
        results: List<Pay.ConfirmPaymentResult>
    ): Result<Pay.ConfirmPaymentResponse> = withContext(Dispatchers.IO) {
        val yttriumClient = client ?: return@withContext Result.failure(
            IllegalStateException("WalletConnectPay not initialized. Call initialize() first.")
        )
        try {
            val yttriumResults = results.map { Mappers.mapConfirmPaymentResultToYttrium(it) }
            val response = yttriumClient.confirmPayment(paymentId = paymentId, optionId = optionId, results = yttriumResults, maxPollMs = 60000)
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
