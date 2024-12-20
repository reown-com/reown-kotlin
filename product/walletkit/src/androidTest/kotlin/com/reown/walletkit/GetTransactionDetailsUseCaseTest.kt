package com.reown.walletkit

import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.toWallet
import com.reown.walletkit.use_cases.GetTransactionDetailsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Test
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.Amount
import uniffi.yttrium.FeeEstimatedTransaction
import uniffi.yttrium.Transaction
import uniffi.yttrium.TransactionFee
import uniffi.yttrium.TxnDetails
import uniffi.yttrium.UiFields
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
class GetTransactionDetailsUseCaseTest {
    private val chainAbstractionClient: ChainAbstractionClient = mockk()
    private val getTransactionDetailsUseCase = GetTransactionDetailsUseCase(chainAbstractionClient)

    @Test
    fun shouldCallOnSuccessWithExpectedResultWhenClientSucceeds() = runTest {
        val available = Wallet.Model.PrepareSuccess.Available(
            fulfilmentId = "123",
            checkIn = 11,
            initialTransaction = transaction.toWallet(),
            transactions = listOf(transaction.toWallet()),
            funding = listOf(Wallet.Model.FundingMetadata(chainId = "1", tokenContract = "token", symbol = "s", amount = "11", decimals = 18, bridgingFee = "0")),
            initialTransactionMetadata = Wallet.Model.InitialTransactionMetadata(transferTo = "aa", amount = "11", tokenContract = "cc", symbol = "s", decimals = 18)
        )
        val resultFields = UiFields(
            route = listOf(txDetails),
            initial = txDetails,
            bridge = listOf(TransactionFee(Amount("11", "18", 2u, "22", "1222"), Amount("11", "18", 2u, "22", "1222"))),
            localTotal = Amount("11", "18", 2u, "22", "1222"),
            localBridgeTotal = Amount("11", "18", 2u, "22", "1222"),
            localRouteTotal = Amount("11", "18", 2u, "22", "1222"),
        )

        coEvery { chainAbstractionClient.getUiFields(any(), any()) } returns resultFields

        val result = async {
            suspendCoroutine { continuation ->
                getTransactionDetailsUseCase.invoke(
                    available,
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
    fun shouldCallOnErrorWhenClientThrowsAnException() = runTest {
        val available = Wallet.Model.PrepareSuccess.Available(
            fulfilmentId = "123",
            checkIn = 11,
            initialTransaction = transaction.toWallet(),
            transactions = listOf(transaction.toWallet()),
            funding = listOf(Wallet.Model.FundingMetadata(chainId = "1", tokenContract = "token", symbol = "s", amount = "11", decimals = 18, bridgingFee = "0")),
            initialTransactionMetadata = Wallet.Model.InitialTransactionMetadata(transferTo = "aa", amount = "11", tokenContract = "cc", symbol = "s", decimals = 18)
        )
        val exception = Exception("Some error occurred")

        coEvery { chainAbstractionClient.getUiFields(any(), any()) } throws exception

        val result = async {
            suspendCoroutine { continuation ->
                getTransactionDetailsUseCase.invoke(
                    available,
                    onSuccess = {
                        println("success: $it")
                        continuation.resume(false)
                    },
                    onError = {
                        println("test1  error: $it")
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
            gasLimit = "gas",
            chainId = "1"
        )

        private val feeEstimatedTransactionMetadata = FeeEstimatedTransaction(
            from = "from",
            to = "to",
            value = "value",
            input = "data",
            nonce = "nonce",
            gasLimit = "gas",
            chainId = "1",
            maxPriorityFeePerGas = "11",
            maxFeePerGas = "33"
        )

        val txDetails = TxnDetails(
            transaction = feeEstimatedTransactionMetadata,
            fee = TransactionFee(Amount("11", "18", 2u, "22", "1222"), Amount("11", "18", 2u, "22", "1222")),
        )
    }
}