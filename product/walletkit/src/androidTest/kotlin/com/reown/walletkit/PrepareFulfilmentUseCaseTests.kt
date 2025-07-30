//package com.reown.walletkit
//
//import com.reown.walletkit.client.Wallet
//import com.reown.walletkit.use_cases.PrepareChainAbstractionUseCase
//import io.mockk.coEvery
//import io.mockk.mockk
//import junit.framework.TestCase.assertTrue
//import kotlinx.coroutines.async
//import kotlinx.coroutines.test.runTest
//import org.junit.Test
//import uniffi.uniffi_yttrium.ChainAbstractionClient
//import uniffi.yttrium.Amount
//import uniffi.yttrium.BridgingError
//import uniffi.yttrium.FeeEstimatedTransaction
//import uniffi.yttrium.FundingMetadata
//import uniffi.yttrium.InitialTransactionMetadata
//import uniffi.yttrium.Metadata
//import uniffi.yttrium.PrepareDetailedResponse
//import uniffi.yttrium.PrepareDetailedResponseSuccess
//import uniffi.yttrium.PrepareResponseAvailable
//import uniffi.yttrium.PrepareResponseError
//import uniffi.yttrium.PrepareResponseNotRequired
//import uniffi.yttrium.Transaction
//import uniffi.yttrium.TransactionFee
//import uniffi.yttrium.TxnDetails
//import uniffi.yttrium.UiFields
//import kotlin.coroutines.resume
//import kotlin.coroutines.suspendCoroutine
//
//class PrepareChainAbstractionUseCaseTest {
//    private val chainAbstractionClient: ChainAbstractionClient = mockk()
//    private val prepareChainAbstractionUseCase = PrepareChainAbstractionUseCase(chainAbstractionClient)
//
//    @Test
//    fun shouldCallOnSuccessWithAvailableResult() = runTest {
//        val feeEstimatedTransactionMetadata = FeeEstimatedTransaction(
//            from = "from",
//            to = "to",
//            value = "value",
//            input = "data",
//            nonce = "nonce",
//            gasLimit = "gas",
//            chainId = "1",
//            maxPriorityFeePerGas = "11",
//            maxFeePerGas = "33"
//        )
//        val txDetails = TxnDetails(
//            transaction = feeEstimatedTransactionMetadata,
//            fee = TransactionFee(Amount("11", "18", 2u, "22", "1222"), Amount("11", "18", 2u, "22", "1222")),
//            transactionHashToSign = "hash",
//        )
//        val successResult = PrepareDetailedResponse.Success(
//            PrepareDetailedResponseSuccess.Available(
//                UiFields(
//                    route = listOf(txDetails),
//                    initial = txDetails,
//                    bridge = listOf(TransactionFee(Amount("11", "18", 2u, "22", "1222"), Amount("11", "18", 2u, "22", "1222"))),
//                    localTotal = Amount("11", "18", 2u, "22", "1222"),
//                    localBridgeTotal = Amount("11", "18", 2u, "22", "1222"),
//                    localRouteTotal = Amount("11", "18", 2u, "22", "1222"),
//                    routeResponse = PrepareResponseAvailable(
//                        orchestrationId = "123",
//                        initialTransaction = transaction,
//                        metadata = Metadata(
//                            fundingFrom = listOf(FundingMetadata(chainId = "1", tokenContract = "token", symbol = "s", amount = "11", decimals = 18u, bridgingFee = "0")),
//                            initialTransaction = InitialTransactionMetadata(transferTo = "aa", amount = "11", tokenContract = "cc", symbol = "s", decimals = 18u),
//                            checkIn = 11u
//                        ),
//                        transactions = listOf(transaction)
//                    )
//                )
//
//            )
//        )
//        coEvery { chainAbstractionClient.prepareDetailed(any(), any(), any(), any()) } returns successResult
//
//        val result = async {
//            suspendCoroutine { continuation ->
//                prepareChainAbstractionUseCase.invoke(
//                    initTransaction,
//                    onSuccess = {
//                        continuation.resume(true)
//                    },
//                    onError = {
//                        continuation.resume(false)
//                    }
//                )
//            }
//        }.await()
//
//        assertTrue(result)
//    }
//
//    @Test
//    fun shouldCallOnSuccessWithNotRequiredResult() = runTest {
//        val successResult = PrepareDetailedResponse.Success(PrepareDetailedResponseSuccess.NotRequired(PrepareResponseNotRequired(initialTransaction = transaction, transactions = emptyList())))
//        coEvery { chainAbstractionClient.prepareDetailed(any(), any(), any(), any()) } returns successResult
//
//        val result = async {
//            suspendCoroutine { continuation ->
//                prepareChainAbstractionUseCase.invoke(
//                    initTransaction,
//                    onSuccess = {
//                        continuation.resume(it)
//                    },
//                    onError = {
//                        continuation.resume(false)
//                    }
//                )
//            }
//        }.await()
//
//        assertTrue(result is Wallet.Model.PrepareSuccess.NotRequired)
//    }
//
//    @Test
//    fun shouldCallOnErrorWithNoRoutesAvailableError() = runTest {
//        val errorResult = PrepareDetailedResponse.Error(PrepareResponseError(BridgingError.NO_ROUTES_AVAILABLE))
//
//        coEvery { chainAbstractionClient.prepareDetailed(any(), any(), any(), any()) } returns errorResult
//
//        val result = async {
//            suspendCoroutine { continuation ->
//                prepareChainAbstractionUseCase.invoke(
//                    initTransaction,
//                    onSuccess = {
//                        continuation.resume(false)
//                    },
//                    onError = {
//                        continuation.resume(it)
//                    }
//                )
//            }
//        }.await()
//
//        assertTrue(result is Wallet.Model.PrepareError.NoRoutesAvailable)
//    }
//
//    @Test
//    fun shouldCallOnErrorWithInsufficientFundsError() = runTest {
//        val errorResult = PrepareDetailedResponse.Error(PrepareResponseError(BridgingError.INSUFFICIENT_FUNDS))
//
//        coEvery { chainAbstractionClient.prepareDetailed(any(), any(), any(), any()) } returns errorResult
//
//        val result = async {
//            suspendCoroutine { continuation ->
//                prepareChainAbstractionUseCase.invoke(
//                    initTransaction,
//                    onSuccess = {
//                        continuation.resume(false)
//                    },
//                    onError = {
//                        continuation.resume(it)
//                    }
//                )
//            }
//        }.await()
//
//        assertTrue(result is Wallet.Model.PrepareError.InsufficientFunds)
//    }
//
//    @Test
//    fun shouldCallOnErrorWithUnknownErrorOnException() = runTest {
//        coEvery { chainAbstractionClient.prepareDetailed(any(), any(), any(), any()) } throws RuntimeException("Some unexpected error")
//
//        val result = async {
//            suspendCoroutine { continuation ->
//                prepareChainAbstractionUseCase.invoke(
//                    initTransaction,
//                    onSuccess = {
//                        continuation.resume(false)
//                    },
//                    onError = {
//                        continuation.resume(true)
//                    }
//                )
//            }
//        }.await()
//
//        assertTrue(result)
//    }
//
//    companion object {
//        val transaction = Transaction(
//            from = "from",
//            to = "to",
//            value = "value",
//            input = "data",
//            nonce = "nonce",
//            chainId = "1",
//            gasLimit = "0"
//        )
//
//        val initTransaction = Wallet.Model.InitialTransaction(
//            from = "from",
//            to = "to",
//            value = "value",
//            chainId = "1",
//            input = "data"
//        )
//    }
//}