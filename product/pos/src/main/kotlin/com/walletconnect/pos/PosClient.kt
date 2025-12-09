package com.walletconnect.pos

import com.walletconnect.pos.internal.ApiClient
import com.walletconnect.pos.internal.ApiResult
import com.walletconnect.pos.internal.buildPaymentUri
import com.walletconnect.pos.internal.mapCreatePaymentError
import com.walletconnect.pos.internal.mapErrorCodeToPaymentError
import com.walletconnect.pos.internal.mapStatusToPaymentEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.jvm.Throws

/**
 * POS (Point of Sale) client for handling payment transactions.
 */
object PosClient {
    private var delegate: POSDelegate? = null
    private var apiClient: ApiClient? = null
    private val initialized = AtomicBoolean(false)
    private var scope: CoroutineScope? = null
    private var currentPollingJob: Job? = null

    /**
     * Initializes the POS client with API credentials.
     *
     * @param apiKey Your WalletConnect Pay API key
     * @param deviceId Unique identifier for this POS device
     */
    @Throws(IllegalArgumentException::class)
    fun init(apiKey: String, deviceId: String) {
        require(apiKey.isNotBlank()) { "apiKey cannot be blank" }
        require(deviceId.isNotBlank()) { "deviceId cannot be blank" }
        cleanup()
        this.apiClient = ApiClient(apiKey, deviceId)
        this.scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        this.initialized.set(true)
    }

    /**
     * Sets the delegate for receiving payment events.
     *
     * @param delegate The delegate to receive events, or null to remove
     */
    fun setDelegate(delegate: POSDelegate?) {
        this.delegate = delegate
    }

    /**
     * Creates a payment intent and starts the payment flow.
     *
     * @param amount The payment amount
     * @param referenceId Merchant's reference ID for this payment
     * @throws IllegalStateException if SDK is not initialized
     */
    @Throws(java.lang.IllegalStateException::class)
    fun createPaymentIntent(amount: Pos.Model.Amount, referenceId: String) {
        checkInitialized()
        require(apiClient != null) { "ApiClient not initialized" }
        currentPollingJob?.cancel()

        currentPollingJob = scope?.launch {
            //TODO: apiclient nullable
            when (val result = apiClient!!.createPayment(referenceId, amount.unit, amount.value)) {
                is ApiResult.Success -> {
                    val data = result.data
                    //TODO: return gateway URL from API
                    val uri = URI(buildPaymentUri(data.paymentId))
//                    val formattedAmount = formatAmount(data.amount.unit, data.amount.value)

                    emitEvent(
                        Pos.Model.PaymentEvent.PaymentCreated(
                            uri = uri,
                            amount = Pos.Model.Amount(data.amount.unit, data.amount.value),
                            paymentId = data.paymentId
                        )
                    )
                    apiClient!!.startPolling(data.paymentId, data.pollInMs) { event -> emitEvent(event) }
                }

                is ApiResult.Error -> {
                    emitEvent(
                        Pos.Model.PaymentEvent.PaymentError(
                            mapCreatePaymentError(result.code, result.message)
                        )
                    )
                }
            }
        }
    }

    /**
     * Checks the current status of a payment.
     *
     * @param paymentId The payment ID to check
     * @return The current payment status as a [Pos.Model.PaymentEvent]
     * @throws IllegalStateException if SDK is not initialized
     */
    @Throws(IllegalStateException::class)
    suspend fun checkPaymentStatus(paymentId: String): Pos.Model.PaymentEvent {
        checkInitialized()
        require(apiClient != null) { "ApiClient not initialized" }

        return when (val result = apiClient!!.getPayment(paymentId)) {
            is ApiResult.Success -> {
                mapStatusToPaymentEvent(result.data.status, result.data.paymentId)
            }

            is ApiResult.Error -> {
                Pos.Model.PaymentEvent.PaymentError(
                    mapErrorCodeToPaymentError(result.code, result.message)
                )
            }
        }
    }

    /**
     * Cancels any ongoing polling and releases resources.
     *
     * Call this when the payment flow is cancelled by the user
     * or when the POS screen is closed.
     */
    fun cancelPayment() {
        currentPollingJob?.cancel()
        currentPollingJob = null
    }

    /**
     * Releases all resources held by the SDK.
     *
     * Call this when the SDK is no longer needed.
     */
    fun shutdown() {
        cleanup()
        delegate = null
        initialized.set(false)
    }

    private fun emitEvent(event: Pos.Model.PaymentEvent) {
        delegate?.onEvent(event)
    }

    private fun checkInitialized() {
        check(initialized.get()) {
            "POSClient is not initialized. Call POSClient.init(apiKey, deviceId) first."
        }
    }

    private fun cleanup() {
        currentPollingJob?.cancel()
        currentPollingJob = null
        scope?.cancel()
        scope = null
        apiClient = null
    }
}