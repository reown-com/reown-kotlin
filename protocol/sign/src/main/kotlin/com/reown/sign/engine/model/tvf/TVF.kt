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

    fun collect(rpcMethod: String, rpcParams: String, chainId: String): Triple<List<String>, List<String>?, String> {
//        if (rpcMethod !in all) return null

        val contractAddresses = if (rpcMethod == "eth_sendTransaction") {
            runCatching {
                moshi.adapter(Array<EthSendTransaction>::class.java)
                    .fromJson(rpcParams)
                    ?.firstOrNull()
                    ?.takeIf { it.data != "0x" }
                    ?.to
            }.getOrNull()?.let { listOf(it) }
        } else {
            null
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

                SOLANA_SIGN_ALL_TRANSACTION -> {
                    moshi.adapter(SolanaSignAllTransactionsResult::class.java)
                        .fromJson(rpcResult)
                        ?.transactions
                        ?.map { transaction -> extractSignature(transaction) }
                }

                TRON_SIGN_TRANSACTION -> {
                    moshi.adapter(TransactionResponse::class.java)
                        .fromJson(rpcResult)
                        ?.result?.txID
                        ?.let { listOf(it) }
                }

                COSMOS_SIGN_DIRECT -> {
                    moshi.adapter(CosmosSignDirect.SignatureData::class.java)
                        .fromJson(rpcResult)
                        ?.let {
                            val txHash = CosmosSignDirect.computeTxHash(
                                it.signed.bodyBytes,
                                it.signed.authInfoBytes,
                                it.signature.signature
                            )
                            listOf(txHash)
                        }
                }

                COSMOS_SIGN_AMINO -> {
                    moshi.adapter(CosmosSignAmino.SignatureData::class.java)
                        .fromJson(rpcResult)
                        ?.let {
                            val txHash = CosmosSignAmino.computeTxHash(it.signed, it.signature)
                            listOf(txHash)
                        }
                }

                else -> null
            }
        } catch (e: Exception) {
            println("Error processing $rpcMethod - $e")
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
        private const val SUI_SIGN_AND_EXECUTE_TRANSACTION = "sui_signAndExecuteTransaction"
        private const val SUI_SIGN_TRANSACTION = "sui_signTransaction"
        private const val NEAR_SIGN_TRANSACTION = "near_signTransaction"
        private const val NEAR_SIGN_TRANSACTIONS = "near_signTransactions"
        private const val XRPL_SIGN_TRANSACTION = "xrpl_signTransaction"
        private const val XRPL_SIGN_TRANSACTION_FOR = "xrpl_signTransactionFor"
        private const val ALGO_SIGN_TXN = "algo_signTxn"
        private const val POLKADOT_SIGN_TRANSACTION = "polkadot_signTransaction"
        private const val COSMOS_SIGN_DIRECT = "cosmos_signDirect"
        private const val COSMOS_SIGN_AMINO = "cosmos_signAmino"
        private const val TRON_SIGN_TRANSACTION = "tron_signTransaction"
    }
}