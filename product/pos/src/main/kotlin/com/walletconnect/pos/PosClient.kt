package com.walletconnect.pos

import com.walletconnect.pos.api.ApiClient
import com.walletconnect.pos.api.ApiResult
import com.walletconnect.pos.api.ErrorTracker
import com.walletconnect.pos.api.EventTracker
import com.walletconnect.pos.api.mapErrorCodeToPaymentError
import com.walletconnect.pos.api.mapStatusToPaymentEvent
import com.walletconnect.pos.api.toTransaction
import com.walletconnect.pos.api.toTransactionStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * POS (Point of Sale) client for handling payment transactions.
 */
object PosClient {
    private var delegate: POSDelegate? = null
    private var apiClient: ApiClient? = null
    private var eventTracker: EventTracker? = null
    private var errorTracker: ErrorTracker? = null
    private var scope: CoroutineScope? = null
    private var currentPollingJob: Job? = null

    /**
     * Initializes the POS client with API credentials.
     *
     * @param apiKey Your WalletConnect Pay merchant API key
     * @param merchantId Your merchant identifier
     * @param deviceId Unique identifier for this POS device
     */
    @Throws(IllegalStateException::class)
    fun init(apiKey: String, merchantId: String, deviceId: String) {
        check(apiKey.isNotBlank()) { "apiKey cannot be blank" }
        check(merchantId.isNotBlank()) { "merchantId cannot be blank" }
        check(deviceId.isNotBlank()) { "deviceId cannot be blank" }
        cleanup()
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        this.scope = newScope
        this.eventTracker = EventTracker(merchantId, newScope)
        this.errorTracker = ErrorTracker(newScope)
        this.apiClient = ApiClient(apiKey, merchantId, eventTracker!!, errorTracker!!)
    }

    /**
     * Sets the delegate for receiving payment events.
     *
     * @param delegate The delegate to receive events, or null to remove
     */
    fun setDelegate(delegate: POSDelegate) {
        this.delegate = delegate
    }

    /**
     * Creates a payment intent and starts the payment flow.
     * All payment events are emitted through the delegate.
     *
     * @param amount The payment amount
     * @param referenceId Merchant's reference ID for this payment
     * @throws IllegalStateException if SDK is not initialized
     */
    @Throws(IllegalStateException::class)
    fun createPaymentIntent(amount: Pos.Amount, referenceId: String) {
        checkInitialized()
        currentPollingJob?.cancel()
        val valueMinor = amount.value.toLongOrNull() ?: 0L
        eventTracker?.trackWcPaySelected(referenceId, amount.unit, valueMinor)
        currentPollingJob = scope?.launch {
            apiClient!!.createPayment(referenceId, amount.unit, amount.value) { event ->
                emitEvent(event)
            }
        }
    }

    /**
     * Checks the current status of a payment (one-off, no polling).
     *
     * @param paymentId The payment ID to check
     * @return The current payment status as a [Pos.PaymentEvent]
     * @throws IllegalStateException if SDK is not initialized
     */
    @Throws(IllegalStateException::class)
    suspend fun checkPaymentStatus(paymentId: String): Pos.PaymentEvent {
        checkInitialized()

        return when (val result = apiClient!!.getPaymentStatus(paymentId)) {
            is ApiResult.Success -> mapStatusToPaymentEvent(result.data.status, paymentId)
            is ApiResult.Error -> mapErrorCodeToPaymentError(result.code, result.message)
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
     * Fetches transaction history for the merchant.
     *
     * @param limit Number of transactions to fetch per page (default 20, max 200)
     * @param cursor Pagination cursor from previous result for fetching next page
     * @param status Optional status filter (e.g., "succeeded", "failed", "processing")
     * @return Result containing transaction history or error
     * @throws IllegalStateException if SDK is not initialized
     */
    @Throws(IllegalStateException::class)
    suspend fun getTransactionHistory(
        limit: Int = 20,
        cursor: String? = null,
        status: Pos.TransactionStatus? = null
    ): Result<Pos.TransactionHistoryResult> {
        checkInitialized()

        val statusFilter = status?.apiValue

        return when (val result = apiClient!!.getTransactionHistory(limit, cursor, statusFilter)) {
            is ApiResult.Success -> {
                val data = result.data
                Result.success(
                    Pos.TransactionHistoryResult(
                        transactions = data.data.map { it.toTransaction() },
                        hasMore = data.nextCursor != null,
                        nextCursor = data.nextCursor,
                        stats = data.stats.toTransactionStats()
                    )
                )
            }
            is ApiResult.Error -> {
                Result.failure(Exception("${result.code}: ${result.message}"))
            }
        }
    }

    /**
     * Releases all resources held by the SDK.
     *
     * Call this when the SDK is no longer needed.
     */
    fun shutdown() {
        cleanup()
        delegate = null
    }

    private fun emitEvent(event: Pos.PaymentEvent) {
        delegate?.onEvent(event)
    }

    private fun checkInitialized() {
        check(apiClient != null) { "ApiClient not initialized, call init() first" }
        check(scope != null) { "Scope not initialized, call init() first" }
    }

    private fun cleanup() {
        currentPollingJob?.cancel()
        currentPollingJob = null
        scope?.cancel()
        scope = null
        apiClient = null
        eventTracker = null
        errorTracker = null
    }
}