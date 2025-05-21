package com.reown.sign.engine.model.tvf

import com.reown.sign.engine.model.tvf.SignTransaction.calculateTransactionDigest
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

internal class TVF(private val moshi: Moshi) {
    private val evm: List<String>
        get() = listOf(ETH_SEND_TRANSACTION, ETH_SEND_RAW_TRANSACTION)
    private val wallet
        get() = listOf(WALLET_SEND_CALLS)

    fun collect(rpcMethod: String, rpcParams: String, chainId: String): Triple<List<String>, List<String>?, String> {
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

    fun collectTxHashes(rpcMethod: String, rpcResult: String, rpcParams: String = ""): List<String>? {
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
                    moshi.adapter(TransactionResult::class.java)
                        .fromJson(rpcResult)
                        ?.txID
                        ?.let { listOf(it) }
                }

                HEDERA_SIGN_AND_EXECUTE_TRANSACTION, HEDERA_EXECUTE_TRANSACTION -> {
                    moshi.adapter(HederaSignAndExecuteTransactionResult::class.java)
                        .fromJson(rpcResult)
                        ?.transactionId
                        ?.let { listOf(it) }
                }

                STX_TRANSFER -> {
                    moshi.adapter(StacksTransactionData::class.java)
                        .fromJson(rpcResult)
                        ?.txid
                        ?.let { listOf(it) }
                }

                SEND_TRANSFER -> {
                    moshi.adapter(BitcoinTransactionResult::class.java)
                        .fromJson(rpcResult)
                        ?.txid
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

                ALGO_SIGN_TXN -> {
                    val listOfStringsType = Types.newParameterizedType(List::class.java, String::class.java)
                    moshi.adapter<List<String>>(listOfStringsType)
                        .fromJson(rpcResult)
                        ?.let { calculateTxIDs(it) }
                }

                XRPL_SIGN_TRANSACTION -> {
                    moshi.adapter(XRPLSignTransaction.TransactionWrapper::class.java)
                        .fromJson(rpcResult)
                        ?.let { listOf(it.tx_json.hash) }
                }

                XRPL_SIGN_TRANSACTION_FOR -> {
                    moshi.adapter(XRPLSignTransactionFor.TransactionWrapper::class.java)
                        .fromJson(rpcResult)
                        ?.let { listOf(it.tx_json.hash) }
                }

                SUI_SIGN_AND_EXECUTE_TRANSACTION -> {
                    moshi.adapter(SignAndExecute.SuiTransactionResponse::class.java)
                        .fromJson(rpcResult)
                        ?.let { listOf(it.digest) }
                }

                SUI_SIGN_TRANSACTION -> {
                    moshi.adapter(SignTransaction.SignatureResult::class.java)
                        .fromJson(rpcResult)
                        ?.let {
                            val digest = calculateTransactionDigest(it.transactionBytes)
                            listOf(digest)
                        }
                }

                NEAR_SIGN_TRANSACTION -> {
                    moshi.adapter(NearSignTransaction.BufferData::class.java)
                        .fromJson(rpcResult)
                        ?.let {
                            val hash = NearSignTransaction.calculateTxID(it)
                            listOf(hash)
                        }
                }

                NEAR_SIGN_TRANSACTIONS -> {
                    val type = Types.newParameterizedType(List::class.java, NearSignTransaction.BufferData::class.java)
                    moshi.adapter<List<NearSignTransaction.BufferData>>(type).fromJson(rpcResult)
                        ?.let { bufferDataList -> bufferDataList.map { NearSignTransaction.calculateTxID(it) } }
                }

                POLKADOT_SIGN_TRANSACTION -> {
                    moshi.adapter(PolkadotSignTransaction.SignatureResponse::class.java)
                        .fromJson(rpcResult)
                        ?.let { signatureResponse ->
                            moshi.adapter(PolkadotSignTransaction.RequestParams::class.java)
                                .fromJson(rpcParams)
                                ?.let { requestParams ->
                                    PolkadotSignTransaction.calculatePolkadotHash(signatureResponse, requestParams)?.let { listOf(it) }
                                }
                        }
                }

                POLKADOT_SIGN_TRANSACTION -> {
                    listOf("")
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
        private const val POLKADOT_SIGN_TRANSACTION = "polkadot_signTransaction"
        private const val NEAR_SIGN_TRANSACTION = "near_signTransaction"
        private const val NEAR_SIGN_TRANSACTIONS = "near_signTransactions"
        private const val SUI_SIGN_AND_EXECUTE_TRANSACTION = "sui_signAndExecuteTransaction"
        private const val SUI_SIGN_TRANSACTION = "sui_signTransaction"
        private const val XRPL_SIGN_TRANSACTION = "xrpl_signTransaction"
        private const val XRPL_SIGN_TRANSACTION_FOR = "xrpl_signTransactionFor"
        private const val ALGO_SIGN_TXN = "algo_signTxn"
        private const val COSMOS_SIGN_DIRECT = "cosmos_signDirect"
        private const val TRON_SIGN_TRANSACTION = "tron_signTransaction"
        private const val HEDERA_SIGN_AND_EXECUTE_TRANSACTION = "hedera_signAndExecuteTransaction"
        private const val HEDERA_EXECUTE_TRANSACTION = "hedera_executeTransaction"
        private const val STX_TRANSFER = "stx_transferStx"
        private const val SEND_TRANSFER = "sendTransfer"

        fun toBase58(bytes: ByteArray): String {
            val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
            var leadingZeros = 0
            for (b in bytes) {
                if (b == 0.toByte()) leadingZeros++ else break
            }

            val input = bytes.copyOf()
            val encoded = mutableListOf<Char>()

            while (input.any { it != 0.toByte() }) {
                var carry = 0
                for (i in input.indices) {
                    carry = carry * 256 + (input[i].toInt() and 0xFF)
                    input[i] = (carry / 58).toByte()
                    carry %= 58
                }
                encoded.add(alphabet[carry])
            }

            val result = StringBuilder()
            repeat(leadingZeros) { result.append('1') }
            encoded.reversed().forEach { result.append(it) }
            return result.toString()
        }
    }
}