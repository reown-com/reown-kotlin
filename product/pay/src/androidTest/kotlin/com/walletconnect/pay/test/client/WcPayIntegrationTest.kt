package com.walletconnect.pay.test.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.walletconnect.pay.Pay
import com.walletconnect.pay.WalletConnectPay
import com.walletconnect.pay.test.scenario.PayClientInstrumentedActivityScenario
import com.walletconnect.pay.test.utils.AmountRequest
import com.walletconnect.pay.test.utils.Common
import com.walletconnect.pay.test.utils.CreatePaymentRequest
import com.walletconnect.pay.test.utils.CreatePaymentResponse
import com.walletconnect.pay.test.utils.TestClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WcPayIntegrationTest {

    @get:Rule
    val scenarioExtension = PayClientInstrumentedActivityScenario()

    /**
     * Full payment flow test:
     * 1. POS creates payment via API
     * 2. Wallet gets payment options
     * 3. Wallet gets required actions
     * 4. Wallet signs actions
     * 5. Wallet confirms payment
     * 6. Assert payment succeeds or is processing
     */
    @Test
    fun fullPaymentFlow_posCreatesPayment_walletPays_succeeds() = runBlocking {
        // Step 1: POS creates payment ($1.00 USD)
        val referenceId = "test-${System.currentTimeMillis()}"
        val createResponse = TestClient.posApi.createPayment(
            CreatePaymentRequest(
                referenceId = referenceId,
                amount = AmountRequest(unit = "iso4217/USD", value = "100")
            )
        )
        assertTrue("Failed to create payment: ${createResponse.errorBody()?.string()}", createResponse.isSuccessful)
        val payment = createResponse.body()!!
        println("Payment created: ${payment.paymentId}, gatewayUrl: ${payment.gatewayUrl}")

        // Step 2: Wallet gets payment options
        val optionsResult = WalletConnectPay.getPaymentOptions(
            paymentLink = payment.gatewayUrl,
            accounts = Common.testAccounts
        )
        assertTrue("Failed to get payment options: ${optionsResult.exceptionOrNull()}", optionsResult.isSuccess)
        val options = optionsResult.getOrThrow()
        println("Payment options received: ${options.options.size} options")
        assertTrue("No payment options available", options.options.isNotEmpty())

        // Step 3: Wallet gets required actions
        val selectedOption = options.options.first()
        println("Selected option: ${selectedOption.id}")

        val actionsResult = WalletConnectPay.getRequiredPaymentActions(
            paymentId = options.paymentId,
            optionId = selectedOption.id
        )
        assertTrue("Failed to get required actions: ${actionsResult.exceptionOrNull()}", actionsResult.isSuccess)
        val actions = actionsResult.getOrThrow()
        println("Required actions: ${actions.size}")

        // Step 4: Sign all required actions
        val signatures = actions.map { action ->
            when (action) {
                is Pay.RequiredAction.WalletRpc -> signAction(action.action)
            }
        }
        println("Signatures generated: ${signatures.size}")

        // Step 5: Confirm payment with signatures
        val confirmResult = WalletConnectPay.confirmPayment(
            paymentId = options.paymentId,
            optionId = selectedOption.id,
            signatures = signatures,
            collectedData = options.collectDataAction?.fields?.map { field ->
                Pay.CollectDataFieldResult(
                    id = field.id,
                    value = when (field.fieldType) {
                        Pay.CollectDataFieldType.TEXT -> "test@example.com"
                        Pay.CollectDataFieldType.DATE -> "1990-01-01"
                    }
                )
            }
        )
        assertTrue("Failed to confirm payment: ${confirmResult.exceptionOrNull()}", confirmResult.isSuccess)

        val confirmation = confirmResult.getOrThrow()
        println("Payment confirmation status: ${confirmation.status}")

        // Step 6: Assert payment succeeded or is processing
        assertTrue(
            "Unexpected payment status: ${confirmation.status}",
            confirmation.status == Pay.PaymentStatus.SUCCEEDED || confirmation.status == Pay.PaymentStatus.PROCESSING
        )
    }

    /**
     * Test: Invalid payment link should return error
     */
    @Test
    fun getPaymentOptions_invalidLink_returnsError() = runBlocking {
        val result = WalletConnectPay.getPaymentOptions(
            paymentLink = "https://pay.walletconnect.com/pay_invalid_123456789",
            accounts = Common.testAccounts
        )

        assertTrue("Expected failure for invalid payment link", result.isFailure)
        val error = result.exceptionOrNull()
        println("Error for invalid link: $error")
        assertTrue(
            "Expected GetPaymentOptionsError but got: ${error?.javaClass?.simpleName}",
            error is Pay.GetPaymentOptionsError
        )
    }

    /**
     * Test: Invalid signatures should fail payment confirmation
     */
    @Test
    fun confirmPayment_invalidSignatures_fails() = runBlocking {
        // Create payment
        val payment = createTestPayment()

        // Get payment options
        val optionsResult = WalletConnectPay.getPaymentOptions(
            paymentLink = payment.gatewayUrl,
            accounts = Common.testAccounts
        )
        assertTrue("Failed to get payment options", optionsResult.isSuccess)
        val options = optionsResult.getOrThrow()

        // Get required actions to know how many signatures we need
        val actionsResult = WalletConnectPay.getRequiredPaymentActions(
            paymentId = options.paymentId,
            optionId = options.options.first().id
        )
        assertTrue("Failed to get required actions", actionsResult.isSuccess)
        val actions = actionsResult.getOrThrow()

        // Submit invalid signatures (all zeros)
        val invalidSignatures = actions.map { "0x" + "0".repeat(130) }

        val result = WalletConnectPay.confirmPayment(
            paymentId = options.paymentId,
            optionId = options.options.first().id,
            signatures = invalidSignatures
        )

        assertTrue("Expected failure for invalid signatures", result.isFailure)
        println("Error for invalid signatures: ${result.exceptionOrNull()}")
    }

    /**
     * Test: POS status polling tracks payment progress
     */
    @Test
    fun posStatusPolling_tracksPaymentProgress() = runBlocking {
        val payment = createTestPayment()
        var lastStatus = ""
        var pollCount = 0

        // Poll status until final or timeout
        withTimeout(60_000) {
            while (pollCount < 30) {
                val statusResponse = TestClient.posApi.getPaymentStatus(payment.paymentId)
                if (statusResponse.isSuccessful) {
                    val status = statusResponse.body()!!
                    lastStatus = status.status
                    println("Poll $pollCount: status=$lastStatus, isFinal=${status.isFinal}")

                    if (status.isFinal) {
                        println("Payment reached final status: $lastStatus")
                        break
                    }
                    delay(status.pollInMs ?: 2000)
                } else {
                    println("Status poll failed: ${statusResponse.errorBody()?.string()}")
                }
                pollCount++
            }
        }

        println("Final status after $pollCount polls: $lastStatus")
        assertTrue(
            "Unexpected final status: $lastStatus",
            lastStatus in listOf("requires_action", "processing", "succeeded", "failed", "expired")
        )
    }

    // Helper function to sign wallet RPC actions
    private fun signAction(action: Pay.WalletRpcAction): String {
        return when (action.method) {
            "eth_signTypedData_v4" -> {
                val params = JSONArray(action.params)
                val typedData = params.getString(1)
                TestClient.signer.signTypedDataV4(typedData)
            }
            "personal_sign" -> {
                val params = JSONArray(action.params)
                val message = params.getString(0)
                TestClient.signer.personalSign(message)
            }
            else -> {
                println("Unsupported signing method: ${action.method}")
                throw UnsupportedOperationException("Unsupported method: ${action.method}")
            }
        }
    }

    // Helper function to create a test payment
    private suspend fun createTestPayment(): CreatePaymentResponse {
        val referenceId = "test-${System.currentTimeMillis()}"
        val response = TestClient.posApi.createPayment(
            CreatePaymentRequest(
                referenceId = referenceId,
                amount = AmountRequest(unit = "iso4217/USD", value = "100")
            )
        )
        assertTrue("Failed to create test payment: ${response.errorBody()?.string()}", response.isSuccessful)
        return response.body()!!
    }
}
