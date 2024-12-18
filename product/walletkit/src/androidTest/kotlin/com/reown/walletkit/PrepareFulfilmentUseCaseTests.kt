package com.reown.walletkit

import com.reown.walletkit.client.Wallet
import com.reown.walletkit.use_cases.PrepareChainAbstractionUseCase
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Test
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.BridgingError
import uniffi.yttrium.FundingMetadata
import uniffi.yttrium.InitialTransactionMetadata
import uniffi.yttrium.Metadata
import uniffi.yttrium.PrepareResponse
import uniffi.yttrium.RouteResponseAvailable
import uniffi.yttrium.RouteResponseError
import uniffi.yttrium.RouteResponseNotRequired
import uniffi.yttrium.RouteResponseSuccess
import uniffi.yttrium.Transaction
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PrepareChainAbstractionUseCaseTest {
    private val chainAbstractionClient: ChainAbstractionClient = mockk()
    private val prepareChainAbstractionUseCase = PrepareChainAbstractionUseCase(chainAbstractionClient)

    @Test
    fun shouldCallOnSuccessWithAvailableResult() = runTest {
        val successResult = PrepareResponse.Success(
            RouteResponseSuccess.Available(
                RouteResponseAvailable(
                    orchestrationId = "123",
                    initialTransaction = transaction,
                    metadata = Metadata(
                        fundingFrom = listOf(FundingMetadata(chainId = "1", tokenContract = "token", symbol = "s", amount = "11", decimals = 18u, bridgingFee = "0")),
                        initialTransaction = InitialTransactionMetadata(transferTo = "aa", amount = "11", tokenContract = "cc", symbol = "s", decimals = 18u),
                        checkIn = 11u
                    ),
                    transactions = listOf(transaction)
                )
            )
        )
        coEvery { chainAbstractionClient.prepare(any()) } returns successResult

        val result = async {
            suspendCoroutine { continuation ->
                prepareChainAbstractionUseCase.invoke(
                    initTransaction,
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
    fun shouldCallOnSuccessWithNotRequiredResult() = runTest {
        val successResult = PrepareResponse.Success(RouteResponseSuccess.NotRequired(RouteResponseNotRequired(initialTransaction = transaction, transactions = emptyList<Transaction>())))
        coEvery { chainAbstractionClient.prepare(any()) } returns successResult

        val result = async {
            suspendCoroutine { continuation ->
                prepareChainAbstractionUseCase.invoke(
                    initTransaction,
                    onSuccess = {
                        continuation.resume(it)
                    },
                    onError = {
                        continuation.resume(false)
                    }
                )
            }
        }.await()

        assertTrue(result is Wallet.Model.FulfilmentSuccess.NotRequired)
    }

    @Test
    fun shouldCallOnErrorWithNoRoutesAvailableError() = runTest {
        val errorResult = PrepareResponse.Error(RouteResponseError(BridgingError.NO_ROUTES_AVAILABLE))

        coEvery { chainAbstractionClient.prepare(any()) } returns errorResult

        val result = async {
            suspendCoroutine { continuation ->
                prepareChainAbstractionUseCase.invoke(
                    initTransaction,
                    onSuccess = {
                        continuation.resume(false)
                    },
                    onError = {
                        continuation.resume(it)
                    }
                )
            }
        }.await()

        assertTrue(result is Wallet.Model.FulfilmentError.NoRoutesAvailable)
    }

    @Test
    fun shouldCallOnErrorWithInsufficientFundsError() = runTest {
        val errorResult = PrepareResponse.Error(RouteResponseError(BridgingError.INSUFFICIENT_FUNDS))

        coEvery { chainAbstractionClient.prepare(any()) } returns errorResult

        val result = async {
            suspendCoroutine { continuation ->
                prepareChainAbstractionUseCase.invoke(
                    initTransaction,
                    onSuccess = {
                        continuation.resume(false)
                    },
                    onError = {
                        continuation.resume(it)
                    }
                )
            }
        }.await()

        assertTrue(result is Wallet.Model.FulfilmentError.InsufficientFunds)
    }

    @Test
    fun shouldCallOnErrorWithUnknownErrorOnException() = runTest {
        coEvery { chainAbstractionClient.prepare(any()) } throws RuntimeException("Some unexpected error")

        val result = async {
            suspendCoroutine { continuation ->
                prepareChainAbstractionUseCase.invoke(
                    initTransaction,
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

    companion object {
        val transaction = Transaction(
            from = "from",
            to = "to",
            value = "value",
            input = "data",
            nonce = "nonce",
            chainId = "1",
            gasLimit = "0"
        )

        val initTransaction = Wallet.Model.InitialTransaction(
            from = "from",
            to = "to",
            value = "value",
            chainId = "1",
            input = "data"
        )
    }
}