package com.reown.walletkit.use_cases

import com.reown.android.internal.common.scope
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.toWallet
import com.reown.walletkit.client.toYttrium
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.ExecuteDetails

class ExecuteChainAbstractionUseCase(private val chainAbstractionClient: ChainAbstractionClient) {
    operator fun invoke(
        prepareAvailable: Wallet.Model.PrepareSuccess.Available,
        signedRouteTxs: List<Wallet.Model.RouteSig>,
        initSignedTx: String,
        onSuccess: (Wallet.Model.ExecuteSuccess) -> Unit,
        onError: (Wallet.Model.Error) -> Unit
    ) {
        scope.launch {
            try {

                val result = async {
                    try {
                        chainAbstractionClient.execute(prepareAvailable.toYttrium(), signedRouteTxs.map { it.toYttrium() }, initSignedTx)
                    } catch (e: Exception) {
                        return@async onError(Wallet.Model.Error(e))
                    }
                }.await()

                if (result is ExecuteDetails) {
                    onSuccess((result).toWallet())
                }

            } catch (e: Exception) {
                onError(Wallet.Model.Error(e))
            }
        }
    }
}