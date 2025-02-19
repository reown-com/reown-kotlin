package com.reown.sample.wallet.ui.routes.dialog_routes.transaction

import androidx.lifecycle.ViewModel
import com.reown.sample.wallet.domain.EthAccountDelegate
import com.reown.sample.wallet.domain.model.Transaction
import com.reown.sample.wallet.ui.routes.dialog_routes.transaction.TokenAddresses.getAddressOn
import com.reown.walletkit.client.WalletKit

class TransactionViewModel : ViewModel() {
    fun getBalance(chain: Chain, token: StableCoin): String {
        return try {
            val balance = WalletKit.getERC20Balance(chain.id, token.getAddressOn(chain), EthAccountDelegate.address)
            Transaction.hexToTokenAmount(balance, 6)?.toPlainString() ?: "-.--"
        } catch (e: Exception) {
            println("getERC20Balance error: $e")
            "-.--"
        }
    }
}
