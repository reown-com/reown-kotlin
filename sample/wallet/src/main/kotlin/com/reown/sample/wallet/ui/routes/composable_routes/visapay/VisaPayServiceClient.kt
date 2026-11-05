package com.reown.sample.wallet.ui.routes.composable_routes.visapay

import com.reown.sample.wallet.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * HTTP client for the hackathon Visa Pay services (github.com/WalletConnect/hackathon-2026-11, `vpay/`):
 * - **bin-sponsor** mock: the bank's cardholder KYC.
 * - **vpay-server**: issues the card at Pismo (the `visa-pay` mock), sets the daily limit, gets tap keys,
 *   card details, payments and activity.
 *
 * Base URLs and keys come from BuildConfig (`VPAY_SERVER_URL`, `BIN_SPONSOR_URL`, ...). The defaults point at the
 * services running locally on the laptop, reached from the phone through `adb reverse`.
 */
internal object VisaPayServiceClient {
    private val JSON = "application/json".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        // Card enrollment polls Pismo for about 1.5 s; leave room for slow demo networks.
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // ---------- bin-sponsor (KYC) ----------

    suspend fun submitKyc(request: KycRequest): KycRecord =
        binSponsor("POST", "/v1/kyc/cardholders", JSONObject().apply {
            put("external_id", request.externalId)
            put("full_name", request.fullName)
            put("date_of_birth", request.dateOfBirth)
            put("country", request.country)
            put("document_number", request.documentNumber)
            put("email", request.email)
        }).toKycRecord()

    suspend fun getKyc(externalId: String): KycRecord =
        binSponsor("GET", "/v1/kyc/cardholders/$externalId").toKycRecord()

    // ---------- vpay-server (card, payments) ----------

    suspend fun createCard(userRef: String, deviceId: String): ServerCard =
        vpay("POST", "/v1/cards", JSONObject().apply {
            put("user_ref", userRef)
            put("device_id", deviceId)
            put("os_type", "ANDROID")
        }).toServerCard()

    suspend fun setDailyLimit(cardId: String, dailyLimit: Int): ServerCard =
        vpay("PATCH", "/v1/cards/$cardId", JSONObject().put("daily_limit", dailyLimit)).toServerCard()

    /** Limited-use keys for tap to pay. Returns how many payments they allow. */
    suspend fun replenishTapKeys(cardId: String): Int? =
        vpay("POST", "/v1/cards/$cardId/replenish", JSONObject().put("mac", "demo")).optIntOrNull("max_pmts")

    suspend fun cardDetails(cardId: String): VisaPayCardDetails =
        vpay("POST", "/v1/cards/$cardId/card-details").let {
            VisaPayCardDetails(number = it.getString("card_number"), expiry = it.getString("expiry"), cvv = it.getString("cvv"))
        }

    /** Demo stand-in for a tap at a terminal: the visa-pay mock sends the payment to the server's webhook. */
    suspend fun demoPay(cardId: String, amount: Double, merchant: String): PaymentResult =
        vpay("POST", "/v1/cards/$cardId/demo-pay", JSONObject().apply {
            put("amount", amount)
            put("merchant", merchant)
        }).let { PaymentResult(responseCode = it.optString("response_code"), transactionId = it.optStringOrNull("transaction_id")) }

    suspend fun activity(cardId: String): ServerActivity =
        vpay("GET", "/v1/cards/$cardId/activity").let { json ->
            ServerActivity(
                leftToday = json.optDouble("left_today", 0.0),
                authorizations = json.optJSONArray("authorizations").orEmpty().map { item ->
                    ServerAuthorization(
                        transactionId = item.optString("pismo_transaction_id"),
                        merchant = item.optString("merchant"),
                        amount = item.optDouble("amount", 0.0),
                        responseCode = item.optString("response_code"),
                    )
                }
            )
        }

    // ---------- plumbing ----------

    private suspend fun binSponsor(method: String, path: String, body: JSONObject? = null) =
        call(BuildConfig.BIN_SPONSOR_URL, BuildConfig.BIN_SPONSOR_API_KEY, method, path, body)

    private suspend fun vpay(method: String, path: String, body: JSONObject? = null) =
        call(BuildConfig.VPAY_SERVER_URL, BuildConfig.VPAY_WALLET_API_KEY, method, path, body)

    private suspend fun call(baseUrl: String, apiKey: String, method: String, path: String, body: JSONObject?): JSONObject =
        withContext(Dispatchers.IO) {
            val requestBody = when {
                body != null -> body.toString().toRequestBody(JSON)
                method == "GET" -> null
                else -> "{}".toRequestBody(JSON)
            }
            val request = Request.Builder()
                .url(baseUrl.trimEnd('/') + path)
                .header("x-api-key", apiKey)
                .method(method, requestBody)
                .build()
            val response = try {
                http.newCall(request).execute()
            } catch (e: java.io.IOException) {
                throw VisaPayServiceException("Can't reach the Visa Pay service at $baseUrl (${e.message})")
            }
            response.use {
                val text = it.body.string()
                if (!it.isSuccessful) {
                    val reason = runCatching { JSONObject(text) }.getOrNull()?.let { json ->
                        json.optJSONObject("error")?.optString("message") ?: json.optString("error")
                    }?.takeIf(String::isNotBlank) ?: text.take(200)
                    throw VisaPayServiceException("$method $path failed (${it.code}): $reason")
                }
                if (text.isBlank()) JSONObject() else JSONObject(text)
            }
        }

    private fun JSONObject.toKycRecord() = KycRecord(
        status = KycStatus.fromValue(optString("status")),
        reason = optStringOrNull("reason"),
    )

    private fun JSONObject.toServerCard() = ServerCard(
        id = getString("id"),
        dailyLimit = optDouble("daily_limit", 0.0),
        last4 = optString("last4"),
    )

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
    private fun JSONObject.optIntOrNull(key: String): Int? = if (has(key) && !isNull(key)) optInt(key) else null
    private fun JSONArray?.orEmpty(): List<JSONObject> = this?.let { array -> List(array.length()) { array.getJSONObject(it) } } ?: emptyList()
}

class VisaPayServiceException(message: String) : Exception(message)

internal data class KycRequest(
    val externalId: String,
    val fullName: String,
    val dateOfBirth: String,
    val country: String,
    val documentNumber: String,
    val email: String,
)

internal enum class KycStatus {
    PENDING, APPROVED, REVIEW, REJECTED;

    companion object {
        fun fromValue(value: String): KycStatus = entries.firstOrNull { it.name == value } ?: PENDING
    }
}

internal data class KycRecord(val status: KycStatus, val reason: String?)

internal data class ServerCard(val id: String, val dailyLimit: Double, val last4: String)

data class PaymentResult(val responseCode: String, val transactionId: String?)

internal data class ServerAuthorization(
    val transactionId: String,
    val merchant: String,
    val amount: Double,
    val responseCode: String,
)

internal data class ServerActivity(val leftToday: Double, val authorizations: List<ServerAuthorization>)

data class VisaPayCardDetails(val number: String, val expiry: String, val cvv: String)
