package com.reown.sample.wallet.ui.routes.dialog_routes.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.sample.wallet.domain.EthAccountDelegate
import com.reown.sample.wallet.domain.model.Transaction
import com.reown.sample.wallet.ui.routes.dialog_routes.transaction.TokenAddresses.getAddressOn
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionViewModel : ViewModel() {
    private val _balanceState = MutableStateFlow<Map<Pair<Chain, StableCoin>, String>>(emptyMap())
    val balanceState = _balanceState.asStateFlow()

    init {
        refreshBalances()
    }

    private fun refreshBalances() {
        viewModelScope.launch {
            // Initialize with loading state for all combinations
            val initialState = Chain.values().flatMap { chain ->
                StableCoin.entries.map { token ->
                    Pair(chain, token) to "-.--"
                }
            }.toMap()
            _balanceState.value = initialState

            // Fetch each balance concurrently
            Chain.entries.forEach { chain ->
                StableCoin.entries.forEach { token ->
                    viewModelScope.launch {
                        try {
                            val balance = withContext(Dispatchers.IO) {
                                WalletKit.getERC20Balance(
                                    chain.id,
                                    token.getAddressOn(chain),
                                    EthAccountDelegate.address
                                )
                            }
                            val formattedBalance = Transaction.hexToTokenAmount(balance, 6)?.toPlainString() ?: "0"
                            _balanceState.update { currentState ->
                                currentState + (Pair(chain, token) to formattedBalance)
                            }
                        } catch (e: Exception) {
                            println("getERC20Balance error for $chain $token: $e")
                            _balanceState.update { currentState ->
                                currentState + (Pair(chain, token) to "-.--")
                            }
                        }
                    }
                }
            }
        }
    }

    fun getBalance(chain: Chain, token: StableCoin): String {
        return _balanceState.value[Pair(chain, token)] ?: "-.--"
    }

//    fun getBalance(chain: Chain, token: StableCoin): String {
//        return try {
//            val balance = WalletKit.getERC20Balance(chain.id, token.getAddressOn(chain), EthAccountDelegate.address)
//            Transaction.hexToTokenAmount(balance, 6)?.toPlainString() ?: ""
//        } catch (e: Exception) {
//            println("getERC20Balance error: $e")
//            "-.--"
//        }
//    }
}
