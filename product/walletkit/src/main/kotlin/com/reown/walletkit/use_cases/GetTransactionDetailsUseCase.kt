package com.reown.walletkit.use_cases

import com.reown.android.internal.common.scope
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.toYttrium
import com.reown.walletkit.client.toWallet
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.Currency
import uniffi.yttrium.UiFields

class GetTransactionDetailsUseCase(private val chainAbstractionClient: ChainAbstractionClient) {
    operator fun invoke(
        available: Wallet.Model.FulfilmentSuccess.Available,
        onSuccess: (Wallet.Model.TransactionsDetails) -> Unit,
        onError: (Wallet.Model.Error) -> Unit
    ) {
        scope.launch {
            try {
                val result = async {
                    try {
                        chainAbstractionClient.getUiFields(available.toYttrium(), Currency.USD)
                    } catch (e: Exception) {
                        return@async onError(Wallet.Model.Error(e))
                    }
                }.await()

                if (result is UiFields) {
                    onSuccess((result).toWallet())
                }

            } catch (e: Exception) {
                onError(Wallet.Model.Error(e))
            }
        }
    }
}