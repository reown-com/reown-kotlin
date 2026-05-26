package com.reown.sample.wallet.payment

import com.reown.walletkit.client.Wallet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentSelectionResolverTest {

    @Test
    fun `resolve should send a single option without collect data to summary`() {
        val option = paymentOption(id = "option-1", unit = "eip155:137/usdc")

        val route = PaymentSelectionResolver.resolve(
            options = listOf(option),
            lastPaidTokenUnit = null,
        )

        assertEquals(InitialPaymentDestination.SUMMARY, route.destination)
        assertEquals(option, route.selectedOption)
    }

    @Test
    fun `resolve should prefer the last paid token when it is available`() {
        val optionA = paymentOption(id = "option-1", unit = "token-a")
        val optionB = paymentOption(id = "option-2", unit = "token-b")

        val route = PaymentSelectionResolver.resolve(
            options = listOf(optionA, optionB),
            lastPaidTokenUnit = "token-b",
        )

        assertEquals(InitialPaymentDestination.SUMMARY, route.destination)
        assertEquals(optionB, route.selectedOption)
    }

    @Test
    fun `resolve should keep token selection when preferred option still needs collect data`() {
        val option = paymentOption(
            id = "option-1",
            unit = "token-a",
            collectData = Wallet.Model.CollectDataAction(
                fields = emptyList(),
                url = "https://example.com",
                schema = null,
            ),
        )

        val route = PaymentSelectionResolver.resolve(
            options = listOf(paymentOption(id = "option-0", unit = "token-z"), option),
            lastPaidTokenUnit = "token-a",
        )

        assertEquals(InitialPaymentDestination.OPTIONS, route.destination)
        assertNull(route.selectedOption)
    }

    @Test
    fun `resolve should keep token selection when there is no preferred match`() {
        val route = PaymentSelectionResolver.resolve(
            options = listOf(
                paymentOption(id = "option-1", unit = "token-a"),
                paymentOption(id = "option-2", unit = "token-b"),
            ),
            lastPaidTokenUnit = "token-c",
        )

        assertEquals(InitialPaymentDestination.OPTIONS, route.destination)
        assertNull(route.selectedOption)
    }

    private fun paymentOption(
        id: String,
        unit: String,
        collectData: Wallet.Model.CollectDataAction? = null,
    ): Wallet.Model.PaymentOption {
        return Wallet.Model.PaymentOption(
            id = id,
            amount = Wallet.Model.PaymentAmount(
                value = "100",
                unit = unit,
                display = null,
            ),
            account = "eip155:137:0xabc",
            estimatedTxs = 1,
            actions = emptyList(),
            collectData = collectData,
        )
    }
}
