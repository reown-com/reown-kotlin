package com.reown.walletkit.use_cases

import com.reown.android.internal.common.scope
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.toInitialYttrium
import com.reown.walletkit.client.toWallet
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.BridgingError
import uniffi.yttrium.PrepareResponse
import uniffi.yttrium.RouteResponseSuccess

class PrepareChainAbstractionUseCase(private val chainAbstractionClient: ChainAbstractionClient) {
    operator fun invoke(
        initialTransaction: Wallet.Model.InitialTransaction,
        onSuccess: (Wallet.Model.PrepareSuccess) -> Unit,
        onError: (Wallet.Model.PrepareError) -> Unit
    ) {
        scope.launch {
            try {
                val result = async {
                    try {
                        chainAbstractionClient.prepare(initialTransaction.toInitialYttrium())
                    } catch (e: Exception) {
                        return@async onError(Wallet.Model.PrepareError.Unknown(e.message ?: "Unknown error"))
                    }
                }.await()

                when (result) {
                    is PrepareResponse.Success -> {
                        when (result.v1) {
                            is RouteResponseSuccess.Available ->
                                onSuccess((result.v1 as RouteResponseSuccess.Available).v1.toWallet())

                            is RouteResponseSuccess.NotRequired ->
                                onSuccess(Wallet.Model.PrepareSuccess.NotRequired((result.v1 as RouteResponseSuccess.NotRequired).v1.initialTransaction.toWallet()))
                        }
                    }

                    is PrepareResponse.Error -> {
                        when (result.v1.error) {
                            BridgingError.NO_ROUTES_AVAILABLE -> onError(Wallet.Model.PrepareError.NoRoutesAvailable)
                            BridgingError.INSUFFICIENT_FUNDS -> onError(Wallet.Model.PrepareError.InsufficientFunds)
                            BridgingError.INSUFFICIENT_GAS_FUNDS -> onError(Wallet.Model.PrepareError.InsufficientGasFunds)
                        }
                    }
                }
            } catch (e: Exception) {
                onError(Wallet.Model.PrepareError.Unknown(e.message ?: "Unknown error"))
            }
        }
    }
}