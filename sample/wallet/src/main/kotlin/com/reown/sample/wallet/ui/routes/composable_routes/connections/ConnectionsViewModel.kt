package com.reown.sample.wallet.ui.routes.composable_routes.connections

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.blockchain.TokenBalance
import com.reown.sample.wallet.blockchain.createBalanceApiService
import com.reown.sample.wallet.blockchain.isSpam
import com.reown.sample.wallet.blockchain.processBalances
import com.reown.sample.wallet.domain.WalletKitDelegate
import com.reown.sample.wallet.domain.account.ACCOUNTS_1_EIP155_ADDRESS
import com.reown.sample.wallet.domain.account.ACCOUNTS_2_EIP155_ADDRESS
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.account.SolanaAccountDelegate
import com.reown.sample.wallet.domain.account.SuiAccountDelegate
import com.reown.sample.wallet.domain.account.TONAccountDelegate
import com.reown.sample.wallet.domain.account.TronAccountDelegate
import com.reown.sample.wallet.ui.routes.dialog_routes.transaction.Chain
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    private val _balances = MutableStateFlow<List<TokenBalance>>(emptyList())
    val balances: StateFlow<List<TokenBalance>> = _balances.asStateFlow()

    private val _isLoadingBalances = MutableStateFlow(false)
    val isLoadingBalances: StateFlow<Boolean> = _isLoadingBalances.asStateFlow()

    private val balanceApiService = createBalanceApiService()

    // Non-EVM addresses (Solana/Sui/TON/Tron) derived once, keyed by CAIP-2
    // namespace. Deriving them is crypto/FFI work that must not run per render or
    // per refresh; the underlying keys only change on wallet import, which
    // recreates this view model.
    private val nonEvmAddresses: Map<String, String> = buildMap {
        runCatching { SolanaAccountDelegate.getSolanaPubKeyForKeyPair() }.getOrNull()
            ?.takeIf { it.isNotBlank() }?.let { put("solana", it) }
        runCatching { SuiAccountDelegate.address }.getOrNull()
            ?.takeIf { it.isNotBlank() }?.let { put("sui", it) }
        runCatching { TONAccountDelegate.addressFriendly }.getOrNull()
            ?.takeIf { it.isNotBlank() }?.let { put("ton", it) }
        runCatching { TronAccountDelegate.address }.getOrNull()
            ?.takeIf { it.isNotBlank() }?.let { put("tron", it) }
    }

    init {
        fetchBalances()
    }

    /** Address shown and copied for a balance row, by the token's CAIP-2 namespace. */
    fun addressFor(chainId: String): String =
        nonEvmAddresses[chainId.substringBefore(":")] ?: EthAccountDelegate.address

    fun fetchBalances() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingBalances.value = true
            try {
                val evmAddress = EthAccountDelegate.address
                // EVM with no chainId returns all EVM tokens; each non-EVM chain
                // is fetched with its own address + chainId.
                val nonEvmTargets = buildList {
                    nonEvmAddresses["solana"]?.let { add(it to Chain.SOLANA.id) }
                    nonEvmAddresses["sui"]?.let { add(it to Chain.SUI.id) }
                    nonEvmAddresses["ton"]?.let { add(it to TONAccountDelegate.mainnet) }
                    nonEvmAddresses["tron"]?.let { add(it to TronAccountDelegate.mainnet) }
                }

                val deferred = buildList {
                    add(async { fetchChainBalances(evmAddress, null) })
                    nonEvmTargets.forEach { (address, chainId) ->
                        add(async { fetchChainBalances(address, chainId) })
                    }
                }
                val results = deferred.awaitAll()

                if (results.all { it == null }) {
                    Log.e("Web3Wallet", "All balance fetches failed; keeping current balances")
                    return@launch
                }

                val apiBalances = results.filterNotNull().flatten().filterNot { isSpam(it) }
                val availableNativeChainIds = buildSet {
                    add("eip155:1")
                    nonEvmTargets.forEach { (_, chainId) -> add(chainId) }
                }
                _balances.value = processBalances(apiBalances, availableNativeChainIds)
            } catch (e: Exception) {
                Log.e("Web3Wallet", "Error fetching balances", e)
            } finally {
                _isLoadingBalances.value = false
            }
        }
    }

    // Returns the balances for a single call, or null if the request failed (so
    // one chain's failure doesn't drop the others).
    private suspend fun fetchChainBalances(address: String, chainId: String?): List<TokenBalance>? =
        runCatching {
            val response = balanceApiService.getBalance(
                address = address,
                projectId = BuildConfig.PROJECT_ID,
                chainId = chainId
            )
            if (response.isSuccessful) {
                val balances = response.body()?.balances ?: emptyList()
                Log.d("Web3Wallet", "Balances for ${chainId ?: "evm"} ($address): ${balances.size}")
                balances
            } else {
                Log.e("Web3Wallet", "Failed to fetch balances for ${chainId ?: "evm"}: ${response.code()}")
                null
            }
        }.getOrElse {
            Log.e("Web3Wallet", "Error fetching balances for ${chainId ?: "evm"}", it)
            null
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