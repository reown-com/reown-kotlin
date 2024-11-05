package com.reown.sample.wallet.domain

import com.reown.sample.common.Chains
import com.reown.sample.wallet.ui.routes.dialog_routes.session_request.SessionRequestUI
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Signer {
    suspend fun sign(sessionRequest: SessionRequestUI.Content) = when {
        SmartAccountEnabler.isSmartAccountEnabled.value -> when (sessionRequest.method) {
            "wallet_sendCalls" -> {
                val transactions: MutableList<Wallet.Model.Transaction> = mutableListOf()
                val callsArray = JSONArray(sessionRequest.param).getJSONObject(0).getJSONArray("calls")
                for (i in 0 until callsArray.length()) {
                    val call = callsArray.getJSONObject(i)
                    val to = call.getString("to") ?: ""
                    val value = call.getString("value") ?: ""
                    val data = try {
                        call.getString("data")
                    } catch (e: Exception) {
                        ""
                    }
                    transactions.add(Wallet.Model.Transaction(to, value, data))
                }
                val ownerAccount = Wallet.Model.Account(EthAccountDelegate.sepoliaAddress)
                val prepareSendTxsParams = Wallet.Params.PrepareSendTransactions(transactions = transactions, owner = ownerAccount)
                suspendCoroutine { continuation ->
                    WalletKit.prepareSendTransactions(prepareSendTxsParams) { result ->
                        val signature = EthSigner.signHash(result.hash, EthAccountDelegate.privateKey)
                        val doSendTxsParams = Wallet.Params.DoSendTransactions(
                            owner = ownerAccount,
                            signatures = listOf(Wallet.Model.OwnerSignature(address = EthAccountDelegate.account, signature = signature)),
                            doSendTransactionParams = result.doSendTransactionParams
                        )

                        WalletKit.doSendTransactions(doSendTxsParams) { doTxsResult -> continuation.resume(doTxsResult.userOperationHash) }
                    }
                }
            }
            "eth_sendTransaction" -> {
                val transactions: MutableList<Wallet.Model.Transaction> = mutableListOf()
                val params = JSONArray(sessionRequest.param).getJSONObject(0)
                val to = params.getString("to") ?: ""
                val value = params.getString("value") ?: ""
                val data = try {
                    params.getString("data")
                } catch (e: Exception) {
                    ""
                }

                transactions.add(Wallet.Model.Transaction(to, value, data))
                val ownerAccount = Wallet.Model.Account(EthAccountDelegate.sepoliaAddress)
                val prepareSendTxsParams = Wallet.Params.PrepareSendTransactions(transactions = transactions, owner = ownerAccount)
                suspendCoroutine { continuation ->
                    WalletKit.prepareSendTransactions(prepareSendTxsParams) { result ->
                        val signature = EthSigner.signHash(result.hash, EthAccountDelegate.privateKey)
                        val doSendTxsParams = Wallet.Params.DoSendTransactions(
                            owner = ownerAccount,
                            signatures = listOf(Wallet.Model.OwnerSignature(address = EthAccountDelegate.account, signature = signature)),
                            doSendTransactionParams = result.doSendTransactionParams
                        )

                        WalletKit.doSendTransactions(doSendTxsParams) { doTxsResult -> continuation.resume(doTxsResult.userOperationHash) }
                    }
                }
            }
            else -> throw Exception("Unsupported Method")
        }

        !SmartAccountEnabler.isSmartAccountEnabled.value -> when {
            sessionRequest.method == PERSONAL_SIGN_METHOD -> EthSigner.personalSign(sessionRequest.param)
            //Note: Only for testing purposes - it will always fail on Dapp side
            sessionRequest.chain?.contains(Chains.Info.Eth.chain, true) == true ->
                """0xa3f20717a250c2b0b729b7e5becbff67fdaef7e0699da4de7ca5895b02a170a12d887fd3b17bfdce3481f10bea41f45ba9f709d39ce8325427b57afcfc994cee1b"""
            //Note: Only for testing purposes - it will always fail on Dapp side
            sessionRequest.chain?.contains(Chains.Info.Cosmos.chain, true) == true ->
                """{"signature":"pBvp1bMiX6GiWmfYmkFmfcZdekJc19GbZQanqaGa\/kLPWjoYjaJWYttvm17WoDMyn4oROas4JLu5oKQVRIj911==","pub_key":{"value":"psclI0DNfWq6cOlGrKD9wNXPxbUsng6Fei77XjwdkPSt","type":"tendermint\/PubKeySecp256k1"}}"""
            //Note: Only for testing purposes - it will always fail on Dapp side
            sessionRequest.chain?.contains(Chains.Info.Solana.chain, true) == true ->
                """{"signature":"pBvp1bMiX6GiWmfYmkFmfcZdekJc19GbZQanqaGa\/kLPWjoYjaJWYttvm17WoDMyn4oROas4JLu5oKQVRIj911==","pub_key":{"value":"psclI0DNfWq6cOlGrKD9wNXPxbUsng6Fei77XjwdkPSt","type":"tendermint\/PubKeySecp256k1"}}"""

            else -> throw Exception("Unsupported Method")
        }

        else -> throw Exception("Unsupported Chain")
    }

    const val PERSONAL_SIGN_METHOD = "personal_sign"
}