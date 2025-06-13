package com.reown.android.internal.common.signing.eip6492

import com.reown.android.internal.common.signing.cacao.toCAIP222Message
import com.reown.android.internal.common.signing.signature.Signature
import com.reown.android.internal.common.signing.signature.toHexSignature
import com.reown.util.bytesToHex
import kotlinx.coroutines.runBlocking
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import uniffi.yttrium.Erc6492Client

object EIP6492Verifier {
    private lateinit var erc6492Client: Erc6492Client

    fun init(chainId: String, projectId: String) {
        println("kobe: $chainId $projectId")
        try {
            val rpcUrl = "https://rpc.walletconnect.com/v1?chainId=$chainId&projectId=$projectId"
            erc6492Client = Erc6492Client(rpcUrl)
        } catch (e: Exception) {
            println("kobe: init error: $e")
        }
    }

    fun verify6492(originalMessage: String, address: String, signature: String) = try {
        println("kobe: verify6492: $originalMessage")
        println("kobe: $address $signature")

        val client = erc6492Client
        val messageHashBytes = Sign.getEthereumMessageHash(originalMessage.toByteArray())
        val messageHashHex = Numeric.toHexString(messageHashBytes)
        println("kobe: mess hash: $messageHashHex")

        val sigHex = Signature.fromString(signature).toHexSignature()
        println("kobe: sig: $signature")

        runBlocking {
            client.verifySignature(signature = signature, address = address, messageHash = messageHashHex)
        }
    } catch (e: Exception) {
        false
    }
}