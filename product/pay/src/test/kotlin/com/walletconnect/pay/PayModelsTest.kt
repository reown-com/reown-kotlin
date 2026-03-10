package com.walletconnect.pay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PayModelsTest {

    @Test
    fun `SdkConfig should hold correct values`() {
        val config = Pay.SdkConfig(
            apiKey = "test-api-key",
            packageName = "com.test.app"
        )

        assertEquals("test-api-key", config.apiKey)
        assertEquals("com.test.app", config.packageName)
    }

    @Test
    fun `PaymentStatus enum should have all required values`() {
        val statuses = Pay.PaymentStatus.entries.toTypedArray()

        assertEquals(5, statuses.size)
        assertNotNull(Pay.PaymentStatus.valueOf("REQUIRES_ACTION"))
        assertNotNull(Pay.PaymentStatus.valueOf("PROCESSING"))
        assertNotNull(Pay.PaymentStatus.valueOf("SUCCEEDED"))
        assertNotNull(Pay.PaymentStatus.valueOf("FAILED"))
        assertNotNull(Pay.PaymentStatus.valueOf("EXPIRED"))
    }

    @Test
    fun `AmountDisplay should hold correct values`() {
        val display = Pay.AmountDisplay(
            assetSymbol = "ETH",
            assetName = "Ethereum",
            decimals = 18,
            iconUrl = "https://example.com/eth.png",
            networkName = "Ethereum Mainnet",
            networkIconUrl = null
        )

        assertEquals("ETH", display.assetSymbol)
        assertEquals("Ethereum", display.assetName)
        assertEquals(18, display.decimals)
        assertEquals("https://example.com/eth.png", display.iconUrl)
        assertEquals("Ethereum Mainnet", display.networkName)
    }

    @Test
    fun `AmountDisplay should handle null optional fields`() {
        val display = Pay.AmountDisplay(
            assetSymbol = "USDC",
            assetName = "USD Coin",
            decimals = 6,
            iconUrl = null,
            networkName = null,
            networkIconUrl = null
        )

        assertEquals("USDC", display.assetSymbol)
        assertNull(display.iconUrl)
        assertNull(display.networkName)
    }

    @Test
    fun `Amount should hold correct values with display`() {
        val display = Pay.AmountDisplay(
            assetSymbol = "USDC",
            assetName = "USD Coin",
            decimals = 6,
            iconUrl = null,
            networkName = null,
            networkIconUrl = null
        )

        val amount = Pay.Amount(
            value = "1000000",
            unit = "USDC",
            display = display
        )

        assertEquals("1000000", amount.value)
        assertEquals("USDC", amount.unit)
        assertNotNull(amount.display)
        assertEquals("USDC", amount.display?.assetSymbol)
    }

    @Test
    fun `PaymentOption should hold correct values`() {
        val amount = Pay.Amount(
            value = "100",
            unit = "USDC",
            display = null
        )

        val option = Pay.PaymentOption(
            id = "option-123",
            amount = amount,
            estimatedTxs = 2,
            account = ""
        )

        assertEquals("option-123", option.id)
        assertEquals("100", option.amount.value)
        assertEquals(2, option.estimatedTxs)
    }

    @Test
    fun `MerchantInfo should hold correct values`() {
        val merchant = Pay.MerchantInfo(
            name = "Test Merchant",
            iconUrl = "https://example.com/merchant.png"
        )

        assertEquals("Test Merchant", merchant.name)
        assertEquals("https://example.com/merchant.png", merchant.iconUrl)
    }

    @Test
    fun `PaymentInfo should hold correct values`() {
        val amount = Pay.Amount(value = "100", unit = "USD", display = null)
        val merchant = Pay.MerchantInfo(name = "Merchant", iconUrl = null)

        val info = Pay.PaymentInfo(
            status = Pay.PaymentStatus.REQUIRES_ACTION,
            amount = amount,
            expiresAt = 1234567890L,
            merchant = merchant
        )

        assertEquals(Pay.PaymentStatus.REQUIRES_ACTION, info.status)
        assertEquals(1234567890L, info.expiresAt)
        assertEquals("Merchant", info.merchant.name)
    }

    @Test
    fun `WalletRpcAction should hold correct values`() {
        val action = Pay.WalletRpcAction(
            chainId = "eip155:1",
            method = "eth_sendTransaction",
            params = """{"to":"0x123","value":"0x100"}"""
        )

        assertEquals("eip155:1", action.chainId)
        assertEquals("eth_sendTransaction", action.method)
        assertEquals("""{"to":"0x123","value":"0x100"}""", action.params)
    }

    @Test
    fun `RequiredAction WalletRpc should wrap WalletRpcAction correctly`() {
        val walletAction = Pay.WalletRpcAction(
            chainId = "eip155:1",
            method = "eth_signTypedData_v4",
            params = "{}"
        )

        val action = Pay.RequiredAction.WalletRpc(walletAction)

        assertEquals("eip155:1", action.action.chainId)
        assertEquals("eth_signTypedData_v4", action.action.method)
    }

    @Test
    fun `CollectDataFieldType enum should have all required values`() {
        val types = Pay.CollectDataFieldType.entries.toTypedArray()

        assertEquals(3, types.size)
        assertNotNull(Pay.CollectDataFieldType.valueOf("TEXT"))
        assertNotNull(Pay.CollectDataFieldType.valueOf("DATE"))
        assertNotNull(Pay.CollectDataFieldType.valueOf("CHECKBOX"))
    }

    @Test
    fun `CollectDataField should hold correct values`() {
        val field = Pay.CollectDataField(
            id = "field-1",
            name = "Email",
            fieldType = Pay.CollectDataFieldType.TEXT,
            required = true
        )

        assertEquals("field-1", field.id)
        assertEquals("Email", field.name)
        assertEquals(Pay.CollectDataFieldType.TEXT, field.fieldType)
        assertEquals(true, field.required)
    }

    @Test
    fun `CollectDataFieldResult should hold correct values`() {
        val result = Pay.CollectDataFieldResult(
            id = "field-1",
            value = "test@example.com"
        )

        assertEquals("field-1", result.id)
        assertEquals("test@example.com", result.value)
    }

    @Test
    fun `ConfirmPaymentResponse should hold correct values`() {
        val response = Pay.ConfirmPaymentResponse(
            status = Pay.PaymentStatus.PROCESSING,
            isFinal = false,
            pollInMs = 5000L,
            info = null
        )

        assertEquals(Pay.PaymentStatus.PROCESSING, response.status)
        assertEquals(false, response.isFinal)
        assertEquals(5000L, response.pollInMs)
        assertNull(response.info)
    }

    @Test
    fun `ConfirmPaymentResponse with final status`() {
        val amount = Pay.Amount(value = "100", unit = "USD", display = null)
        val resultInfo = Pay.PaymentResultInfo(
            txId = "0x123abc",
            optionAmount = amount
        )
        val response = Pay.ConfirmPaymentResponse(
            status = Pay.PaymentStatus.SUCCEEDED,
            isFinal = true,
            pollInMs = null,
            info = resultInfo
        )

        assertEquals(Pay.PaymentStatus.SUCCEEDED, response.status)
        assertEquals(true, response.isFinal)
        assertNull(response.pollInMs)
        assertNotNull(response.info)
        assertEquals("0x123abc", response.info?.txId)
        assertEquals("100", response.info?.optionAmount?.value)
    }

    @Test
    fun `PaymentResultInfo should hold correct values`() {
        val amount = Pay.Amount(value = "50", unit = "USDC", display = null)
        val resultInfo = Pay.PaymentResultInfo(
            txId = "tx-456",
            optionAmount = amount
        )

        assertEquals("tx-456", resultInfo.txId)
        assertEquals("50", resultInfo.optionAmount.value)
        assertEquals("USDC", resultInfo.optionAmount.unit)
    }

    @Test
    fun `CollectDataAction should hold correct values`() {
        val fields = listOf(
            Pay.CollectDataField(
                id = "email",
                name = "Email Address",
                fieldType = Pay.CollectDataFieldType.TEXT,
                required = true
            ),
            Pay.CollectDataField(
                id = "dob",
                name = "Date of Birth",
                fieldType = Pay.CollectDataFieldType.DATE,
                required = false
            ),
            Pay.CollectDataField(
                id = "terms",
                name = "Accept Terms",
                fieldType = Pay.CollectDataFieldType.CHECKBOX,
                required = true
            )
        )

        val action = Pay.CollectDataAction(
            fields = fields,
            url = "https://example.com/ic-form",
            schema = "ic-schema-v1"
        )

        assertEquals(3, action.fields.size)
        assertEquals("email", action.fields[0].id)
        assertEquals("dob", action.fields[1].id)
        assertEquals("terms", action.fields[2].id)
        assertEquals(Pay.CollectDataFieldType.TEXT, action.fields[0].fieldType)
        assertEquals(Pay.CollectDataFieldType.DATE, action.fields[1].fieldType)
        assertEquals(Pay.CollectDataFieldType.CHECKBOX, action.fields[2].fieldType)
        assertEquals("https://example.com/ic-form", action.url)
        assertEquals("ic-schema-v1", action.schema)
    }

    @Test
    fun `CollectDataAction should handle null url and schema`() {
        val fields = listOf(
            Pay.CollectDataField(
                id = "email",
                name = "Email",
                fieldType = Pay.CollectDataFieldType.TEXT,
                required = true
            )
        )

        val action = Pay.CollectDataAction(
            fields = fields,
            url = null,
            schema = null
        )

        assertEquals(1, action.fields.size)
        assertNull(action.url)
        assertNull(action.schema)
    }

    @Test
    fun `PaymentOptionsResponse should hold correct values with all fields`() {
        val amount = Pay.Amount(value = "100", unit = "USD", display = null)
        val merchant = Pay.MerchantInfo(name = "Test Store", iconUrl = "https://example.com/icon.png")
        val info = Pay.PaymentInfo(
            status = Pay.PaymentStatus.REQUIRES_ACTION,
            amount = amount,
            expiresAt = 1704067200L,
            merchant = merchant
        )

        val options = listOf(
            Pay.PaymentOption(id = "opt-1", amount = amount, estimatedTxs = 1, account = ""),
            Pay.PaymentOption(id = "opt-2", amount = amount, estimatedTxs = 2, account = "")
        )

        val collectDataAction = Pay.CollectDataAction(
            fields = listOf(
                Pay.CollectDataField(
                    id = "email",
                    name = "Email",
                    fieldType = Pay.CollectDataFieldType.TEXT,
                    required = true
                )
            ),
            url = null,
            schema = null
        )

        val response = Pay.PaymentOptionsResponse(
            info = info,
            options = options,
            paymentId = "pay-123",
            collectDataAction = collectDataAction
        )

        assertEquals("pay-123", response.paymentId)
        assertEquals(2, response.options.size)
        assertNotNull(response.info)
        assertEquals(Pay.PaymentStatus.REQUIRES_ACTION, response.info?.status)
        assertNotNull(response.collectDataAction)
        assertEquals(1, response.collectDataAction?.fields?.size)
    }

    @Test
    fun `PaymentOptionsResponse should handle null optional fields`() {
        val amount = Pay.Amount(value = "50", unit = "USDC", display = null)
        val options = listOf(
            Pay.PaymentOption(id = "opt-1", amount = amount, estimatedTxs = null, account = "")
        )

        val response = Pay.PaymentOptionsResponse(
            info = null,
            options = options,
            paymentId = "pay-456",
            collectDataAction = null
        )

        assertEquals("pay-456", response.paymentId)
        assertNull(response.info)
        assertNull(response.collectDataAction)
        assertEquals(1, response.options.size)
        assertNull(response.options[0].estimatedTxs)
    }

    @Test
    fun `PaymentOption should handle null estimatedTxs`() {
        val amount = Pay.Amount(value = "25", unit = "ETH", display = null)

        val option = Pay.PaymentOption(
            id = "option-null-tx",
            amount = amount,
            estimatedTxs = null,
            account = ""
        )

        assertEquals("option-null-tx", option.id)
        assertNull(option.estimatedTxs)
    }

    @Test
    fun `MerchantInfo should handle null iconUrl`() {
        val merchant = Pay.MerchantInfo(
            name = "No Icon Store",
            iconUrl = null
        )

        assertEquals("No Icon Store", merchant.name)
        assertNull(merchant.iconUrl)
    }
}
