package com.walletconnect.pos

import androidx.annotation.WorkerThread
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.walletconnect.pos.api.ApiClient
import com.walletconnect.pos.api.ApiResult
import com.walletconnect.pos.api.ErrorTracker
import com.walletconnect.pos.api.EventTracker
import com.walletconnect.pos.api.MtlsConfig
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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor


/**
 * POS (Point of Sale) client for handling payment transactions.
 */
object PosClient {
    private val lock = Any()

    @Volatile private var delegate: POSDelegate? = null
    @Volatile private var apiClient: ApiClient? = null
    @Volatile private var eventTracker: EventTracker? = null
    @Volatile private var errorTracker: ErrorTracker? = null
    @Volatile private var scope: CoroutineScope? = null
    @Volatile private var currentPollingJob: Job? = null
    @Volatile private var sharedHttpClient: OkHttpClient? = null
    @Volatile private var payHttpClient: OkHttpClient? = null

    private val sharedMoshi: Moshi by lazy {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }

    /**
     * Initializes the POS client with API credentials.
     *
     * @param apiKey Your WalletConnect Pay merchant API key
     * @param merchantId Your merchant identifier
     * @param deviceId Unique identifier for this POS device
     * @param mtlsConfig mTLS configuration. When using [Pos.MtlsConfig.DeviceKeyChain],
     *   this method must be called from a background thread.
     */
    @WorkerThread
    @Throws(IllegalStateException::class)
    fun init(
        apiKey: String,
        merchantId: String,
        deviceId: String,
        mtlsConfig: Pos.MtlsConfig = Pos.MtlsConfig.Disabled
    ) {
        check(apiKey.isNotBlank()) { "apiKey cannot be blank" }
        check(merchantId.isNotBlank()) { "merchantId cannot be blank" }
        check(deviceId.isNotBlank()) { "deviceId cannot be blank" }
        synchronized(lock) {
            cleanup()
            val baseHttpClient = createBaseHttpClient()
            sharedHttpClient = baseHttpClient

            val (mtlsClient, apiBaseUrl) = when (mtlsConfig) {
                is Pos.MtlsConfig.DeviceKeyChain -> createMtlsHttpClientFromDeviceKeyChain(mtlsConfig.context, mtlsConfig.alias) to BuildConfig.MTLS_API_BASE_URL
                is Pos.MtlsConfig.Disabled -> baseHttpClient to BuildConfig.CORE_API_BASE_URL
            }
            payHttpClient = mtlsClient

            val newScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope = newScope
            eventTracker = EventTracker(merchantId, deviceId, newScope, sharedMoshi, baseHttpClient)
            errorTracker = ErrorTracker(newScope, sharedMoshi, baseHttpClient)
            apiClient = ApiClient(apiKey, merchantId, eventTracker!!, errorTracker!!, sharedMoshi, mtlsClient, apiBaseUrl)
        }
    }

