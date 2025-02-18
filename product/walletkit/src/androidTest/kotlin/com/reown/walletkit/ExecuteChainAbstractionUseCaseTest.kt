package com.reown.walletkit

import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.toWallet
import com.reown.walletkit.use_cases.ExecuteChainAbstractionUseCase
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.Amount
import uniffi.yttrium.ExecuteDetails
import uniffi.yttrium.FeeEstimatedTransaction
import uniffi.yttrium.FundingMetadata
import uniffi.yttrium.InitialTransactionMetadata
import uniffi.yttrium.Metadata
import uniffi.yttrium.PrepareDetailedResponseSuccess
import uniffi.yttrium.PrepareResponseAvailable
import uniffi.yttrium.TransactionFee
import uniffi.yttrium.TxnDetails
import uniffi.yttrium.UiFields
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ExecuteChainAbstractionUseCaseTest {
    private lateinit var chainAbstractionClient: ChainAbstractionClient
    private lateinit var useCase: ExecuteChainAbstractionUseCase
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
    private val txDetails = TxnDetails(
        transaction = feeEstimatedTransactionMetadata,
        fee = TransactionFee(Amount("11", "18", 2u, "22", "1222"), Amount("11", "18", 2u, "22", "1222")),
        transactionHashToSign = "hash",
    )
    private var prepareAvailable: Wallet.Model.PrepareSuccess.Available = PrepareDetailedResponseSuccess.Available(
        UiFields(
            route = listOf(txDetails),
            initial = txDetails,
            bridge = listOf(TransactionFee(Amount("11", "18", 2u, "22", "1222"), Amount("11", "18", 2u, "22", "1222"))),
            localTotal = Amount("11", "18", 2u, "22", "1222"),
            localBridgeTotal = Amount("11", "18", 2u, "22", "1222"),
            localRouteTotal = Amount("11", "18", 2u, "22", "1222"),
            routeResponse = PrepareResponseAvailable(
                orchestrationId = "123",
                initialTransaction = PrepareChainAbstractionUseCaseTest.transaction,
                metadata = Metadata(
                    fundingFrom = listOf(FundingMetadata(chainId = "1", tokenContract = "token", symbol = "s", amount = "11", decimals = 18u, bridgingFee = "0")),
                    initialTransaction = InitialTransactionMetadata(transferTo = "aa", amount = "11", tokenContract = "cc", symbol = "s", decimals = 18u),
                    checkIn = 11u
                ),
                transactions = listOf(PrepareChainAbstractionUseCaseTest.transaction)
            )
        )

    ).v1.toWallet()

    @Before
    fun setup() {
        chainAbstractionClient = mockk()
        useCase = ExecuteChainAbstractionUseCase(chainAbstractionClient)
    }

    @Test
    fun invokeSuccessfulExecution() = runTest {
        // Given
        val signedRouteTxs = listOf("signedTx1", "signedTx2")
        val initSignedTx = "initTx"
        val executeDetails = ExecuteDetails(initialTxnReceipt = "receipt", initialTxnHash = "initTx")

        coEvery { chainAbstractionClient.execute(any(), signedRouteTxs, initSignedTx) } returns executeDetails

        // When
        val result = async {
            suspendCoroutine { continuation ->
                useCase(prepareAvailable, signedRouteTxs, initSignedTx,
                    onSuccess = {
                        continuation.resume(it.initialTxHash)
                    }, onError = {
                        continuation.resume("")
                    })
            }
        }.await()

        // Then
        assertEquals("initTx", result)
    }

    @Test
    fun invokeClientExecutionThrowsException() = runTest {
        // Given
        val signedRouteTxs = listOf("signedTx1", "signedTx2")
        val initSignedTx = "initTx"
        val exception = Exception("Test error")

        coEvery { chainAbstractionClient.execute(any(), signedRouteTxs, initSignedTx) } throws exception


        // When
        val result = async {
            suspendCoroutine { continuation ->
                useCase(prepareAvailable, signedRouteTxs, initSignedTx,
                    onSuccess = {
                        continuation.resume("")
                    }, onError = {
                        continuation.resume(it.throwable.message)
                    })
            }
        }.await()

        // Then
        assertEquals("Test error", result)
    }
}