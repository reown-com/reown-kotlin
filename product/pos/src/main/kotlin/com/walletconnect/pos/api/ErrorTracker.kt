package com.walletconnect.pos.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.walletconnect.pos.BuildConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class ErrorTracker(
    private val scope: CoroutineScope,
    baseUrl: String = BuildConfig.PULSE_BASE_URL
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

    private val pulseApi: PulseApi = retrofit.create(PulseApi::class.java)

    private val silentExceptionHandler = CoroutineExceptionHandler { _, t -> println(t) }

    companion object {
        private const val MAX_RETRIES = 3
        private val RETRY_DELAYS_MS = listOf(1000L, 3000L, 5000L)
        private const val SDK_TYPE = "pos"
    }

    fun trackError(type: String, message: String, method: String) {
        try {
            val event = PulseEvent(
                eventId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                props = PulseProps(
                    type = type,
                    properties = PulseErrorProperties(
                        message = message,
                        method = method
                    )
                )
            )

            scope.launch(Dispatchers.IO + silentExceptionHandler) {
                sendWithRetry(event)
            }
        } catch (_: Exception) {
            // Silently ignore any synchronous errors
            // Error tracking should never affect the main flow
        }
    }

    private suspend fun sendWithRetry(event: PulseEvent) {
        repeat(MAX_RETRIES) { attempt ->
            try {
                val response = pulseApi.sendEvent(
                    sdkType = SDK_TYPE,
                    sdkVersion = BuildConfig.SDK_VERSION,
                    projectId = BuildConfig.POS_PROJECT_ID,
                    body = event
                )
                if (response.isSuccessful) {
                    return
                }
            } catch (_: Exception) {
                // Network or other error, retry silently
            }

            if (attempt < MAX_RETRIES - 1) {
                delay(RETRY_DELAYS_MS[attempt])
            }
        }
    }

    private fun String.ensureTrailingSlash(): String {
        return if (endsWith("/")) this else "$this/"
    }
}
