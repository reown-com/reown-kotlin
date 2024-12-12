package com.reown.walletkit.use_cases

import com.reown.android.internal.common.scope
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.toCAYttrium
import com.reown.walletkit.client.toWallet
import com.reown.walletkit.client.toYttrium
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.Currency

class GetTransactionDetailsUseCase(private val chainAbstractionClient: ChainAbstractionClient) {
    operator fun invoke(
        available: Wallet.Model.FulfilmentSuccess.Available,
        initTransaction: Wallet.Model.Transaction,
        currency: Wallet.Model.Currency,
        onSuccess: (Wallet.Model.RouteUiFields) -> Unit,
        onError: (Wallet.Model.Error) -> Unit
    ) {
        scope.launch {
            try {
                val result = async { chainAbstractionClient.getRouteUiFields(available.toYttrium(), initTransaction.toCAYttrium(), currency.toYttrium()) }.await()
                onSuccess(result.toWallet())
            } catch (e: Exception) {
                onError(Wallet.Model.Error(e))
            }
        }
    }

    private companion object {
        fun Wallet.Model.Currency.toYttrium(): Currency {
            return when (this) {
                Wallet.Model.Currency.USD -> Currency.USD
                Wallet.Model.Currency.EUR -> Currency.EUR
                Wallet.Model.Currency.GBP -> Currency.GBP
                Wallet.Model.Currency.AUD -> Currency.AUD
                Wallet.Model.Currency.CAD -> Currency.CAD
                Wallet.Model.Currency.INR -> Currency.INR
                Wallet.Model.Currency.JPY -> Currency.JPY
                Wallet.Model.Currency.BTC -> Currency.BTC
                Wallet.Model.Currency.ETH -> Currency.ETH
            }
        }
    }
}