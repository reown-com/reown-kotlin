package com.reown.sample.wallet.ui.routes.dialog_routes.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.android.Core
import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.domain.EthAccountDelegate
import com.reown.sample.wallet.domain.SolanaAccountDelegate
import com.reown.sample.wallet.domain.WCDelegate
import com.reown.sample.wallet.domain.emitChainAbstractionRequest
import com.reown.sample.wallet.domain.getErrorMessage
import com.reown.sample.wallet.domain.mixPanel
import com.reown.sample.wallet.domain.model.Transaction
import com.reown.sample.wallet.ui.routes.dialog_routes.transaction.TokenAddresses.getAddressOn
import com.reown.walletkit.client.ChainAbstractionExperimentalApi
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.math.BigDecimal

sealed class UIState {
    data object Ide : UIState()
    data object Loading : UIState()
    data object NotRequired : UIState()
    data object NavigateToCA : UIState()
    data class Error(val message: String) : UIState()
}

class TransactionViewModel : ViewModel() {
    private val _balanceState = MutableStateFlow<Map<Pair<Chain, Token>, String>>(emptyMap())
    val balanceState = _balanceState.asStateFlow()

    private val _uiState = MutableStateFlow<UIState>(UIState.Ide)
    val uiState = _uiState.asStateFlow()

    init {
        refreshBalances()
    }

    @OptIn(ChainAbstractionExperimentalApi::class)
    fun sendTransaction(chain: Chain, token: Token, amount: String, to: String, from: String) {
        try {
            val initialTransaction = when (token) {
                is StableCoin -> {
                    val hexAmount = stringToTokenHex(amount, token.decimals)
                    _uiState.value = UIState.Loading

                    val transferCall = WalletKit.prepareErc20TransferCall(
                        contractAddress = token.getAddressOn(chain),
                        to = to,
                        amount = hexAmount
                    )

                    Wallet.Model.InitialTransaction(
                        from = from,
                        to = transferCall.to,
                        chainId = chain.id,
                        input = transferCall.input,
                        value = transferCall.value,
                    )
                }

                is Coin -> {
                    val hexAmount = stringToTokenHex(amount, token.decimals)
                    _uiState.value = UIState.Loading

                    Wallet.Model.InitialTransaction(
                        from = from,
                        to = to,
                        chainId = chain.id,
                        input = "0x",
                        value = hexAmount,
                    )
                }

                else -> {
                    throw Exception("Unknown token")
                }
            }

            println("initial tx: $initialTransaction")

            WalletKit.ChainAbstraction.prepare(
                initialTransaction,
                listOf(SolanaAccountDelegate.keys.third),
                onSuccess = { result ->
                    when (result) {
                        is Wallet.Model.PrepareSuccess.Available -> {
                            println("Prepare success available: $result")
                            val sessionRequest = Wallet.Model.SessionRequest(
                                topic = "",
                                chainId = chain.id,
                                request = Wallet.Model.SessionRequest.JSONRPCRequest(
                                    id = 0,
                                    method = "eth_sendTransaction",
                                    params = ""
                                ),
                                peerMetaData = Core.Model.AppMetaData(
                                    name = "Kotlin Wallet",
                                    description = "Kotlin Wallet Implementation",
                                    url = "https://appkit-lab.reown.com",
                                    redirect = "",
                                    icons = listOf("https://raw.githubusercontent.com/WalletConnect/walletconnect-assets/master/Icon/Gradient/Icon.png"),
                                )
                            )
                            val verifyContext = Wallet.Model.VerifyContext(
                                id = 0,
                                origin = "kotlin wallet",
                                validation = Wallet.Model.Validation.VALID,
                                isScam = false,
                                verifyUrl = ""
                            )
                            emitChainAbstractionRequest(sessionRequest, result, verifyContext)
                        }

                        is Wallet.Model.PrepareSuccess.NotRequired -> {
                            println("Prepare success not required: $result")
                            //todo: handle that tx execution
                            _uiState.value = UIState.NotRequired
                        }
                    }
                },
                onError = { error ->
                    WCDelegate.prepareError = error
                    println("Prepare success not required: ${getErrorMessage()}")
                    recordError(Throwable("Prepare error: ${getErrorMessage()}"))
                    _uiState.value = UIState.Error("Prepare error: $error")
                }
            )
        } catch (e: Exception) {
            recordError(e)
            _uiState.value = UIState.Error("Error: ${e.message}")
        }
    }

