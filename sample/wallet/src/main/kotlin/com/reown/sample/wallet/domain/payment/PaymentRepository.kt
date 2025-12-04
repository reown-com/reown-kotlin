package com.reown.sample.wallet.domain.payment

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object PaymentRepository {
    private const val BASE_URL = "https://pay-mvp-core-worker.walletconnect-v1-bridge.workers.dev/"
    private val gson: Gson = GsonBuilder().create()

    private val apiService: PaymentApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()

        retrofit.create(PaymentApi::class.java)
    }

    var currentPayment: PaymentSession? = null
        private set

    /**
     * Prepares a payment session by fetching payment info and building payment options.
     * 
     * @param paymentId The payment identifier
     * @param account The user's wallet account in CAIP-10 format (e.g., "eip155:8453:0x742d...")
     * @return PaymentSession with the selected payment option
     */
    suspend fun preparePayment(paymentId: String, account: String): PaymentSession {
        Log.d("PaymentRepository", "preparePayment start id=$paymentId account=$account")
        
        // Step 1: Get payment info
        val paymentInfo = apiService.getPaymentInfo(paymentId)
        Log.d("PaymentRepository", "getPaymentInfo status=${paymentInfo.status} amount=${paymentInfo.amount} currency=${paymentInfo.currency}")
        
        // Step 2: Build payment options with the user's account
        val buildPaymentResponse = apiService.buildPayment(BuildPaymentRequest(paymentId, listOf(account)))
        Log.d("PaymentRepository", "buildPayment options=${buildPaymentResponse.options.size}")
        
        // Step 3: Find USDC on Base network (eip155:8453) as the preferred payment option
        val selectedOption = buildPaymentResponse.options.firstOrNull { 
            it.symbol == "USDC" && it.chainId == "eip155:8453" && it.sufficient && it.signingRequest != null 
        } ?: throw IllegalStateException("USDC on Base not available or insufficient balance.")
        
        Log.d("PaymentRepository", "Selected option: ${selectedOption.symbol} on ${selectedOption.chain}, method=${selectedOption.signingRequest?.method}")
        
        // Step 4: Parse typed data from the signing request params (for eth_signTypedData_v4)
        val typedData: PaymentTypedDataPayload?
        val typedDataJson: String?
        
        if (selectedOption.signingRequest?.method == "eth_signTypedData_v4") {
            val typedDataElement = selectedOption.signingRequest.params.getOrNull(1)
                ?: throw IllegalStateException("Missing typed data payload in signing request")
            
            // The typed data can be either a JSON string or a JSON object
            typedDataJson = if (typedDataElement.isJsonPrimitive && typedDataElement.asJsonPrimitive.isString) {
                // It's a stringified JSON - use the string content directly
                typedDataElement.asString
            } else {
                // It's a JSON object - convert to string
                typedDataElement.toString()
            }
            typedData = gson.fromJson(typedDataJson, PaymentTypedDataPayload::class.java)
            Log.d("PaymentRepository", "Parsed typedData primaryType=${typedData.primaryType}")
        } else {
            // For other methods (eth_sendTransaction, solana_signTransaction), store raw params
            typedDataJson = selectedOption.signingRequest?.params?.toString()
            typedData = null
        }
        
        val session = PaymentSession(
            paymentId = paymentId,
            info = paymentInfo,
            selectedOption = selectedOption,
            typedData = typedData,
            typedDataJson = typedDataJson
        )
        currentPayment = session
        return session
    }

    /**
     * Submits a payment with ERC-3009 authorization signature.
     */
    suspend fun submitPayment(paymentId: String, authorization: PaymentAuthorization, asset: String): SubmitPaymentResponse {
        Log.d("PaymentRepository", "submitPayment id=$paymentId asset=$asset value=${authorization.value}")
        
        // Build JSON with explicit field order to match expected format (same as Swift/viem)
        // Order: from, to, value, validAfter, validBefore, nonce, v, r, s
        val signatureJson = buildString {
            append("{")
            append("\"from\":\"${authorization.from}\",")
            append("\"to\":\"${authorization.to}\",")
            append("\"value\":\"${authorization.value}\",")
            append("\"validAfter\":${authorization.validAfter},")
            append("\"validBefore\":${authorization.validBefore},")
            append("\"nonce\":\"${authorization.nonce}\",")
            append("\"v\":${authorization.v},")
            append("\"r\":\"${authorization.r}\",")
            append("\"s\":\"${authorization.s}\"")
            append("}")
        }
        Log.d("PaymentRepository", "submitPayment signatureJson=$signatureJson")
        
        return apiService.submitPayment(
            SubmitPaymentRequest(
                paymentId = paymentId,
                signature = signatureJson,
                asset = asset
            )
        ).also { 
            Log.d("PaymentRepository", "submitPayment result status=${it.status} txHash=${it.txHash} chainName=${it.chainName} error=${it.error}") 
        }
    }

    fun clearPayment() {
        Log.d("PaymentRepository", "clearPayment")
        currentPayment = null
    }
}

/**
 * Payment session containing all information needed for the payment flow.
 */
data class PaymentSession(
    val paymentId: String,
    val info: PaymentInfoResponse,
    val selectedOption: PaymentOption,
    val typedData: PaymentTypedDataPayload?,       // Parsed typed data (for eth_signTypedData_v4)
    val typedDataJson: String?                     // Raw JSON for signing
)
