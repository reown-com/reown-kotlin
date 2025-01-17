package com.reown.walletkit.use_cases

import com.reown.android.internal.common.scope
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.toWallet
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.BridgingError
import uniffi.yttrium.Call
import uniffi.yttrium.PrepareResponse
import uniffi.yttrium.PrepareResponseSuccess

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
                        val call = Call(initialTransaction.to, initialTransaction.value, initialTransaction.input)
                        chainAbstractionClient.prepare(initialTransaction.chainId, initialTransaction.from, call)
                    } catch (e: Exception) {
                        return@async onError(Wallet.Model.PrepareError.Unknown(e.message ?: "Unknown error"))
                    }
                }.await()

                when (result) {
                    is PrepareResponse.Success -> {
                        when (result.v1) {
                            is PrepareResponseSuccess.Available ->
                                onSuccess((result.v1 as PrepareResponseSuccess.Available).v1.toWallet())

                            is PrepareResponseSuccess.NotRequired ->
                                onSuccess(Wallet.Model.PrepareSuccess.NotRequired((result.v1 as PrepareResponseSuccess.NotRequired).v1.initialTransaction.toWallet()))
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