package com.reown.sign.engine.model.tvf

import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.wcKoinApp
import com.squareup.moshi.Moshi
import org.koin.core.qualifier.named

object TVF {
    val moshi: Moshi = wcKoinApp.koin.get<Moshi.Builder>(named(AndroidCommonDITags.MOSHI)).build()

    private val evm: List<String>
        get() = listOf(ETH_SEND_TRANSACTION, ETH_SEND_RAW_TRANSACTION)
    private val solana
        get() = listOf(SOLANA_SIGN_TRANSACTION, SOLANA_SIGN_AND_SEND_TRANSACTION, SOLANA_SIGN_ALL_TRANSACTION)
    private val wallet
        get() = listOf(WALLET_SEND_CALLS)

    private val all get() = evm + solana + wallet

    fun collect(rpcMethod: String, rpcParams: String, chainId: String): Triple<List<String>, List<String>?, String>? {
        if (rpcMethod !in all) return null

        val contractAddresses = when (rpcMethod) {
            "eth_sendTransaction" -> {
                runCatching {
                    moshi.adapter(Array<EthSendTransaction>::class.java)
                        .fromJson(rpcParams)
                        ?.firstOrNull()
                        ?.to
                        ?.let { listOf(it) }
                        ?: listOf("")
                }.getOrElse {
                    println("Error parsing rpcParams: $it")
                    listOf("")
                }
            }

            else -> null
        }

        return Triple(listOf(rpcMethod), contractAddresses, chainId)
    }

    fun collectTxHashes(rpcMethod: String, rpcResult: String): List<String>? {
        return try {
            when (rpcMethod) {
                in evm + wallet -> listOf(rpcResult)

                SOLANA_SIGN_TRANSACTION ->
                    moshi.adapter(SolanaSignTransactionResult::class.java)
                        .fromJson(rpcResult)
                        ?.signature
                        ?.let { listOf(it) }

                SOLANA_SIGN_AND_SEND_TRANSACTION ->
                    moshi.adapter(SolanaSignAndSendTransactionResult::class.java)
                        .fromJson(rpcResult)
                        ?.signature
                        ?.let { listOf(it) }

                SOLANA_SIGN_ALL_TRANSACTION ->
                    moshi.adapter(SolanaSignAllTransactionsResult::class.java)
                        .fromJson(rpcResult)
                        ?.transactions

                else -> null
            }
        } catch (e: Exception) {
            println("kobe: error processing $rpcMethod - $e")
            null
        }
    }

    private const val ETH_SEND_TRANSACTION = "eth_sendTransaction"
    private const val ETH_SEND_RAW_TRANSACTION = "eth_sendRawTransaction"
    private const val WALLET_SEND_CALLS = "wallet_sendCalls"
    private const val SOLANA_SIGN_TRANSACTION = "solana_signTransaction"
    private const val SOLANA_SIGN_AND_SEND_TRANSACTION = "solana_signAndSendTransaction"
    private const val SOLANA_SIGN_ALL_TRANSACTION = "solana_signAllTransactions"
}