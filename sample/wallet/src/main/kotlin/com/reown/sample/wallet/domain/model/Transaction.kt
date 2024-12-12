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
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Numeric
import org.web3j.utils.Numeric.toBigInt
import java.math.BigDecimal
import java.math.BigInteger

object Transaction {
    suspend fun send(sessionRequest: Wallet.Model.SessionRequest): String {
        val transaction = getTransaction(sessionRequest)
        val nonceResult = getNonce(transaction.chainId, transaction.from)
        val signedTransaction = sign(transaction, nonceResult, DefaultGasProvider.GAS_LIMIT)
        val txHash = sendRaw(transaction.chainId, signedTransaction)
        return txHash
    }

    fun getTransaction(sessionRequest: Wallet.Model.SessionRequest): Wallet.Model.Transaction {
        val requestParams = JSONArray(sessionRequest.request.params).getJSONObject(0)
        val from = requestParams.getString("from")
        val to = requestParams.getString("to")
        val data = requestParams.getString("data")
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

        return Wallet.Model.Transaction(
            from = from,
            to = to,
            value = value,
            data = data,
            nonce = nonce,
            gas = gas,
            gasPrice = "0", //todo: will be removed
            chainId = sessionRequest.chainId!!,
            maxPriorityFeePerGas = "0", //todo: will be removed
            maxFeePerGas = "0" //todo: will be removed
        )
    }

    @OptIn(ChainAbstractionExperimentalApi::class)
    fun sign(transaction: Wallet.Model.Transaction, nonce: BigInteger? = null, gasLimit: BigInteger? = null): String {
        val fees = WalletKit.estimateFees(transaction.chainId)
        val chainId = transaction.chainId.split(":")[1].toLong()
        if (transaction.nonce.startsWith("0x")) {
            transaction.nonce = hexToBigDecimal(transaction.nonce)?.toBigInteger().toString()
        }
        if (transaction.gas.startsWith("0x")) {
            transaction.gas = hexToBigDecimal(transaction.gas)?.toBigInteger().toString()
        }
        if (transaction.value.startsWith("0x")) {
            transaction.value = hexToBigDecimal(transaction.value)?.toBigInteger().toString()
        }

        println("kobe: fees: $fees")

        println("kobe: chainId: $chainId")
        println("kobe: nonce: ${nonce ?: transaction.nonce.toBigInteger()}")
        println("kobe: gas: ${gasLimit ?: transaction.gas.toBigInteger()}")

        println("kobe: value: ${transaction.value}")
        println("kobe: maxFeePerGas: ${fees.maxFeePerGas.toBigInteger()}")
        println("kobe: maxPriorityFeePerGas: ${fees.maxPriorityFeePerGas.toBigInteger()}")
        println("kobe: //////////////////////////////////////")

        val rawTransaction = RawTransaction.createTransaction(
            chainId,
            nonce ?: transaction.nonce.toBigInteger(),
            gasLimit ?: transaction.gas.toBigInteger(),
            transaction.to,
            transaction.value.toBigInteger(),
            transaction.data,
            fees.maxPriorityFeePerGas.toBigInteger(),
            fees.maxFeePerGas.toBigInteger(),
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

    suspend fun getReceipt(chainId: String, txHash: String) {
        withTimeout(30000) {
            while (true) {
                val service = createBlockChainApiService(BuildConfig.PROJECT_ID, chainId)
                val nonceRequest = JsonRpcRequest(
                    method = "eth_getTransactionReceipt",
                    params = listOf(txHash),
                    id = generateId()
                )

                val receipt = async { service.sendJsonRpcRequest(nonceRequest) }.await()
                when {
                    receipt.error != null -> throw Exception("Getting tx receipt failed: ${receipt.error.message}")
                    receipt.result == null -> delay(3000)
                    else -> {
                        println("kobe: receipt: $receipt")
                        break
                    }
                }
            }
        }
    }

    fun hexToBigDecimal(input: String): BigDecimal? {
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

    fun hexToTokenAmount(hexValue: String, decimals: Int): BigDecimal? {
        return try {
            val cleanedHex = hexValue.removePrefix("0x")
            val amountBigInt = cleanedHex.toBigInteger(16)
            val divisor = BigDecimal.TEN.pow(decimals)
            BigDecimal(amountBigInt).divide(divisor)
        } catch (e: NumberFormatException) {
            println("Invalid hexadecimal value: $hexValue")
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