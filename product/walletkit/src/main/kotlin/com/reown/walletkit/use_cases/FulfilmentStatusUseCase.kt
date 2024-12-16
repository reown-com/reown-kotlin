package com.reown.walletkit.use_cases

import com.reown.android.internal.common.scope
import com.reown.walletkit.client.Wallet
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.StatusResponse

class FulfilmentStatusUseCase(private val chainAbstractionClient: ChainAbstractionClient) {
    operator fun invoke(
        fulfilmentId: String,
        checkIn: Long,
        onSuccess: (Wallet.Model.FulfilmentStatus.Completed) -> Unit,
        onError: (Wallet.Model.FulfilmentStatus.Error) -> Unit
    ) {
        scope.launch {
            withTimeout(FULFILMENT_TIMEOUT) {
                delay(checkIn)

                while (true) {
                    try {
                        println("kobe: fulfilmentId: $fulfilmentId")
                        val result = async { chainAbstractionClient.status(fulfilmentId) }.await()

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
                        println("kobe: Catch status WK: $e")
                        onError(Wallet.Model.FulfilmentStatus.Error(e.message ?: "Unknown error"))
                        break
                    }
                }
            }
        }
    }

    private companion object {
        const val FULFILMENT_TIMEOUT = 180000L
    }
}