package com.reown.sample.dapp.ui.routes.composable_routes.account

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.sample.common.Chains
import com.reown.sample.common.getEthSendTransaction
import com.reown.sample.common.getEthSignBody
import com.reown.sample.common.getEthSignTypedData
import com.reown.sample.common.getPersonalSignBody
import com.reown.sample.dapp.domain.DappDelegate
import com.reown.sample.dapp.ui.DappSampleEvents
import com.reown.sample.dapp.ui.accountArg
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import com.reown.appkit.client.models.Session
import com.reown.appkit.client.models.request.Request
import com.reown.sample.common.getGetWalletAssetsParams
import com.reown.sample.common.getSolanaSignAndSendParams
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AccountViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val selectedAccountInfo = checkNotNull(savedStateHandle.get<String>(accountArg))

    private val _uiState: MutableStateFlow<AccountUi> = MutableStateFlow(AccountUi.Loading)
    val uiState: StateFlow<AccountUi> = _uiState.asStateFlow()

    private val _awaitResponse: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val awaitResponse: StateFlow<Boolean> = _awaitResponse.asStateFlow()

    private val _events: MutableSharedFlow<DappSampleEvents> = MutableSharedFlow()
    val events: SharedFlow<DappSampleEvents>
        get() = _events.asSharedFlow()

    init {
        DappDelegate.wcEventModels
            .filterNotNull()
            .onEach { walletEvent ->
                when (walletEvent) {
                    is Modal.Model.UpdatedSession -> fetchAccountDetails()
                    is Modal.Model.SessionRequestResponse -> {
                        val request = when (walletEvent.result) {
                            is Modal.Model.JsonRpcResponse.JsonRpcResult -> {
                                _awaitResponse.value = false
                                val successResult = (walletEvent.result as Modal.Model.JsonRpcResponse.JsonRpcResult)
                                DappSampleEvents.RequestSuccess((successResult.result ?: "No result") as String)
                            }

                            is Modal.Model.JsonRpcResponse.JsonRpcError -> {
                                _awaitResponse.value = false
                                val errorResult = (walletEvent.result as Modal.Model.JsonRpcResponse.JsonRpcError)
                                DappSampleEvents.RequestPeerError("Error Message: ${errorResult.message}\n Error Code: ${errorResult.code}")
                            }
                        }

                        _events.emit(request)
                    }

                    is Modal.Model.ExpiredRequest -> {
                        _awaitResponse.value = false
                        _events.emit(DappSampleEvents.RequestError("Request expired"))
                    }

                    is Modal.Model.DeletedSession -> {
                        _events.emit(DappSampleEvents.Disconnect)
                    }

                    else -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    fun requestMethod(method: String, sendSessionRequestDeepLink: (Uri) -> Unit) {
        (uiState.value as? AccountUi.AccountData)?.let { currentState ->
            try {
                _awaitResponse.value = true

                val (_, _, account) = currentState.selectedAccount.split(":")
                val params: String = when {
                    method.equals("personal_sign", true) -> getPersonalSignBody(account)
                    method.equals("wallet_getAssets", true) -> getGetWalletAssetsParams(account)
                    method.equals("eth_sign", true) -> getEthSignBody(account)
                    method.equals("eth_sendTransaction", true) -> getEthSendTransaction(account)
                    method.equals("eth_signTypedData", true) -> getEthSignTypedData(account)
                    method.equals("solana_signAndSendTransaction", true) -> getSolanaSignAndSendParams()
                    else -> "[]"
                }
                val requestParams = Request(
                    method = method,
                    params = params, // stringified JSON
                )

                AppKit.request(requestParams,
                    onSuccess = { _ ->
                        println("AppKit request success: $method")
                    },
                    onError = {
                        viewModelScope.launch {
                            _awaitResponse.value = false
                            _events.emit(DappSampleEvents.RequestError(it.localizedMessage ?: "Error trying to send request"))
                        }
                    })
            } catch (e: Exception) {
                viewModelScope.launch {
                    _awaitResponse.value = false
                    _events.emit(DappSampleEvents.RequestError(e.localizedMessage ?: "Error trying to send request"))
                }
            }
        }
    }

    fun fetchAccountDetails() {
        val (chainNamespace, chainReference, account) = selectedAccountInfo.split(":")
        val chainDetails = Chains.values().first {
            it.chainNamespace == chainNamespace && it.chainReference == chainReference
        }

        val listOfMethodsByChainId: List<String> =
            (AppKit.getSession() as Session.WalletConnectSession).namespaces
                .filter { (namespaceKey, _) -> namespaceKey == chainDetails.chainId }
                .flatMap { (_, namespace) -> namespace.methods }


        val listOfMethodsByNamespace: List<String> =
            (AppKit.getSession() as Session.WalletConnectSession).namespaces
                .filter { (namespaceKey, _) -> namespaceKey == chainDetails.chainNamespace }
                .flatMap { (_, namespace) -> namespace.methods }

        viewModelScope.launch {
            _uiState.value = AccountUi.AccountData(
                icon = chainDetails.icon,
                chainName = chainDetails.chainName,
                account = account,
                listOfMethods = listOfMethodsByChainId.ifEmpty { listOfMethodsByNamespace },
                selectedAccount = selectedAccountInfo
            )
        }
    }
}