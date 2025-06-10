package com.reown.walletkit.use_cases

import com.reown.android.internal.common.scope
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.reown.walletkit.client.toWallet
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.BridgingError
import uniffi.yttrium.Call
import uniffi.yttrium.Currency
import uniffi.yttrium.PrepareDetailedResponse
import uniffi.yttrium.PrepareDetailedResponseSuccess
import uniffi.yttrium.solanaGenerateKeypair

class PrepareChainAbstractionUseCase(private val chainAbstractionClient: ChainAbstractionClient) {
    operator fun invoke(
        initialTransaction: Wallet.Model.InitialTransaction,
        accounts: List<String>,
        onSuccess: (Wallet.Model.PrepareSuccess) -> Unit,
        onError: (Wallet.Model.PrepareError) -> Unit
    ) {
        scope.launch {
            try {
                val result = async {
                    try {
                        val call = Call(to = initialTransaction.to, value = initialTransaction.value, input = initialTransaction.input)
                        solanaGenerateKeypair()
                        chainAbstractionClient.prepareDetailed(
                            initialTransaction.chainId,
                            initialTransaction.from,
                            call,
                            accounts,
                            Currency.USD,
                            useLifi = false
                        )
                    } catch (e: Exception) {
                        return@async onError(Wallet.Model.PrepareError.Unknown(e.message ?: "Unknown error"))
                    }
                }.await()

                when (result) {
                    is PrepareDetailedResponse.Success -> {
                        when (result.v1) {
                            is PrepareDetailedResponseSuccess.Available ->
                                onSuccess((result.v1 as PrepareDetailedResponseSuccess.Available).v1.toWallet())

                            is PrepareDetailedResponseSuccess.NotRequired ->
                                onSuccess(Wallet.Model.PrepareSuccess.NotRequired((result.v1 as PrepareDetailedResponseSuccess.NotRequired).v1.initialTransaction.toWallet()))
                        }
                    }

                    is PrepareDetailedResponse.Error -> {
                        when (result.v1.error) {
                            BridgingError.NO_ROUTES_AVAILABLE -> onError(Wallet.Model.PrepareError.NoRoutesAvailable(result.v1.reason))
                            BridgingError.INSUFFICIENT_FUNDS -> onError(Wallet.Model.PrepareError.InsufficientFunds(result.v1.reason))
                            BridgingError.INSUFFICIENT_GAS_FUNDS -> onError(Wallet.Model.PrepareError.InsufficientGasFunds(result.v1.reason))
                            BridgingError.ASSET_NOT_SUPPORTED -> onError(Wallet.Model.PrepareError.AssetNotSupported(result.v1.reason))
                            BridgingError.UNKNOWN -> onError(Wallet.Model.PrepareError.Unknown("Unknown"))
                            BridgingError.TRANSACTION_SIMULATION_FAILED -> TODO()
                        }
                    }
                }
            } catch (e: Exception) {
                onError(Wallet.Model.PrepareError.Unknown(e.message ?: "Unknown error"))
            }
        }
    }
}