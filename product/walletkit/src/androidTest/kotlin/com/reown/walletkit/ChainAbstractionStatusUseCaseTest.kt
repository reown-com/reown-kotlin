package com.reown.walletkit

import com.reown.walletkit.client.Wallet
import com.reown.walletkit.use_cases.ChainAbstractionStatusUseCase
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Test
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.StatusResponse
import uniffi.yttrium.StatusResponseCompleted
import uniffi.yttrium.StatusResponseError
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
class ChainAbstractionStatusUseCaseTest {
    private val chainAbstractionClient: ChainAbstractionClient = mockk()
    private val chainAbstractionStatusUseCase = ChainAbstractionStatusUseCase(chainAbstractionClient)

    @Test
    fun shouldCallOnSuccessWhenStatusIsCompleted() = runTest {
        val fulfilmentId = "123"
        val checkIn = 1000L
        val completedResult = StatusResponse.Completed(StatusResponseCompleted(createdAt = 1u))

        coEvery { chainAbstractionClient.status(fulfilmentId) } returns completedResult

        val result = async {
            suspendCoroutine { continuation ->
                chainAbstractionStatusUseCase.invoke(
                    fulfilmentId,
                    checkIn,
                    onSuccess = {
                        continuation.resume(true)
                    },
                    onError = {
                        continuation.resume(false)
                    }
                )
            }
        }.await()

        assertTrue(result)
    }

    @Test
    fun shouldCallOnErrorWhenStatusIsError() = runTest {
        val fulfilmentId = "123"
        val checkIn = 1000L
        val errorResult = StatusResponse.Error(StatusResponseError(createdAt = 1u, error = "error"))

        coEvery { chainAbstractionClient.status(fulfilmentId) } returns errorResult

        val result = async {
            suspendCoroutine { continuation ->
                chainAbstractionStatusUseCase.invoke(
                    fulfilmentId,
                    checkIn,
                    onSuccess = {
                        continuation.resume(true)
                    },
                    onError = {
                        continuation.resume(it)
                    }
                )
            }
        }.await()

        assertTrue(result is Wallet.Model.FulfilmentStatus.Error)
    }

    @Test
    fun shouldCallOnErrorWhenErrorIsThrown() = runTest {
        val fulfilmentId = "123"
        val checkIn = 1000L

        coEvery { chainAbstractionClient.status(fulfilmentId) } throws  RuntimeException("error")

        val result = async {
            suspendCoroutine { continuation ->
                chainAbstractionStatusUseCase.invoke(
                    fulfilmentId,
                    checkIn,
                    onSuccess = {
                        continuation.resume(false)
                    },
                    onError = {
                        continuation.resume(true)
                    }
                )
            }
        }.await()

        assertTrue(result)
    }
}