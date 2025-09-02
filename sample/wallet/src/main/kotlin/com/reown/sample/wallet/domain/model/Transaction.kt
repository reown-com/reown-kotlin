package com.reown.sample.wallet.domain.model

import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.blockchain.JsonRpcRequest
import com.reown.sample.wallet.blockchain.createBlockChainApiService
import com.reown.sample.wallet.domain.EthAccountDelegate
import com.reown.walletkit.client.ChainAbstractionExperimentalApi
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

object Transaction {
    suspend fun send(sessionRequest: Wallet.Model.SessionRequest): String {
        val transaction = getTransaction(sessionRequest)
        val nonceResult = getNonce(transaction.chainId, transaction.from)
        val signedTransaction = sign(transaction, nonceResult, DefaultGasProvider.GAS_LIMIT)
        val txHash = sendRaw(transaction.chainId, signedTransaction)
        return txHash
    }

    @OptIn(ChainAbstractionExperimentalApi::class)
    fun getTransaction(sessionRequest: Wallet.Model.SessionRequest): Wallet.Model.FeeEstimatedTransaction {
//        val fees = WalletKit.estimateFees(sessionRequest.chainId!!)
        val requestParams = JSONArray(sessionRequest.request.params).getJSONObject(0)
        val from = requestParams.getString("from")
        val to = requestParams.getString("to")
        val data = requestParams.getString("data")
        val maxFeePerGas = try {
            requestParams.getString("maxFeePerGas")
        } catch (e: Exception) {
            "0"
        }
        val maxPriorityFeePerGas = try {
            requestParams.getString("maxPriorityFeePerGas")
        } catch (e: Exception) {
            "0"
        }

        val value = try {
            requestParams.getString("value")
        } catch (e: Exception) {
            "0"
        }
        val nonce = try {
            requestParams.getString("nonce")
        } catch (e: Exception) {
            "0"
        }
        val gas = try {
            requestParams.getString("gas")
        } catch (e: Exception) {
            "0"
        }

        return Wallet.Model.FeeEstimatedTransaction(
            from = from,
            to = to,
            value = value,
            input = data,
            nonce = nonce,
            gasLimit = gas,
            chainId = sessionRequest.chainId!!,
            //TODO: add getting fees
            maxFeePerGas = maxFeePerGas,
            maxPriorityFeePerGas = maxPriorityFeePerGas
        )
    }

    fun getInitialTransaction(sessionRequest: Wallet.Model.SessionRequest): Wallet.Model.InitialTransaction {
        val requestParams = JSONArray(sessionRequest.request.params).getJSONObject(0)
        val from = requestParams.getString("from")
        val to = requestParams.getString("to")
        val data = try {
            requestParams.getString("data")
        } catch (e: Exception) {
            "0x"
        }
        val value = try {
            requestParams.getString("value")
        } catch (e: Exception) {
            "0"
        }
        val gas = try {
            requestParams.getString("gas")
        } catch (e: Exception) {
            "0"
        }

        return Wallet.Model.InitialTransaction(
            from = from,
            to = to,
            value = value,
            chainId = sessionRequest.chainId!!,
            input = data,
        )
    }

    fun sign(transaction: Wallet.Model.FeeEstimatedTransaction, nonce: BigInteger? = null, gasLimit: BigInteger? = null): String {
        val chainId = transaction.chainId.split(":")[1].toLong()
        if (transaction.nonce.startsWith("0x")) {
            transaction.nonce = hexToBigDecimal(transaction.nonce)?.toBigInteger().toString()
        }

        if (transaction.gasLimit.startsWith("0x")) {
            transaction.gasLimit = hexToBigDecimal(transaction.gasLimit)?.toBigInteger().toString()
        }
        if (transaction.value.startsWith("0x")) {
            transaction.value = hexToBigDecimal(transaction.value)?.toBigInteger().toString()
        }
        if (transaction.maxFeePerGas.startsWith("0x")) {
            transaction.maxFeePerGas = hexToBigDecimal(transaction.maxFeePerGas)?.toBigInteger().toString()
        }
        if (transaction.maxPriorityFeePerGas.startsWith("0x")) {
            transaction.maxPriorityFeePerGas = hexToBigDecimal(transaction.maxPriorityFeePerGas)?.toBigInteger().toString()
        }

        println("chainId: $chainId")
        println("nonce: ${nonce ?: transaction.nonce.toBigInteger()}")
        println("gas: ${gasLimit ?: transaction.gasLimit.toBigInteger()}")
        println("value: ${transaction.value}")
        println("maxFeePerGas: ${transaction.maxFeePerGas.toBigInteger()}")
        println("maxPriorityFeePerGas: ${transaction.maxPriorityFeePerGas.toBigInteger()}")
        println("//////////////////////////////////////")

        val rawTransaction = RawTransaction.createTransaction(
            chainId,
            nonce ?: transaction.nonce.toBigInteger(),
            gasLimit ?: transaction.gasLimit.toBigInteger(),
            transaction.to,
            transaction.value.toBigInteger(),
            transaction.input,
            transaction.maxPriorityFeePerGas.toBigInteger(),
            transaction.maxFeePerGas.toBigInteger(),
        )

        return Numeric.toHexString(TransactionEncoder.signMessage(rawTransaction, Credentials.create(EthAccountDelegate.privateKey)))
    }

