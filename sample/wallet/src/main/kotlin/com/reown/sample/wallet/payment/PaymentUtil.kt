@file:JvmSynthetic

package com.reown.sample.wallet.payment

import com.reown.walletkit.client.Wallet

internal object PaymentUtil {

    private const val ETH_SEND_TRANSACTION = "eth_sendTransaction"

    data class PaymentContext(
        val approvalAction: Wallet.Model.RequiredAction.WalletRpc?,
    ) {
        val requiresApproval: Boolean get() = approvalAction != null
    }

    fun getPaymentContext(actions: List<Wallet.Model.RequiredAction>?): PaymentContext {
        val approval = actions
            ?.filterIsInstance<Wallet.Model.RequiredAction.WalletRpc>()
            ?.firstOrNull { it.action.method == ETH_SEND_TRANSACTION }
        return PaymentContext(approvalAction = approval)
    }
}
