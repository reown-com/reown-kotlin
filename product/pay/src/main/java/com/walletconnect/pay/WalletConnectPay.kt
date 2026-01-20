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

    class AndroidLogger : Logger {
        override fun log(message: String) {
            if (BuildConfig.DEBUG) println("WalletConnectPay: $message")
        }
    }

    @Volatile
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
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            projectId = config.appId,
            appId = config.appId,
            sdkName = "kotlin-walletconnect-pay",
            sdkVersion = BuildConfig.SDK_VERSION,
            sdkPlatform = "android",
            bundleId = config.packageName,
            clientId = config.clientId,
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
     * Confirms a payment with signatures and optional collected data.
     *
     * @param paymentId The payment ID
     * @param optionId The selected payment option ID
     * @param signatures List of signature strings from wallet RPC actions
     * @param collectedData Optional list of collected data field results
     * @return Result containing confirm payment response or an error
     */
    suspend fun confirmPayment(
        paymentId: String,
        optionId: String,
        signatures: List<String>,
        collectedData: List<Pay.CollectDataFieldResult>? = null
    ): Result<Pay.ConfirmPaymentResponse> = withContext(Dispatchers.IO) {
        val yttriumClient = client ?: return@withContext Result.failure(
            IllegalStateException("WalletConnectPay not initialized. Call initialize() first.")
        )
        try {
            val yttriumCollectedData = collectedData?.map { Mappers.mapCollectDataFieldResultToYttrium(it) }
            val response = yttriumClient.confirmPayment(
                paymentId = paymentId,
                optionId = optionId,
                signatures = signatures,
                collectedData = yttriumCollectedData,
                maxPollMs = 60000
            )
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
