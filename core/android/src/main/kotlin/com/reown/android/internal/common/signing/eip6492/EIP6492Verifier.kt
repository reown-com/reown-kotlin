package com.reown.android.internal.common.signing.eip6492

import kotlinx.coroutines.runBlocking
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import uniffi.yttrium.Erc6492Client

//TODO: move to Sign SDK
object EIP6492Verifier {
    private lateinit var erc6492Client: Erc6492Client

    fun init(chainId: String, projectId: String) {
        try {
            val rpcUrl = "https://rpc.walletconnect.com/v1?chainId=$chainId&projectId=$projectId"
            erc6492Client = Erc6492Client(rpcUrl)
        } catch (e: Exception) {
            println("init error: $e")
        }
    }

    fun verify6492(originalMessage: String, address: String, signature: String) =
        try {
            val messageHashBytes = Sign.getEthereumMessageHash(originalMessage.toByteArray())
            val messageHashHex = Numeric.toHexString(messageHashBytes)
            runBlocking { erc6492Client.verifySignature(signature = signature, address = address, messageHash = messageHashHex) }
        } catch (e: Exception) {
            println("Error in verify6492: ${e.message}")
            e.printStackTrace()
            false
        }
}