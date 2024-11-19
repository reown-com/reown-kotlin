package com.reown.walletkit.use_cases

import com.reown.android.internal.common.scope
import com.reown.walletkit.client.Wallet
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.StatusResponse
import uniffi.yttrium.StatusResponseSuccess

//todo: test me
class FulfilmentStatusUseCase(private val chainAbstractionClient: ChainAbstractionClient) {
    //todo: remove on error success, keep pulling inside the method?
    operator fun invoke(
        fulfilmentId: String,
        onSuccess: (Wallet.Model.FulfilmentStatus) -> Unit,
        onError: (Wallet.Model.Error) -> Unit
    ) {
        scope.launch {
            try {
                val result = async {
                    try {
                        println("kobe: fulfilmentId: $fulfilmentId")
                        chainAbstractionClient.status(fulfilmentId)
                    } catch (e: Exception) {
                        onError(Wallet.Model.Error(e))
                    }
                }.await()

                println("kobe: WK Status result: $result")

                when (result) {
                    is StatusResponse.Success -> {
                        when (result.v1) {
                            is StatusResponseSuccess.Completed -> {
                                println("kobe: success: completed")
                                onSuccess(Wallet.Model.FulfilmentStatus.Completed(result.v1.v1.createdAt.toLong()))
                            }

                            is StatusResponseSuccess.Error -> {
                                println("kobe: success: error: ${result.v1.v1.errorReason}")
                                onSuccess(Wallet.Model.FulfilmentStatus.Error(result.v1.v1.createdAt.toLong(), result.v1.v1.errorReason))
                            }

                            is StatusResponseSuccess.Pending -> {
                                println("kobe: success: pending")
                                onSuccess(Wallet.Model.FulfilmentStatus.Pending(result.v1.v1.createdAt.toLong(), result.v1.v1.checkIn.toLong()))
                            }
                        }
                    }

                    is StatusResponse.Error -> {
                        println("kobe: error: ${result.v1.error}")
                        onError(Wallet.Model.Error(Throwable(result.v1.error)))
                    }
                }
            } catch (e: Exception) {
                onError(Wallet.Model.Error(e))
            }
        }
    }
}