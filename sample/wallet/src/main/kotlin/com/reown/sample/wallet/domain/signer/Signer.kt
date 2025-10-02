package com.reown.sample.wallet.domain.signer

import com.reown.sample.common.Chains
import com.reown.sample.wallet.domain.WCDelegate
import com.reown.sample.wallet.domain.account.TONAccountDelegate
import com.reown.sample.wallet.domain.client.TONClient
import com.reown.sample.wallet.domain.model.Transaction
import com.reown.sample.wallet.ui.routes.dialog_routes.session_request.request.SessionRequestUI
import kotlinx.coroutines.supervisorScope
import org.json.JSONArray
import org.json.JSONObject
import uniffi.yttrium.SendTxMessage

object Signer {
    suspend fun sign(sessionRequest: SessionRequestUI.Content): String = supervisorScope {
        when {
//            SmartAccountEnabler.isSmartAccountEnabled.value -> when (sessionRequest.method) {
//                "wallet_sendCalls" -> {
//                    val calls: MutableList<Wallet.Params.Call> = mutableListOf()
//                    val callsArray = JSONArray(sessionRequest.param).getJSONObject(0).getJSONArray("calls")
//                    for (i in 0 until callsArray.length()) {
//                        val call = callsArray.getJSONObject(i)
//                        val to = call.getString("to") ?: ""
//                        val value = try {
//                            call.getString("value")
//                        } catch (e: Exception) {
//                            ""
//                        }
//
//                        val data = try {
//                            call.getString("data")
//                        } catch (e: Exception) {
//                            ""
//                        }
//                        calls.add(Wallet.Params.Call(to, value, data))
//                    }
//                    val ownerAccount = Wallet.Params.Account(EthAccountDelegate.sepoliaAddress)
//                    val prepareSendTxsParams = Wallet.Params.PrepareSendTransactions(calls = calls, owner = ownerAccount)
//
//                    val prepareTxsResult = async { prepareTransactions(prepareSendTxsParams) }.await().getOrThrow()
//                    val signature = EthSigner.signHash(prepareTxsResult.hash, EthAccountDelegate.privateKey)
//                    val doSendTxsParams = Wallet.Params.DoSendTransactions(
//                        owner = ownerAccount,
//                        signatures = listOf(Wallet.Params.OwnerSignature(address = EthAccountDelegate.account, signature = signature)),
//                        doSendTransactionParams = prepareTxsResult.doSendTransactionParams
//                    )
//                    val doTxsResult = async { doTransactions(doSendTxsParams) }.await().getOrThrow()
//                    val userOperationReceiptParam = Wallet.Params.WaitForUserOperationReceipt(owner = ownerAccount, userOperationHash = doTxsResult.userOperationHash)
//
//                    val userOperationReceipt = waitForUserOperationReceipt(userOperationReceiptParam)
//                    println("userOperationReceipt: $userOperationReceipt")
//
//                    doTxsResult.userOperationHash
//                }
//
//                ETH_SEND_TRANSACTION -> {
//                    val calls: MutableList<Wallet.Params.Call> = mutableListOf()
//                    val params = JSONArray(sessionRequest.param).getJSONObject(0)
//                    val to = params.getString("to") ?: ""
//                    val value = params.getString("value") ?: ""
//                    val data = try {
//                        params.getString("data")
//                    } catch (e: Exception) {
//                        ""
//                    }
//
//                    calls.add(Wallet.Params.Call(to, value, data))
//                    val ownerAccount = Wallet.Params.Account(EthAccountDelegate.sepoliaAddress)
//                    val prepareSendTxsParams = Wallet.Params.PrepareSendTransactions(calls = calls, owner = ownerAccount)
//                    val prepareTxsResult = async { prepareTransactions(prepareSendTxsParams) }.await().getOrThrow()
//                    val signature = EthSigner.signHash(prepareTxsResult.hash, EthAccountDelegate.privateKey)
//                    val doSendTxsParams = Wallet.Params.DoSendTransactions(
//                        owner = ownerAccount,
//                        signatures = listOf(Wallet.Params.OwnerSignature(address = EthAccountDelegate.account, signature = signature)),
//                        doSendTransactionParams = prepareTxsResult.doSendTransactionParams
//                    )
//                    val doTxsResult = async { doTransactions(doSendTxsParams) }.await().getOrThrow()
//                    doTxsResult.userOperationHash
//                }
//
//                else -> throw Exception("Unsupported Method")
//            }

//            !SmartAccountEnabler.isSmartAccountEnabled.value -> when {
            sessionRequest.method == "SignData" -> {
                println("kobe: SignData: $sessionRequest")

                try {
                    // Parse the params JSON array
                    val paramsArray = JSONArray(sessionRequest.param)
                    val firstParam = paramsArray.getJSONObject(0)
                    val text = firstParam.getString("text")

                    // Sign the data using TONClient
                    val signature = TONClient.signData(text)

                    println("kobe: Generated signature: $signature")
                    signature
                } catch (e: Exception) {
                    println("kobe: Error signing TON data: ${e.message}")
                    throw Exception("Failed to sign TON data: ${e.message}")
                }
            }

            sessionRequest.method == "SendMessage" -> {
                //params=[{"valid_until":1759397476,"from":"EQDV4YleDzbJ2W8wPMoIm0kG26uI4EU6wU7SofK1OUvS1VaG","messages":[{"address":"EQDV4YleDzbJ2W8wPMoIm0kG26uI4EU6wU7SofK1OUvS1VaG","amount":"1000"}]}]
                println("kobe: SendMessage: $sessionRequest")
                
                try {
                    // Parse the params JSON array
                    val paramsArray = JSONArray(sessionRequest.param)
                    val firstParam = paramsArray.getJSONObject(0)
                    
                    // Extract parameters
                    val validUntil = firstParam.getLong("valid_until").toUInt()
                    val from = firstParam.getString("from")
                    val messagesArray = firstParam.getJSONArray("messages")
                    
                    println("kobe: Extracted valid_until: $validUntil, from: $from, messages: $messagesArray")
                    
                    // Convert messages array to List<SendTxMessage>
                    val sendTxMessages = mutableListOf<SendTxMessage>()
                    for (i in 0 until messagesArray.length()) {
                        val messageObj = messagesArray.getJSONObject(i)
                        val address = messageObj.getString("address")
                        val amount = messageObj.getString("amount")
                        
                        // Create SendTxMessage object
                        val sendTxMessage = SendTxMessage(address, amount, null, null)
                        sendTxMessages.add(sendTxMessage)
                        
                        println("kobe: Created SendTxMessage - address: $address, amount: $amount")
                    }
                    
                    // Send the message using TONClient
                    val result = TONClient.sendMessage(from, validUntil, sendTxMessages)
                    
                    println("kobe: SendMessage result: $result")
                    result
                } catch (e: Exception) {
                    println("kobe: Error sending TON message: ${e.message}")
                    throw Exception("Failed to send TON message: ${e.message}")
                }
            }

            sessionRequest.method == PERSONAL_SIGN -> EthSigner.personalSign(sessionRequest.param)
            sessionRequest.method == ETH_SEND_TRANSACTION -> {
                val txHash = Transaction.send(WCDelegate.sessionRequestEvent!!.first)
                txHash
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
//            }

//            else -> throw Exception("Unsupported Chain")
        }
    }

//    private suspend fun prepareTransactions(params: Wallet.Params.PrepareSendTransactions): Result<Wallet.Params.PrepareSendTransactionsResult> =
//        suspendCoroutine { continuation ->
//            try {
//                WalletKit.prepareSendTransactions(params) { result -> continuation.resume(Result.success(result)) }
//            } catch (e: Exception) {
//                continuation.resume(Result.failure(e))
//            }
//        }
//
//    private suspend fun doTransactions(params: Wallet.Params.DoSendTransactions): Result<Wallet.Params.DoSendTransactionsResult> =
//        suspendCoroutine { continuation ->
//            try {
//                WalletKit.doSendTransactions(params) { result -> continuation.resume(Result.success(result)) }
//            } catch (e: Exception) {
//                continuation.resume(Result.failure(e))
//            }
//        }
//
//    private suspend fun waitForUserOperationReceipt(params: Wallet.Params.WaitForUserOperationReceipt): Result<String> =
//        suspendCoroutine { continuation ->
//            try {
//                WalletKit.waitForUserOperationReceipt(params) { result -> continuation.resume(Result.success(result)) }
//            } catch (e: Exception) {
//                continuation.resume(Result.failure(e))
//            }
//        }

    const val PERSONAL_SIGN = "personal_sign"
    const val ETH_SEND_TRANSACTION = "eth_sendTransaction"
}
