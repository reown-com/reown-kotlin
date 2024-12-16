package com.reown.walletkit

import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.toWallet
import com.reown.walletkit.use_cases.PrepareFulfilmentUseCase
import io.mockk.*
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Test
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.BridgingError
import uniffi.yttrium.FundingMetadata
import uniffi.yttrium.InitialTransactionMetadata
import uniffi.yttrium.Metadata
import uniffi.yttrium.RouteResponse
import uniffi.yttrium.RouteResponseAvailable
import uniffi.yttrium.RouteResponseError
import uniffi.yttrium.RouteResponseNotRequired
import uniffi.yttrium.RouteResponseSuccess
import uniffi.yttrium.Transaction
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PrepareFulfilmentUseCaseTest {
    private val chainAbstractionClient: ChainAbstractionClient = mockk()
    private val prepareFulfilmentUseCase = PrepareFulfilmentUseCase(chainAbstractionClient)

    @Test
    fun shouldCallOnSuccessWithAvailableResult() = runTest {
        val successResult = RouteResponse.Success(
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
        coEvery { chainAbstractionClient.route(any()) } returns successResult

        val result = async {
            suspendCoroutine { continuation ->
                prepareFulfilmentUseCase.invoke(transaction.toWallet(),
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
        val successResult = RouteResponse.Success(RouteResponseSuccess.NotRequired(RouteResponseNotRequired(initialTransaction = transaction, transactions = emptyList<Transaction>())))
        coEvery { chainAbstractionClient.route(any()) } returns successResult

        val result = async {
            suspendCoroutine { continuation ->
                prepareFulfilmentUseCase.invoke(
                    transaction.toWallet(),
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
        val errorResult = RouteResponse.Error(RouteResponseError(BridgingError.NO_ROUTES_AVAILABLE))

        coEvery { chainAbstractionClient.route(any()) } returns errorResult

        val result = async {
            suspendCoroutine { continuation ->
                prepareFulfilmentUseCase.invoke(
                    transaction.toWallet(),
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
        val errorResult = RouteResponse.Error(RouteResponseError(BridgingError.INSUFFICIENT_FUNDS))

        coEvery { chainAbstractionClient.route(any()) } returns errorResult

        val result = async {
            suspendCoroutine { continuation ->
                prepareFulfilmentUseCase.invoke(
                    Companion.transaction.toWallet(),
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
        coEvery { chainAbstractionClient.route(any()) } throws RuntimeException("Some unexpected error")

        val result = async {
            suspendCoroutine { continuation ->
                prepareFulfilmentUseCase.invoke(
                    Companion.transaction.toWallet(),
                    onSuccess = {
                        continuation.resume(false)
                    },
                    onError = {
                        continuation.resume(it)
                    }
                )
            }
        }.await()

        // Then
        assertTrue(result is Wallet.Model.FulfilmentError.Unknown)
    }

    companion object {
        val transaction = Transaction(
            from = "from",
            to = "to",
            value = "value",
            data = "data",
            nonce = "nonce",
            gas = "gas",
            gasPrice = "0", //todo: will be removed
            chainId = "1",
            maxPriorityFeePerGas = "0", //todo: will be removed
            maxFeePerGas = "0" //todo: will be removed
        )
    }
}