@file:JvmSynthetic

package com.reown.sample.wallet.payment

import com.reown.walletkit.client.Wallet

internal object PaymentUtil {

    private const val ETH_SEND_TRANSACTION = "eth_sendTransaction"

    fun getApprovalAction(actions: List<Wallet.Model.RequiredAction>?): Wallet.Model.RequiredAction.WalletRpc? {
        return actions
            ?.filterIsInstance<Wallet.Model.RequiredAction.WalletRpc>()
            ?.firstOrNull { it.action.method == ETH_SEND_TRANSACTION }
    }

    fun requiresApproval(actions: List<Wallet.Model.RequiredAction>?): Boolean {
        return getApprovalAction(actions) != null
    }

    fun shouldShowSetupLoader(actions: List<Wallet.Model.RequiredAction>?): Boolean {
        return (actions?.size ?: 0) > 1 && requiresApproval(actions)
    }
}
