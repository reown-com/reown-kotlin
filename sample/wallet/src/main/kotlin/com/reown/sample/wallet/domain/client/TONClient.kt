package com.reown.sample.wallet.domain.client

import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.domain.account.TONAccountDelegate
import uniffi.yttrium.Logger
import uniffi.yttrium.PulseMetadata
import uniffi.yttrium.SendTxMessage
import uniffi.yttrium.TonClient
import uniffi.yttrium.TonClientConfig
import uniffi.yttrium.registerLogger

data class Keypair(val secretKey: String, val publicKey: String)
data class Wallet(val raw: String, val friendly: String)

object TONClient {
    private lateinit var client: TonClient

    fun init(packageName: String) {
        val config = TonClientConfig(TONAccountDelegate.mainnet)

        registerLogger(object : Logger {
            override fun log(message: String) {
                println("kobe: From Yttrium: $message")
            }

        })

        //mainnet: -239
        //testnet: -3
        client = TonClient(
            config, projectId = BuildConfig.PROJECT_ID, pulseMetadata = PulseMetadata(
                sdkPlatform = "mobile",
                sdkVersion = "reown-kotlin-${BuildConfig.BOM_VERSION}",
                bundleId = packageName,
                url = null
            )
        )
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
            Wallet(wallet.rawHex, wallet.friendly)
        } catch (e: Exception) {
            println("Error getting address from keypair: ${e.message}")
            throw e
        }
    }

    fun signData(text: String): String {
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

    suspend fun sendMessage(from: String, validUntil: UInt, messages: List<SendTxMessage>): String {
        return try {
            if (!::client.isInitialized) {
                throw IllegalStateException("TONClient not initialized. Call init() first.")
            }

            client.sendMessage(
                "${TONAccountDelegate.mainnet}",
                from,
                uniffi.yttrium.Keypair(TONAccountDelegate.secretKey, TONAccountDelegate.publicKey),
                validUntil,
                messages
            ).also {
                println("kobe: sent message RESULT: $it")
            }
        } catch (e: Exception) {
            println("Error sending message: ${e.message}")
            throw e
        }
    }
}