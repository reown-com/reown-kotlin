package com.reown.walletkit

import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.toWallet
import com.reown.walletkit.use_cases.GetTransactionDetailsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.uniffi_yttrium.Eip1559Estimation
import uniffi.uniffi_yttrium.RouteUiFields
import uniffi.uniffi_yttrium.TxnDetails
import uniffi.yttrium.Amount
import uniffi.yttrium.Transaction
import uniffi.yttrium.TransactionFee
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
class GetTransactionDetailsUseCaseTest {
    private val chainAbstractionClient: ChainAbstractionClient = mockk()
    private val getTransactionDetailsUseCase = GetTransactionDetailsUseCase(chainAbstractionClient)

    @Test
    fun shouldCallOnSuccessWithExpectedResultWhenClientSucceeds() = runTest {
        val available = Wallet.Model.FulfilmentSuccess.Available(
            fulfilmentId = "123",
            checkIn = 11,
            initialTransaction = transaction.toWallet(),
            transactions = listOf(transaction.toWallet()),
            funding = listOf(Wallet.Model.FundingMetadata(chainId = "1", tokenContract = "token", symbol = "s", amount = "11", decimals = 18, bridgingFee = "0")),
            initialTransactionMetadata = Wallet.Model.InitialTransactionMetadata(transferTo = "aa", amount = "11", tokenContract = "cc", symbol = "s", decimals = 18)
        )
        val initTransaction = transaction.toWallet()
        val resultFields = RouteUiFields(
            route = listOf(txDetails),
            initial = txDetails,
            bridge = listOf(TransactionFee(Amount("11", "18", 2u, "22", "1222"), Amount("11", "18", 2u, "22", "1222"))),
            localTotal = Amount("11", "18", 2u, "22", "1222")
        )

        coEvery { chainAbstractionClient.getRouteUiFields(any(), any(), any()) } returns resultFields

        val result = async {
            suspendCoroutine { continuation ->
                getTransactionDetailsUseCase.invoke(
                    available,
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

//    @Test
//    fun shouldCallOnErrorWhenClientThrowsAnException() = runTest {
//        val available = Wallet.Model.FulfilmentSuccess.Available(
//            fulfilmentId = "123",
//            checkIn = 11,
//            initialTransaction = transaction.toWallet(),
//            transactions = listOf(transaction.toWallet()),
//            funding = listOf(Wallet.Model.FundingMetadata(chainId = "1", tokenContract = "token", symbol = "s", amount = "11", decimals = 18, bridgingFee = "0")),
//            initialTransactionMetadata = Wallet.Model.InitialTransactionMetadata(transferTo = "aa", amount = "11", tokenContract = "cc", symbol = "s", decimals = 18)
//        )
//        val initTransaction = transaction.toWallet()
//        val exception = Exception("Some error occurred")
//
//        coEvery { chainAbstractionClient.getRouteUiFields(any(), any(), any()) } throws exception
//        var errorCaptured = false
//        val result = async {
//            suspendCoroutine { continuation ->
//                getTransactionDetailsUseCase.invoke(
//                    available,
//                    initTransaction,
//                    onSuccess = {
//                        println("kobe: success: $it")
//                        continuation.resumeWith(Result.success(false))
//                    },
//                    onError = {
//                        println("kobe: test1  error: $it")
//                        errorCaptured = true
//                        continuation.resume(it.throwable)
//
////                        continuation.resumeWith(Result.success(true))
//                    }
//                )
//            }
//        }.await()
//
//        println("kobe: result: $result")
//        println("kobe: errorCaptured: $errorCaptured")
//        assertEquals(errorCaptured, true)
//    }

    companion object {
        val transaction = Transaction(
            from = "from",
            to = "to",
            value = "value",
            data = "data",
            nonce = "nonce",
            gas = "gas",
            gasPrice = "0",
            chainId = "1",
            maxPriorityFeePerGas = "0",
            maxFeePerGas = "0"
        )

        val txDetails = TxnDetails(
            transaction = transaction,
            estimate = Eip1559Estimation("11", "22"),
            fee = TransactionFee(Amount("11", "18", 2u, "22", "1222"), Amount("11", "18", 2u, "22", "1222")),
        )
    }
}