    suspend fun getNonce(chainId: String, from: String): BigInteger {
        return coroutineScope {
            val service = createBlockChainApiService(BuildConfig.PROJECT_ID, chainId)
            val nonceRequest = JsonRpcRequest(
                method = "eth_getTransactionCount",
                params = listOf(from, "latest"),
                id = generateId()
            )

            val nonceResult = async { service.sendJsonRpcRequest(nonceRequest) }.await()
            if (nonceResult.error != null) {
                throw Exception("Getting nonce failed: ${nonceResult.error.message}")
            } else {
                val nonceHex = nonceResult.result as String
                hexToBigDecimal(nonceHex)?.toBigInteger() ?: throw Exception("Getting nonce failed")
            }
        }
    }

    suspend fun getBalance(chainId: String, from: String): String {
        return coroutineScope {
            val service = createBlockChainApiService(BuildConfig.PROJECT_ID, chainId)
            val nonceRequest = JsonRpcRequest(
                method = "eth_getBalance",
                params = listOf(from, "latest"),
                id = generateId()
            )

            val nonceResult = async { service.sendJsonRpcRequest(nonceRequest) }.await()
            if (nonceResult.error != null) {
                throw Exception("Getting nonce failed: ${nonceResult.error.message}")
            } else {
                nonceResult.result as String
            }
        }
    }

    suspend fun sendRaw(chainId: String, signedTx: String, txType: String = ""): String {
        val service = createBlockChainApiService(BuildConfig.PROJECT_ID, chainId)
        val request = JsonRpcRequest(
            method = "eth_sendRawTransaction",
            params = listOf(signedTx),
            id = generateId()
        )
        val resultTx = service.sendJsonRpcRequest(request)

        if (resultTx.error != null) {
            throw Exception("$txType transaction failed: ${resultTx.error.message}")
        } else {
            return resultTx.result as String
        }
    }

//    suspend fun getReceipt(chainId: String, txHash: String) {
//        withTimeout(60000) {
//            while (true) {
//                val service = createBlockChainApiService(BuildConfig.PROJECT_ID, chainId)
//                val nonceRequest = JsonRpcRequest(
//                    method = "eth_getTransactionReceipt",
//                    params = listOf(txHash),
//                    id = generateId()
//                )
//
//                val receipt = async { service.sendJsonRpcRequest(nonceRequest) }.await()
//                when {
//                    receipt.error != null -> throw Exception("Getting tx receipt failed: ${receipt.error.message}")
//                    receipt.result == null -> delay(3000)
//                    else -> {
//                        println("receipt: $receipt")
//                        break
//                    }
//                }
//            }
//        }
//    }

    private fun hexToBigDecimal(input: String): BigDecimal? {
        val trimmedInput = input.trim()
        var hex = trimmedInput
        return if (hex.isEmpty()) {
            null
        } else try {
            val isHex: Boolean = containsHexPrefix(hex)
            if (isHex) {
                hex = Numeric.cleanHexPrefix(trimmedInput)
            }
            BigInteger(hex, if (isHex) HEX else DEC).toBigDecimal()
        } catch (ex: NullPointerException) {
            null
        } catch (ex: NumberFormatException) {
            null
        }
    }

    fun hexToTokenAmount(value: String, decimals: Int): BigDecimal? {
        return try {
            if (value.startsWith("0x")) {
                val cleanedHex = value.removePrefix("0x")
                val divisor = BigDecimal.TEN.pow(decimals)
                BigDecimal(cleanedHex.toBigInteger(16)).divide(divisor)
            } else {
                BigDecimal(value)//.setScale(4, RoundingMode.HALF_UP)
            }


        } catch (e: NumberFormatException) {
            println("Invalid hexadecimal value: $value")
            null
        }
    }

    fun convertTokenAmount(value: BigInteger, decimals: Int): BigDecimal? {
        return try {
            val divisor = BigDecimal.TEN.pow(decimals)
            BigDecimal(value).divide(divisor)
        } catch (e: NumberFormatException) {
            println("Invalid value: $value")
            null
        }
    }

    private fun generateId(): Int = ("${(100..999).random()}").toInt()

    private fun containsHexPrefix(input: String): Boolean = input.startsWith("0x")
    private const val HEX = 16
    private const val DEC = 10
}