    private fun refreshBalances() {
        viewModelScope.launch {
            Chain.entries.forEach { chain ->
                when (chain.id) {
                    Chain.ETHEREUM.id, Chain.BASE.id, Chain.ARBITRUM.id, Chain.OPTIMISM.id -> {
                        withContext(Dispatchers.IO) {
                            val balance = getBalance(
                                chain.id,
                                EthAccountDelegate.address,
                                Coin.ETH.name
                            )

                            val formattedBalance =
                                Transaction.hexToTokenAmount(balance, Coin.ETH.decimals)
                                    ?.toPlainString() ?: "0"
                            _balanceState.update { currentState ->
                                currentState + (Pair(chain, Coin.ETH) to formattedBalance)
                            }
                        }
                    }

                    Chain.SOLANA.id -> {
                        withContext(Dispatchers.IO) {
                            val balance = getBalance(
                                chain.id,
                                SolanaAccountDelegate.keys.second,
                                StableCoin.USDC.name
                            )

                            val formattedBalance =
                                Transaction.hexToTokenAmount(balance, StableCoin.USDC.decimals)
                                    ?.toPlainString() ?: "0"
                            _balanceState.update { currentState ->
                                currentState + (Pair(chain, StableCoin.USDC) to formattedBalance)
                            }
                        }
                    }
                }

                StableCoin.entries
                    .filter { chain != Chain.SOLANA }
                    .forEach { token ->
                        viewModelScope.launch {
                            try {
                                val balance =
                                    withContext(Dispatchers.IO) {
                                        WalletKit.getERC20Balance(
                                            chain.id,
                                            token.getAddressOn(chain),
                                            EthAccountDelegate.address
                                        )
                                    }
                                if (balance.isEmpty()) {
                                    _balanceState.update { currentState ->
                                        currentState + (Pair(chain, token) to "-.--")
                                    }
                                } else {
                                    val formattedBalance =
                                        Transaction.hexToTokenAmount(balance, token.decimals)
                                            ?.toPlainString() ?: "0"
                                    _balanceState.update { currentState ->
                                        currentState + (Pair(chain, token) to formattedBalance)
                                    }
                                }
                            } catch (e: Exception) {
                                recordError(e)
                                _balanceState.update { currentState ->
                                    currentState + (Pair(chain, token) to "-.--")
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun getBalance(
        chainId: String,
        ownerAddress: String,
        tokenSymbol: String
    ): String {
        val client = OkHttpClient()
        // Build the URL with path parameters and query parameters
        val urlBuilder =
            "https://rpc.walletconnect.com/v1/account/${ownerAddress}/balance".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("currency", "usd")
                ?.addQueryParameter("projectId", BuildConfig.PROJECT_ID)
                ?.addQueryParameter("chainId", chainId)
                ?.addQueryParameter("sv", "reown-kotlin-${BuildConfig.BOM_VERSION}")

        val url = urlBuilder?.build() ?: throw IllegalArgumentException("Invalid URL")

        // Create request with headers
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        // Execute the request
        val response = client.newCall(request).execute()

        return if (response.isSuccessful && response.body != null) {
            val responseBody = response.body!!.string()
            val jsonObject = JSONObject(responseBody)

            // Get the balances array
            val balancesArray = jsonObject.getJSONArray("balances")
            println("balances: $balancesArray")
            // Iterate through the array to find the matching token address
            for (i in 0 until balancesArray.length()) {
                val token = balancesArray.getJSONObject(i)
                if (token.getString("symbol") == tokenSymbol) {
                    val numeric = token.getJSONObject("quantity").getString("numeric")
                    return numeric
                }
            }
            println("Token not found for address: $tokenSymbol")
            ""
        } else {
            println("Error: ${response.body}")
            ""
        }
    }

    private fun stringToTokenHex(amount: String, decimals: Int): String {
        return try {
            val multiplier = BigDecimal.TEN.pow(decimals)
            val withDecimals = amount.toBigDecimal().multiply(multiplier)
            val hex = withDecimals.toBigInteger().toString(16)
            "0x$hex"
        } catch (e: Exception) {
            "0x0"
        }
    }

    private fun recordError(throwable: Throwable) {
        mixPanel.track("error: $throwable; errorMessage: ${throwable.message}")
        Firebase.crashlytics.recordException(throwable)
    }
}
