package com.reown.sign.engine.model.tvf

import com.squareup.moshi.Moshi

class TVF(private val moshi: Moshi) {
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
                }.getOrNull()
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
            println("error processing $rpcMethod - $e")
            null
        }
    }

    companion object {
        private const val ETH_SEND_TRANSACTION = "eth_sendTransaction"
        private const val ETH_SEND_RAW_TRANSACTION = "eth_sendRawTransaction"
        private const val WALLET_SEND_CALLS = "wallet_sendCalls"
        private const val SOLANA_SIGN_TRANSACTION = "solana_signTransaction"
        private const val SOLANA_SIGN_AND_SEND_TRANSACTION = "solana_signAndSendTransaction"
        private const val SOLANA_SIGN_ALL_TRANSACTION = "solana_signAllTransactions"
    }
}