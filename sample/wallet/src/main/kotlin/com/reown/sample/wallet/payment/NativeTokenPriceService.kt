@file:JvmSynthetic

package com.reown.sample.wallet.payment

import android.util.Log
import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.blockchain.FungiblePriceRequest
import com.reown.sample.wallet.blockchain.createFungiblePriceApiService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap

internal data class NativeTokenPrice(
    val price: Double,
    val currency: String,
)

internal object NativeTokenPriceService {

    private const val TAG = "NativeTokenPriceService"
    private const val NATIVE_TOKEN_ADDRESS = "0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
    private const val CACHE_TTL_MS = 60_000L
    private const val REQUEST_TIMEOUT_MS = 10_000L
    private const val DEFAULT_FIAT_CURRENCY = "USD"
    private val SUPPORTED_FIAT_CURRENCIES = setOf("USD", "EUR")

    private data class CachedPrice(val price: Double, val expiresAt: Long)

    private val priceCache = ConcurrentHashMap<String, CachedPrice>()
    private val inFlightRequests = ConcurrentHashMap<String, CompletableDeferred<NativeTokenPrice?>>()

    private val service by lazy { createFungiblePriceApiService() }

    fun normalizeFiatCurrency(currency: String?): String {
        val normalized = currency?.trim()?.uppercase()
        return if (normalized != null && normalized in SUPPORTED_FIAT_CURRENCIES) normalized
        else DEFAULT_FIAT_CURRENCY
    }

    suspend fun fetchNativeTokenPrice(chainId: String, currency: String?): NativeTokenPrice? {
        val projectId = BuildConfig.PROJECT_ID.trim()
        if (projectId.isEmpty()) return null

        val fiatCurrency = normalizeFiatCurrency(currency)
        val cacheKey = "$fiatCurrency:$chainId"

        priceCache[cacheKey]?.let { cached ->
            if (cached.expiresAt > System.currentTimeMillis()) {
                return NativeTokenPrice(cached.price, fiatCurrency)
            }
        }

        inFlightRequests[cacheKey]?.let { existing -> return existing.await() }

        val deferred = CompletableDeferred<NativeTokenPrice?>()
        val previous = inFlightRequests.putIfAbsent(cacheKey, deferred)
        if (previous != null) return previous.await()

        return try {
            val nativeAddress = "$chainId:$NATIVE_TOKEN_ADDRESS"
            val price = runCatching {
                withTimeout(REQUEST_TIMEOUT_MS) {
                    val response = service.getFungiblePrice(
                        FungiblePriceRequest(
                            projectId = projectId,
                            currency = fiatCurrency.lowercase(),
                            addresses = listOf(nativeAddress),
                        ),
                    )
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Native token price HTTP ${response.code()} for $chainId")
                        null
                    } else {
                        response.body()?.fungibles
                            ?.firstOrNull { it.address?.equals(nativeAddress, ignoreCase = true) == true }
                            ?.price
                            ?.takeIf { it.isFinite() && it > 0.0 }
                    }
                }
            }.getOrElse { error ->
                Log.w(TAG, "Native token price fetch failed for $chainId: ${error.message}")
                null
            }

            val result = price?.let { NativeTokenPrice(it, fiatCurrency) }
            if (price != null) {
                priceCache[cacheKey] = CachedPrice(price, System.currentTimeMillis() + CACHE_TTL_MS)
            }
            deferred.complete(result)
            result
        } finally {
            inFlightRequests.remove(cacheKey, deferred)
        }
    }
}