    /**
     * Sets the delegate for receiving payment events.
     * Delegate callbacks are dispatched on the main thread when available.
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
     * @param amount The payment amount. [Pos.Amount.value] must be a non-negative integer string
     *               representing the amount in minor units (e.g., cents for USD).
     * @param referenceId Merchant's reference ID for this payment
     * @throws IllegalStateException if SDK is not initialized
     * @throws IllegalArgumentException if amount value is not a valid non-negative integer
     */
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun createPaymentIntent(amount: Pos.Amount, referenceId: String) {
        synchronized(lock) {
            checkInitialized()
            requireValidAmount(amount)
            currentPollingJob?.cancel()
            val valueMinor = amount.value.toLong()
            eventTracker?.trackWcPaySelected(referenceId, amount.unit, valueMinor)
            currentPollingJob = scope!!.launch {
                apiClient!!.createPayment(referenceId, amount.unit, amount.value) { event ->
                    emitEvent(event)
                }
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
    suspend fun checkPaymentStatus(paymentId: String, maxPollMs: Long? = null): Pos.PaymentEvent {
        val client = synchronized(lock) {
            checkInitialized()
            apiClient!!
        }

        return when (val result = client.getPaymentStatus(paymentId, maxPollMs)) {
            is ApiResult.Success -> mapStatusToPaymentEvent(result.data.status, paymentId)
            is ApiResult.Error -> mapErrorCodeToPaymentError(result.code, result.message)
        }
    }

    /**
     * Pauses payment status polling.
     *
     * Call this when the app goes to the background to stop polling.
     * Use [resume] to restart polling when the app returns to the foreground.
     */
    fun pause() {
        synchronized(lock) {
            currentPollingJob?.cancel()
            currentPollingJob = null
        }
    }

    /**
     * Resumes payment status polling if a payment is active.
     *
     * Call this when the app returns to the foreground.
     * Does nothing if there is no active payment to poll.
     */
    fun resume() {
        synchronized(lock) {
            val client = apiClient ?: return
            if (client.activePollingState == null) return
            currentPollingJob?.cancel()
            currentPollingJob = scope?.launch {
                client.resumePolling { event ->
                    emitEvent(event)
                }
            }
        }
    }

    /**
     * Cancels an active payment.
     *
     * Immediately stops polling and clears state so the UI can navigate away without waiting.
     * The cancel API request is sent in the background on a best-effort basis;
     * failures are silently ignored since the user does not need to know.
     */
    fun cancelPayment() {
        synchronized(lock) {
            currentPollingJob?.cancel()
            currentPollingJob = null
            val client = apiClient
            val paymentId = client?.activePollingState?.paymentId
            client?.clearActivePollingState()
            if (client != null && paymentId != null) {
                scope?.launch {
                    client.cancelPayment(paymentId)
                }
            }
        }
    }

    /**
     * Fetches transaction history for the merchant.
     *
     * @param limit Number of transactions to fetch per page (default 20, max 200)
     * @param cursor Pagination cursor from previous result for fetching next page
     * @param statuses Optional status filter list (e.g., listOf(SUCCEEDED) or listOf(FAILED, EXPIRED, CANCELLED))
     * @param dateRange Optional date range filter. Use [Pos.DateRanges] factory methods
     *                  for common ranges (e.g., `DateRanges.today()`, `DateRanges.thisWeek()`).
     *                  Defaults to null (all time).
     * @return Result containing transaction history or error
     * @throws IllegalStateException if SDK is not initialized
     * @throws IllegalArgumentException if limit is not between 1 and 200
     */
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    suspend fun getTransactionHistory(
        limit: Int = 20,
        cursor: String? = null,
        statuses: List<Pos.TransactionStatus>? = null,
        dateRange: Pos.DateRange? = null
    ): Result<Pos.TransactionHistoryResult> {
        require(limit in 1..200) { "limit must be between 1 and 200" }

        val client = synchronized(lock) {
            checkInitialized()
            apiClient!!
        }

        val statusFilter = statuses?.map { it.apiValue }

        return when (val result = client.getTransactionHistory(
            limit = limit,
            cursor = cursor,
            status = statusFilter,
            startTs = dateRange?.startTime,
            endTs = dateRange?.endTime
        )) {
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
        synchronized(lock) {
            cleanup()
            delegate = null
        }
    }

    private fun emitEvent(event: Pos.PaymentEvent) {
        val currentDelegate = delegate ?: return
        val currentScope = scope ?: return
        try {
            currentScope.launch(Dispatchers.Main.immediate) {
                currentDelegate.onEvent(event)
            }
        } catch (_: IllegalStateException) {
            // Main dispatcher not available (e.g., in unit tests) — call directly
            currentDelegate.onEvent(event)
        }
    }

    private fun checkInitialized() {
        check(apiClient != null) { "PosClient not initialized, call init() first" }
        check(scope != null) { "PosClient not initialized, call init() first" }
    }

    // Must be called within synchronized(lock)
    private fun cleanup() {
        currentPollingJob?.cancel()
        currentPollingJob = null
        scope?.cancel()
        scope = null
        apiClient = null
        eventTracker = null
        errorTracker = null
        payHttpClient?.let { client ->
            if (client !== sharedHttpClient) {
                client.dispatcher.executorService.shutdown()
                client.connectionPool.evictAll()
            }
        }
        payHttpClient = null
        sharedHttpClient?.let { client ->
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
        sharedHttpClient = null
    }

    private fun createBaseHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.HEADERS
                        redactHeader("Api-Key")
                        redactHeader("Merchant-Id")
                    })
                }
            }
            .build()
    }

    private fun createMtlsHttpClientFromDeviceKeyChain(context: android.content.Context, alias: String): OkHttpClient {
        val (sslSocketFactory, trustManager) = MtlsConfig.createSslConfigFromDeviceKeyChain(context, alias)
        return buildMtlsOkHttpClient(sslSocketFactory, trustManager)
    }

    private fun buildMtlsOkHttpClient(
        sslSocketFactory: javax.net.ssl.SSLSocketFactory,
        trustManager: javax.net.ssl.X509TrustManager
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustManager)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.HEADERS
                        redactHeader("Api-Key")
                        redactHeader("Merchant-Id")
                    })
                }
            }
            .build()
    }

    private fun requireValidAmount(amount: Pos.Amount) {
        val value = amount.value.toLongOrNull()
        require(value != null && value >= 0) {
            "amount.value must be a non-negative integer string, got: \"${amount.value}\""
        }
    }
}