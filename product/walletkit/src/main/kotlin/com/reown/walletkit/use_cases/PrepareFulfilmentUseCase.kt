package com.reown.walletkit.use_cases

import com.reown.android.internal.common.scope
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.toWallet
import com.reown.walletkit.client.toYttrium
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.BridgingError
import uniffi.yttrium.RouteResponse
import uniffi.yttrium.RouteResponseSuccess

class PrepareFulfilmentUseCase(private val chainAbstractionClient: ChainAbstractionClient) {
    operator fun invoke(
        transaction: Wallet.Model.Transaction,
        onSuccess: (Wallet.Model.FulfilmentSuccess) -> Unit,
        onError: (Wallet.Model.FulfilmentError) -> Unit
    ) {
        scope.launch {
            try {
                val result = async { chainAbstractionClient.route(transaction.toYttrium()) }.await()

                when (result) {
                    is RouteResponse.Success -> {
                        when (result.v1) {
                            is RouteResponseSuccess.Available ->
                                onSuccess((result.v1 as RouteResponseSuccess.Available).v1.toWallet())

                            is RouteResponseSuccess.NotRequired ->
                                onSuccess(Wallet.Model.FulfilmentSuccess.NotRequired((result.v1 as RouteResponseSuccess.NotRequired).v1.initialTransaction.toWallet()))
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