package com.reown.sample.wallet.domain.client

import com.reown.sample.wallet.domain.account.TONAccountDelegate
import org.web3j.protocol.Network
import uniffi.yttrium.TonClient
import uniffi.yttrium.TonClientConfig

data class Keypair(val secretKey: String, val publicKey: String)
data class Wallet(val raw: String, val friendly: String)

object TONClient {
    private lateinit var client: TonClient

    fun init() {
        val config = TonClientConfig("-239")
        client = TonClient(config)
    }

    fun generateKeyPair(): Keypair {
        return try {
            if (!::client.isInitialized) {
                throw IllegalStateException("TONClient not initialized. Call init() first.")
            }
            val keyPair = client.generateKeypair()
            Keypair(keyPair.sk, keyPair.pk)
        } catch (e: Exception) {
            println("Error generating keypair: ${e.message}")
            throw e
        }
    }

    fun getAddressFromKeyPair(keyPair: Keypair): Wallet {
        return try {
            if (!::client.isInitialized) {
                throw IllegalStateException("TONClient not initialized. Call init() first.")
            }

            val wallet = client.getAddressFromKeypair(uniffi.yttrium.Keypair(keyPair.secretKey, keyPair.publicKey))
            Wallet(wallet.rawHex, wallet.friendlyEq)
        } catch (e: Exception) {
            println("Error getting address from keypair: ${e.message}")
            throw e
        }
    }

    fun signData(from: String, text: String): String {
        return try {
            if (!::client.isInitialized) {
                throw IllegalStateException("TONClient not initialized. Call init() first.")
            }

            client.signData(text, uniffi.yttrium.Keypair(TONAccountDelegate.secretKey, TONAccountDelegate.publicKey))
        } catch (e: Exception) {
            println("Error signing data: ${e.message}")
            throw e
        }
    }

    fun sendMessage(network: String, from: String): String {
        return try {
            if (!::client.isInitialized) {
                throw IllegalStateException("TONClient not initialized. Call init() first.")
            }

            client.sendMessage(network, from, uniffi.yttrium.Keypair(TONAccountDelegate.secretKey, TONAccountDelegate.publicKey))
        } catch (e: Exception) {
            println("Error sending message: ${e.message}")
            throw e
        }
    }
}