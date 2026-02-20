package com.reown.sample.wallet.ui.routes.composable_routes.connections

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.blockchain.TokenBalance
import com.reown.sample.wallet.blockchain.createBalanceApiService
import com.reown.sample.wallet.domain.WalletKitDelegate
import com.reown.sample.wallet.domain.account.ACCOUNTS_1_EIP155_ADDRESS
import com.reown.sample.wallet.domain.account.ACCOUNTS_2_EIP155_ADDRESS
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConnectionsViewModel : ViewModel() {
    private var _refreshFlow: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 0, extraBufferCapacity = 1, BufferOverflow.DROP_OLDEST)
    private var refreshFlow: SharedFlow<Unit> = _refreshFlow.asSharedFlow()
    private val signConnectionsFlow = merge(WalletKitDelegate.walletEvents, refreshFlow).map {
        Log.d("Web3Wallet", "signConnectionsFlow: $it")
        getLatestActiveSignSessions()
    }
    var displayedAccounts: List<String> = emptyList()

    // USDC Balance state
    private val _usdcBalances = MutableStateFlow<List<TokenBalance>>(emptyList())
    val usdcBalances: StateFlow<List<TokenBalance>> = _usdcBalances.asStateFlow()

    // EUROC Balance state
    private val _eurocBalances = MutableStateFlow<List<TokenBalance>>(emptyList())
    val eurocBalances: StateFlow<List<TokenBalance>> = _eurocBalances.asStateFlow()

    private val _isLoadingBalances = MutableStateFlow(false)
    val isLoadingBalances: StateFlow<Boolean> = _isLoadingBalances.asStateFlow()

    private val balanceApiService = createBalanceApiService()

    init {
        fetchBalances()
    }

    fun fetchBalances() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingBalances.value = true
            try {
                val address = EthAccountDelegate.address
                Log.d("Web3Wallet", "Fetching balances for address: $address")
                val response = balanceApiService.getBalance(
                    address = address,
                    projectId = BuildConfig.PROJECT_ID
                )
                Log.d("Web3Wallet", "Balance API response code: ${response.code()}")
                if (response.isSuccessful) {
                    val balances = response.body()?.balances ?: emptyList()
                    Log.d("Web3Wallet", "All balances received: ${balances.size} items")
                    balances.forEach { balance ->
                        Log.d("Web3Wallet", "Balance: ${balance.symbol} on ${balance.chainId} = ${balance.quantity.numeric}")
                    }
                    // Filter for USDC on Ethereum, Polygon, and Base
                    val usdcBalances = balances.filter { balance ->
                        balance.symbol == "USDC" && balance.chainId in listOf(
                            "eip155:1",     // Ethereum Mainnet
                            "eip155:137",   // Polygon
                            "eip155:8453",  // Base
                            "eip155:10"     // Optimism
                        )
                    }
                    _usdcBalances.value = usdcBalances
                    Log.d("Web3Wallet", "Filtered USDC balances: $usdcBalances")

                    // Filter for EURC on Ethereum and Base (not deployed on Polygon)
                    val eurocBalances = balances.filter { balance ->
                        balance.symbol == "EURC" && balance.chainId in listOf(
                            "eip155:1",     // Ethereum Mainnet
                            "eip155:8453",  // Base
                            "eip155:10"     // Optimism
                        )
                    }
                    _eurocBalances.value = eurocBalances
                    Log.d("Web3Wallet", "Filtered EURC balances: $eurocBalances")
                } else {
                    Log.e("Web3Wallet", "Failed to fetch balances: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("Web3Wallet", "Error fetching balances", e)
            } finally {
                _isLoadingBalances.value = false
            }
        }
    }

    var currentConnectionId: Int? = null
        set(value) {
            field = value
            refreshCurrentConnectionUI()
        }

    private fun getConnectionUI(): ConnectionUI? = connections.value.firstOrNull { it.id == currentConnectionId }

    val connections: StateFlow<List<ConnectionUI>> =
        signConnectionsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, getLatestActiveSignSessions())

    val currentConnectionUI: MutableState<ConnectionUI?> = mutableStateOf(getConnectionUI())

    // Refreshes connections list from Web3Wallet
    fun refreshConnections() {
        val res = _refreshFlow.tryEmit(Unit)
        Log.e("Web3Wallet", "refreshConnections $res")
    }

    private var areNewAccounts: Boolean = true

    fun getAccountsToChange(): String {
        return if (areNewAccounts) {
            areNewAccounts = false
            "[\"${"eip155:1:$ACCOUNTS_2_EIP155_ADDRESS"}\",\"${"eip155:137:$ACCOUNTS_2_EIP155_ADDRESS"}\",\"${"eip155:56:$ACCOUNTS_2_EIP155_ADDRESS"}\"]"
        } else {
            areNewAccounts = true
            "[\"${"eip155:1:$ACCOUNTS_1_EIP155_ADDRESS"}\",\"${"eip155:137:$ACCOUNTS_1_EIP155_ADDRESS"}\",\"${"eip155:56:$ACCOUNTS_1_EIP155_ADDRESS"}\"]"
        }
    }

    private fun refreshCurrentConnectionUI() {
        currentConnectionUI.value = getConnectionUI()
    }

    private fun getLatestActiveSignSessions(): List<ConnectionUI> {
        return try {
            WalletKit.getListOfActiveSessions().filter { wcSession ->
                wcSession.metaData != null
            }.mapIndexed { index, wcSession ->
                ConnectionUI(
                    icon = wcSession.metaData?.icons?.firstOrNull(),
                    name = wcSession.metaData!!.name.takeIf { it.isNotBlank() } ?: "Dapp",
                    uri = wcSession.metaData!!.url.takeIf { it.isNotBlank() } ?: "Not provided",
                    id = index,
                    type = ConnectionType.Sign(topic = wcSession.topic, namespaces = wcSession.namespaces),
                )
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }
}