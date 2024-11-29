package com.reown.walletkit.use_cases

import com.reown.android.internal.common.scope
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit.response
import com.reown.walletkit.client.toWallet
import com.reown.walletkit.client.toYttrium
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.BridgingError
import uniffi.yttrium.RouteResponse
import uniffi.yttrium.RouteResponseSuccess

//todo: test me
class CanFulfilUseCase(private val chainAbstractionClient: ChainAbstractionClient) {
    operator fun invoke(
        transaction: Wallet.Model.Transaction,
        onSuccess: (Wallet.Model.FulfilmentSuccess) -> Unit,
        onError: (Wallet.Model.FulfilmentError) -> Unit
    ) {
        scope.launch {
            try {
                println("kobe: ${transaction.toYttrium()}")
                val result = async {
                    try {
                        chainAbstractionClient.route(transaction.toYttrium())
                    } catch (e: Exception) {
                        onError(Wallet.Model.FulfilmentError.Unknown(e.message ?: "Unknown error"))
                    }
                }.await()

                when (result) {
                    is RouteResponse.Success -> {
                        when (result.v1) {
                            is RouteResponseSuccess.Available -> {
                                response = result.v1.v1
                                onSuccess(result.v1.v1.toWallet())
                            }
                            is RouteResponseSuccess.NotRequired -> onSuccess(Wallet.Model.FulfilmentSuccess.NotRequired)
                        }
                    }

                    is RouteResponse.Error -> {
                        when (result.v1.error) {
                            BridgingError.NO_ROUTES_AVAILABLE -> onError(Wallet.Model.FulfilmentError.NoRoutesAvailable)
                            BridgingError.INSUFFICIENT_FUNDS -> onError(Wallet.Model.FulfilmentError.InsufficientFunds)
                            BridgingError.INSUFFICIENT_GAS_FUNDS -> onError(Wallet.Model.FulfilmentError.InsufficientGasFunds)
                        }
                    }
                }
            } catch (e: Exception) {
                onError(Wallet.Model.FulfilmentError.Unknown(e.message ?: "Unknown error"))
            }
        }

    }
}