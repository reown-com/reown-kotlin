package com.reown.sample.wallet.ui.routes.dialog_routes.session_request

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.android.internal.common.exception.NoConnectivityException
import com.reown.android.utils.cacao.sign
import com.reown.sample.wallet.domain.EthAccountDelegate
import com.reown.sample.wallet.domain.Signer
import com.reown.sample.wallet.domain.Signer.PERSONAL_SIGN_METHOD
import com.reown.sample.wallet.domain.SmartAccountEnabler
import com.reown.sample.wallet.domain.WCDelegate
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.sample.wallet.ui.common.peer.toPeerUI
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.exception.CryptoWeb3jException
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import org.web3j.utils.Numeric.hexStringToByteArray
import org.web3j.utils.Numeric.toBigInt
import java.math.BigInteger

class SessionRequestViewModel : ViewModel() {
    var sessionRequestUI: SessionRequestUI = generateSessionRequestUI()


    fun approve(onSuccess: (Uri?) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val sessionRequest = sessionRequestUI as? SessionRequestUI.Content
                if (sessionRequest != null) {
                    val result: String = Signer.sign(sessionRequest)

                    //Available(fulfilmentId=e7184c85-13bb-4d89-8804-33394933f342, transactions=[Transaction(from=0xc3d7420ea0d9102760c4dcf700245961ffc5ec42, to=0x833589fcd6edb6e08f4c7c32d4f71b54bda02913, value=0x00, gas=0xf9e82, gasPrice=0x62e80b, data=0x095ea7b30000000000000000000000003a23f943181408eac424116af7b7790c94cb97a50000000000000000000000000000000000000000000000000000000000100590, nonce=0x0, maxFeePerGas=0, maxPriorityFeePerGas=0, chainId=eip155:8453), Transaction(from=0xc3d7420ea0d9102760c4dcf700245961ffc5ec42, to=0x3a23f943181408eac424116af7b7790c94cb97a5, value=0x00, gas=0xf9e82, gasPrice=0x62e80b, data=0x0000019b792ebcb90000000000000000000000000000000000000000000000000000000000100590000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000120000000000000000000000000000000000000000000000000000000000000018000000000000000000000000000000000000000000000000000000000000001e0000000000000000000000000000000000000000000000000000000000000220c0000000000000000000000000000000000000000000000000000000000001b3b0000000000000000000000000000000000000000000000000000000000000002000000000000000000000000c3d7420ea0d9102760c4dcf700245961ffc5ec42000000000000000000000000c3d7420ea0d9102760c4dcf700245961ffc5ec420000000000000000000000000000000000000000000000000000000000000002000000000000000000000000833589fcd6edb6e08f4c7c32d4f71b54bda029130000000000000000000000000b2c639c533813f4aa9d7837caf62653d097ff85000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000fe384000000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000006736f8ef0000000000000000000000000000000000000000000000000000000067374cf5d00dfeeddeadbeef765753be7f7a64d5509974b0d678e1e3149b02f4, nonce=0x1, maxFeePerGas=0, maxPriorityFeePerGas=0, chainId=eip155:8453)], funding=[FundingMetadata(chainId=eip155:8453, tokenContract=0x833589fcd6edb6e08f4c7c32d4f71b54bda02913, symbol=USDC, amount=0x100590)])

                    val fulfilmentId = WCDelegate.fulfilmentAvailable!!.fulfilmentId
                    val funding = WCDelegate.fulfilmentAvailable!!.funding
                    val transactions = WCDelegate.fulfilmentAvailable!!.transactions
                    val initialTransaction = WCDelegate.initTransaction
                    val signedTransactions = mutableListOf<String>()


                    transactions.forEach { transaction ->
                        val chainId = transaction.chainId.split(":")[1].toLong()
                        println("kobe: chainId: $chainId")
                        println("kobe: nonce: ${toBigInt(transaction.nonce)}")
                        println("kobe: gas: ${toBigInt(transaction.gas)}")
                        println("kobe: value: ${toBigInt(transaction.value)}")

                        //todo: get gas
                        val rawTransaction = RawTransaction.createTransaction(
                            chainId,
                            toBigInt(transaction.nonce),
                            toBigInt(transaction.gas),
                            transaction.to,
                            toBigInt(transaction.value),
                            transaction.data,
                            BigInteger.valueOf(5678),//transaction.maxPriorityFeePerGas.toBigInteger(),
                            BigInteger.valueOf(1100000) //transaction.maxFeePerGas.toBigInteger()
                        )

                        //sign fulfilment txs
                        val signedTransaction = Numeric.toHexString(TransactionEncoder.signMessage(rawTransaction, Credentials.create(EthAccountDelegate.privateKey)))
                        signedTransactions.add(signedTransaction)
                    }
                    println("kobe: signedTransactions: $signedTransactions")

                    //use blockchain to eth_sendRawTransaction
                    //call WK for the status
                    //if status completed, execute init tx
                    //send back the response with the tx hash of init to a dapp


                    val response = Wallet.Params.SessionRequestResponse(
                        sessionTopic = sessionRequest.topic,
                        jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(sessionRequest.requestId, result)
                    )
                    val redirect = WalletKit.getActiveSessionByTopic(sessionRequest.topic)?.redirect?.toUri()

                    WalletKit.respondSessionRequest(response,
                        onSuccess = {
                            clearSessionRequest()
                            onSuccess(redirect)
                        },
                        onError = { error ->
                            Firebase.crashlytics.recordException(error.throwable)
                            if (error.throwable !is NoConnectivityException) {
                                clearSessionRequest()
                            }
                            onError(error.throwable)
                        })
                } else {
                    onError(Throwable("Approve - Cannot find session request"))
                }
            } catch (e: Exception) {
                println("kobe: Error: $e")
                Firebase.crashlytics.recordException(e)
                reject()
                clearSessionRequest()
                onError(Throwable(e.message ?: "Undefined error, please check your Internet connection"))
            }
        }
    }

    fun reject(onSuccess: (Uri?) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        try {
            val sessionRequest = sessionRequestUI as? SessionRequestUI.Content
            if (sessionRequest != null) {
                val result = Wallet.Params.SessionRequestResponse(
                    sessionTopic = sessionRequest.topic,
                    jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                        id = sessionRequest.requestId,
                        code = 500,
                        message = "Kotlin Wallet Error"
                    )
                )
                val redirect = WalletKit.getActiveSessionByTopic(sessionRequest.topic)?.redirect?.toUri()
                WalletKit.respondSessionRequest(result,
                    onSuccess = {
                        clearSessionRequest()
                        onSuccess(redirect)
                    },
                    onError = { error ->
                        Firebase.crashlytics.recordException(error.throwable)
                        if (error.throwable !is NoConnectivityException) {
                            clearSessionRequest()
                        }
                        onError(error.throwable)
                    })
            } else {
                onError(Throwable("Reject - Cannot find session request"))
            }
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            clearSessionRequest()
            onError(Throwable(e.message ?: "Undefined error, please check your Internet connection"))
        }
    }

    private fun extractMessageParamFromPersonalSign(input: String): String {
        val jsonArray = JSONArray(input)
        return if (jsonArray.length() > 0) {
            if (jsonArray.getString(0).startsWith("0x")) {
                String(hexStringToByteArray(jsonArray.getString(0)))
            } else {
                jsonArray.getString(0)
            }
        } else {
            throw IllegalArgumentException()
        }
    }

    private fun clearSessionRequest() {
        WCDelegate.sessionRequestEvent = null
        WCDelegate.currentId = null
        sessionRequestUI = SessionRequestUI.Initial
    }

    private fun generateSessionRequestUI(): SessionRequestUI {
        return if (WCDelegate.sessionRequestEvent != null) {
            val (sessionRequest, context) = WCDelegate.sessionRequestEvent!!
            SessionRequestUI.Content(
                peerUI = PeerUI(
                    peerName = sessionRequest.peerMetaData?.name ?: "",
                    peerIcon = sessionRequest.peerMetaData?.icons?.firstOrNull() ?: "",
                    peerUri = sessionRequest.peerMetaData?.url ?: "",
                    peerDescription = sessionRequest.peerMetaData?.description ?: "",
                    linkMode = sessionRequest.peerMetaData?.linkMode ?: false
                ),
                topic = sessionRequest.topic,
                requestId = sessionRequest.request.id,
                param = if (sessionRequest.request.method == PERSONAL_SIGN_METHOD) extractMessageParamFromPersonalSign(sessionRequest.request.params) else sessionRequest.request.params,
                chain = sessionRequest.chainId,
                method = sessionRequest.request.method,
                peerContextUI = context.toPeerUI()
            )
        } else {
            SessionRequestUI.Initial
        }
    }
}