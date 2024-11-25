package com.reown.walletkit.use_cases

import com.reown.android.internal.common.scope
import com.reown.walletkit.client.Wallet
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.StatusResponse
import uniffi.yttrium.StatusResponseCompleted
import uniffi.yttrium.StatusResponseError
import uniffi.yttrium.StatusResponsePending

//todo: test me
class FulfilmentStatusUseCase(private val chainAbstractionClient: ChainAbstractionClient) {
    operator fun invoke(
        fulfilmentId: String,
        checkIn: Long,
        onSuccess: (Wallet.Model.FulfilmentStatus.Completed) -> Unit,
        onError: (Wallet.Model.FulfilmentStatus.Error) -> Unit
    ) {
        //todo: add timeout
        scope.launch {
            supervisorScope {
                println("kobe: start checkin: $checkIn")
                delay(checkIn)

                while (true) {
                    try {
                        val result = async {
                            println("kobe: fulfilmentId: $fulfilmentId")
                            chainAbstractionClient.status(fulfilmentId)
                        }.await()

                        println("kobe: WK Status result: $result")
                        when (result) {
                            is StatusResponse.Completed -> {
                                println("kobe: success: completed")
                                onSuccess(Wallet.Model.FulfilmentStatus.Completed(result.v1.createdAt.toLong()))
                                break
                            }

                            is StatusResponse.Error -> {
                                println("kobe: success: error: ${result.v1.error}")
                                onError(Wallet.Model.FulfilmentStatus.Error(result.v1.error))
                                break
                            }

                            is StatusResponse.Pending -> {
                                println("kobe: success: pending: ${result.v1.checkIn.toLong()}")
                                delay(result.v1.checkIn.toLong())
                            }
                        }
                    } catch (e: Exception) {
                        onError(Wallet.Model.FulfilmentStatus.Error(e.message ?: "Unknown error"))
                    }
                }
            }
        }
    }
}