package com.walletconnect.pos.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.walletconnect.pos.BuildConfig
import com.walletconnect.pos.Pos
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class EventTracker(
    private val merchantId: String,
    private val scope: CoroutineScope,
    baseUrl: String = BuildConfig.INGEST_BASE_URL
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.ensureTrailingSlash())
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val ingestApi: IngestApi = retrofit.create(IngestApi::class.java)

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val silentExceptionHandler = CoroutineExceptionHandler { _, t -> println(t) }

    companion object {
        private const val MAX_RETRIES = 3
        private val RETRY_DELAYS_MS = listOf(1000L, 3000L, 5000L)
    }

    /**
     * Track wc_pay_selected event.
     * Called at the start of createPaymentIntent, before payment is created.
     * Has amount and referenceId but no payment_url yet.
     */
    fun trackWcPaySelected(referenceId: String, unit: String, valueMinor: Long) {
        val currency = unit.substringAfter("/", "")
        val displayAmount = valueMinor / 100.0
        val payload = EventPayload(
            amount = PaymentAmountPayload(unit = unit, valueMinor = valueMinor),
            referenceId = referenceId.ifBlank { null },
            displayAmount = displayAmount,
            currency = currency
        )
        sendEvent(referenceId, EventType.WC_PAY_SELECTED, payload)
    }

    /**
     * Track payment_created event.
     * Called after successful payment creation.
     */
    fun trackPaymentCreated(paymentId: String, context: PaymentContext) {
        sendEvent(paymentId, EventType.PAYMENT_CREATED, context.toEventPayload())
    }

    /**
     * Track payment_requested event.
     * Called when payment status changes to requires_action.
     */
    fun trackPaymentRequested(paymentId: String, context: PaymentContext) {
        sendEvent(paymentId, EventType.PAYMENT_REQUESTED, context.toEventPayload())
    }

    /**
     * Track payment_processing event.
     * Called when payment status changes to processing.
     */
    fun trackPaymentProcessing(paymentId: String, context: PaymentContext) {
        sendEvent(paymentId, EventType.PAYMENT_PROCESSING, context.toEventPayload())
    }

    /**
     * Track payment_completed event.
     * Called when payment succeeds.
     */
    fun trackPaymentCompleted(paymentId: String, context: PaymentContext) {
        sendEvent(paymentId, EventType.PAYMENT_COMPLETED, context.toEventPayload())
    }

    /**
     * Track payment_failed event with error details.
     * Called when payment fails.
     */
    fun trackPaymentFailed(paymentId: String, context: PaymentContext?, error: Pos.PaymentEvent.PaymentError) {
        val payload = context?.toErrorEventPayload(error) ?: error.toErrorPayload()
        sendEvent(paymentId, EventType.PAYMENT_FAILED, payload)
    }

    /**
     * Sends an event asynchronously without blocking the caller.
     * Any failures are silently ignored to ensure the payment flow is never affected.
     */
    private fun sendEvent(
        paymentId: String,
        eventType: String,
        payload: EventPayload? = null
    ) {
        try {
            val eventId = UUID.randomUUID().toString()
            val request = IngestEventRequest(
                eventId = eventId,
                paymentId = paymentId,
                eventType = eventType,
                timestamp = isoDateFormat.format(Date()),
                sdkName = "pos-kotlin",
                sdkVersion = BuildConfig.SDK_VERSION,
                merchantId = merchantId,
                payload = payload
            )

            // Fire-and-forget: launch on IO dispatcher with silent exception handling
            scope.launch(Dispatchers.IO + silentExceptionHandler) {
                sendWithRetry(request)
            }
        } catch (_: Exception) {
            // Silently ignore any synchronous errors (e.g., scope cancelled)
            // Event tracking should never affect the payment flow
        }
    }

    private suspend fun sendWithRetry(request: IngestEventRequest) {
        repeat(MAX_RETRIES) { attempt ->
            try {
                val response = ingestApi.sendEvent(request)
                if (response.isSuccessful) {
                    return // Success, exit
                }
                // Non-success HTTP response, retry
            } catch (_: Exception) {
                // Network or other error, retry silently
            }

            // Delay before next retry (except after last attempt)
            if (attempt < MAX_RETRIES - 1) {
                delay(RETRY_DELAYS_MS[attempt])
            }
        }
        // After 3 retries, event is lost - silent failure as per requirements
    }

    private fun String.ensureTrailingSlash(): String {
        return if (endsWith("/")) this else "$this/"
    }
}
