package com.reown.sample.wallet.payment

import com.reown.walletkit.client.Wallet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentUtilTest {

    @Test
    fun `getApprovalAction should return the approval action when present`() {
        val approval = walletRpcAction("eth_sendTransaction")
        val signature = walletRpcAction("eth_signTypedData_v4")

        val approvalAction = PaymentUtil.getApprovalAction(listOf(approval, signature))

        assertNotNull(approvalAction)
        assertEquals("eth_sendTransaction", approvalAction?.action?.method)
    }

    @Test
    fun `requiresApproval should be false for signature-only options`() {
        val signature = walletRpcAction("eth_signTypedData_v4")

        assertFalse(PaymentUtil.requiresApproval(listOf(signature)))
    }

    @Test
    fun `shouldShowSetupLoader should be true for multi-step approval flows`() {
        val approval = walletRpcAction("eth_sendTransaction")
        val signature = walletRpcAction("eth_signTypedData_v4")

        assertTrue(PaymentUtil.shouldShowSetupLoader(listOf(approval, signature)))
    }

    @Test
    fun `shouldShowSetupLoader should be false for single-step approval flows`() {
        val approval = walletRpcAction("eth_sendTransaction")

        assertFalse(PaymentUtil.shouldShowSetupLoader(listOf(approval)))
    }

    private fun walletRpcAction(method: String): Wallet.Model.RequiredAction.WalletRpc {
        return Wallet.Model.RequiredAction.WalletRpc(
            action = Wallet.Model.WalletRpcAction(
                chainId = "eip155:137",
                method = method,
                params = "[]",
            ),
        )
    }
}
