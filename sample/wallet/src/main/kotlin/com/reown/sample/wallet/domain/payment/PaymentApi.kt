package com.reown.sample.wallet.domain.payment

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PaymentApi {
    /**
     * GET /getPaymentInfo/:paymentId
     * Retrieves payment details for display (pre-wallet connect).
     * Shows what assets are supported but no signing requests.
     */
    @GET("getPaymentInfo/{paymentId}")
    suspend fun getPaymentInfo(@Path("paymentId") paymentId: String): PaymentInfoResponse

    /**
     * POST /buildPayment
     * Builds payment options for the user's connected wallet(s).
     * Returns signing requests for all viable options based on balances.
     * - Fetches balances for provided accounts
     * - Generates quotes for native/volatile tokens
     * - Builds signing requests for each asset with sufficient balance
     */
    @POST("buildPayment")
    suspend fun buildPayment(@Body request: BuildPaymentRequest): BuildPaymentResponse

    /**
     * POST /submit
     * Submits the signed transaction to execute the payment.
     */
    @POST("submit")
    suspend fun submitPayment(@Body request: SubmitPaymentRequest): SubmitPaymentResponse
}
