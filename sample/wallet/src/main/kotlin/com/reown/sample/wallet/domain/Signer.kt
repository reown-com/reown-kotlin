package com.reown.sample.wallet.domain

import com.reown.sample.common.Chains
import com.reown.sample.wallet.domain.model.Transaction
import com.reown.sample.wallet.ui.routes.dialog_routes.session_request.request.SessionRequestUI
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Signer {
    suspend fun sign(sessionRequest: SessionRequestUI.Content): String = supervisorScope {
        when {
            SmartAccountEnabler.isSmartAccountEnabled.value -> when (sessionRequest.method) {
                "wallet_sendCalls" -> {
                    val calls: MutableList<Wallet.Params.Call> = mutableListOf()
                    val callsArray = JSONArray(sessionRequest.param).getJSONObject(0).getJSONArray("calls")
                    for (i in 0 until callsArray.length()) {
                        val call = callsArray.getJSONObject(i)
                        val to = call.getString("to") ?: ""
                        val value = try {
                            call.getString("value")
                        } catch (e: Exception) {
                            ""
                        }

                        val data = try {
                            call.getString("data")
                        } catch (e: Exception) {
                            ""
                        }
                        calls.add(Wallet.Params.Call(to, value, data))
                    }
                    val ownerAccount = Wallet.Params.Account(EthAccountDelegate.sepoliaAddress)
                    val prepareSendTxsParams = Wallet.Params.PrepareSendTransactions(calls = calls, owner = ownerAccount)

                    val prepareTxsResult = async { prepareTransactions(prepareSendTxsParams) }.await().getOrThrow()
                    val signature = EthSigner.signHash(prepareTxsResult.hash, EthAccountDelegate.privateKey)
                    val doSendTxsParams = Wallet.Params.DoSendTransactions(
                        owner = ownerAccount,
                        signatures = listOf(Wallet.Params.OwnerSignature(address = EthAccountDelegate.account, signature = signature)),
                        doSendTransactionParams = prepareTxsResult.doSendTransactionParams
                    )
                    val doTxsResult = async { doTransactions(doSendTxsParams) }.await().getOrThrow()
                    val userOperationReceiptParam = Wallet.Params.WaitForUserOperationReceipt(owner = ownerAccount, userOperationHash = doTxsResult.userOperationHash)

                    val userOperationReceipt = waitForUserOperationReceipt(userOperationReceiptParam)
                    println("userOperationReceipt: $userOperationReceipt")

                    doTxsResult.userOperationHash
                }

                ETH_SEND_TRANSACTION -> {
                    val calls: MutableList<Wallet.Params.Call> = mutableListOf()
                    val params = JSONArray(sessionRequest.param).getJSONObject(0)
                    val to = params.getString("to") ?: ""
                    val value = params.getString("value") ?: ""
                    val data = try {
                        params.getString("data")
                    } catch (e: Exception) {
                        ""
                    }

                    calls.add(Wallet.Params.Call(to, value, data))
                    val ownerAccount = Wallet.Params.Account(EthAccountDelegate.sepoliaAddress)
                    val prepareSendTxsParams = Wallet.Params.PrepareSendTransactions(calls = calls, owner = ownerAccount)
                    val prepareTxsResult = async { prepareTransactions(prepareSendTxsParams) }.await().getOrThrow()
                    val signature = EthSigner.signHash(prepareTxsResult.hash, EthAccountDelegate.privateKey)
                    val doSendTxsParams = Wallet.Params.DoSendTransactions(
                        owner = ownerAccount,
                        signatures = listOf(Wallet.Params.OwnerSignature(address = EthAccountDelegate.account, signature = signature)),
                        doSendTransactionParams = prepareTxsResult.doSendTransactionParams
                    )
                    val doTxsResult = async { doTransactions(doSendTxsParams) }.await().getOrThrow()
                    doTxsResult.userOperationHash
                }

                else -> throw Exception("Unsupported Method")
            }

            !SmartAccountEnabler.isSmartAccountEnabled.value -> when {
                sessionRequest.method == PERSONAL_SIGN -> EthSigner.personalSign(sessionRequest.param)
                sessionRequest.method == ETH_SEND_TRANSACTION -> {
                    //todo: revert sending txs
//                    val txHash = Transaction.send(WCDelegate.sessionRequestEvent!!.first)
//                    txHash
                    """0xa3f20717a250c2b0b729b7e5becbff67fdaef7e0699da4de7ca5895b02a170a12d887fd3b17bfdce3481f10bea41f45ba9f709d39ce8325427b57afcfc994cee1b"""
                }
                //Note: Only for testing purposes - it will always fail on Dapp side
                sessionRequest.chain?.contains(Chains.Info.Eth.chain, true) == true ->
                    """0xa3f20717a250c2b0b729b7e5becbff67fdaef7e0699da4de7ca5895b02a170a12d887fd3b17bfdce3481f10bea41f45ba9f709d39ce8325427b57afcfc994cee1b"""
                //Note: Only for testing purposes - it will always fail on Dapp side
                sessionRequest.chain?.contains(Chains.Info.Cosmos.chain, true) == true ->
                    """{"signature":"pBvp1bMiX6GiWmfYmkFmfcZdekJc19GbZQanqaGa\/kLPWjoYjaJWYttvm17WoDMyn4oROas4JLu5oKQVRIj911==","pub_key":{"value":"psclI0DNfWq6cOlGrKD9wNXPxbUsng6Fei77XjwdkPSt","type":"tendermint\/PubKeySecp256k1"}}"""

                sessionRequest.method == "solana_signAndSendTransaction" ||
                        sessionRequest.method == "solana_signTransaction" -> {
                    """{"signature":"2Lb1KQHWfbV3pWMqXZveFWqneSyhH95YsgCENRWnArSkLydjN1M42oB82zSd6BBdGkM9pE6sQLQf1gyBh8KWM2c4"}"""
                }
                sessionRequest.method == "solana_signAllTransactions" -> {
                    """{"transactions":["2Lb1KQHWfbV3pWMqXZveFWqneSyhH95YsgCENRWnArSkLydjN1M42oB82zSd6BBdGkM9pE6sQLQf1gyBh8KWM2c4"]}"""
                }
                //Note: Only for testing purposes - it will always fail on Dapp side
                sessionRequest.chain?.contains(Chains.Info.Solana.chain, true) == true ->
                    """{"signature":"pBvp1bMiX6GiWmfYmkFmfcZdekJc19GbZQanqaGa\/kLPWjoYjaJWYttvm17WoDMyn4oROas4JLu5oKQVRIj911==","pub_key":{"value":"psclI0DNfWq6cOlGrKD9wNXPxbUsng6Fei77XjwdkPSt","type":"tendermint\/PubKeySecp256k1"}}"""

                else -> throw Exception("Unsupported Method")
            }

            else -> throw Exception("Unsupported Chain")
        }
    }

    private suspend fun prepareTransactions(params: Wallet.Params.PrepareSendTransactions): Result<Wallet.Params.PrepareSendTransactionsResult> =
        suspendCoroutine { continuation ->
            try {
                WalletKit.prepareSendTransactions(params) { result -> continuation.resume(Result.success(result)) }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }

    private suspend fun doTransactions(params: Wallet.Params.DoSendTransactions): Result<Wallet.Params.DoSendTransactionsResult> =
        suspendCoroutine { continuation ->
            try {
                WalletKit.doSendTransactions(params) { result -> continuation.resume(Result.success(result)) }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }

    private suspend fun waitForUserOperationReceipt(params: Wallet.Params.WaitForUserOperationReceipt): Result<String> =
        suspendCoroutine { continuation ->
            try {
                WalletKit.waitForUserOperationReceipt(params) { result -> continuation.resume(Result.success(result)) }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }

    const val PERSONAL_SIGN = "personal_sign"
    const val ETH_SEND_TRANSACTION = "eth_sendTransaction"
}
