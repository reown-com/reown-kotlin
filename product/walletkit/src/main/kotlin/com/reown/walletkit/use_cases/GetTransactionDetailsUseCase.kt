package com.reown.walletkit.use_cases

import com.reown.walletkit.client.Wallet
import uniffi.uniffi_yttrium.ChainAbstractionClient

class GetTransactionDetailsUseCase(private val chainAbstractionClient: ChainAbstractionClient) {
    operator fun invoke(
        available: Wallet.Model.FulfilmentSuccess.Available,
        initTransaction: Wallet.Model.Transaction,
        onSuccess: (Wallet.Model.RouteUiFields) -> Unit,
        onError: (Wallet.Model.Error) -> Unit
    ) {
//        scope.launch {
//            try {
//                val result = async { chainAbstractionClient.getUiFields(available.toYttrium(), initTransaction.toYttrium(), Currency.USD) }.await()
//                onSuccess(result.toWallet())
//            } catch (e: Exception) {
//                onError(Wallet.Model.Error(e))
//            }
//        }
    }
}