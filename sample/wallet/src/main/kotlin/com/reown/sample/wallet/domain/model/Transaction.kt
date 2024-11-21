package com.reown.sample.wallet.domain.model

import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.blockchain.JsonRpcRequest
import com.reown.sample.wallet.blockchain.createBlockChainApiService
import com.reown.sample.wallet.domain.EthAccountDelegate
import com.reown.sample.wallet.domain.WCDelegate
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import org.json.JSONArray
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Numeric
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

    fun sign(transaction: Wallet.Model.Transaction, nonce: String? = null, gasLimit: BigInteger? = null): String {
        val fees = WalletKit.estimateFees(transaction.chainId)
        val chainId = transaction.chainId.split(":")[1].toLong()
        val gas = hexToBigDecimal(transaction.gas)

        println("kobe: fees: $fees")
        println("kobe: gas: $gas")
        println("kobe: maxFeePerGas: ${fees.maxFeePerGas.toBigInteger()}")
        println("kobe: maxPriorityFeePerGas: ${fees.maxPriorityFeePerGas.toBigInteger()}")
        println("kobe: //////////////////////////////////////")

        val rawTransaction = RawTransaction.createTransaction(
            chainId,
            Numeric.toBigInt(nonce ?: transaction.nonce),
            gasLimit ?: Numeric.toBigInt(transaction.gas),
            transaction.to,
            Numeric.toBigInt(transaction.value),
            transaction.data,
            fees.maxPriorityFeePerGas.toBigInteger(),
            fees.maxFeePerGas.toBigInteger(),
        )

        return Numeric.toHexString(TransactionEncoder.signMessage(rawTransaction, Credentials.create(EthAccountDelegate.privateKey)))
    }

    suspend fun getNonce(chainId: String, from: String): String {
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
                nonceResult.result as String
            }
        }
    }

    suspend fun sendRaw(chainId: String, signedTx: String): String {
        return coroutineScope {
            supervisorScope {
                val service = createBlockChainApiService(BuildConfig.PROJECT_ID, chainId)
                val request = JsonRpcRequest(
                    method = "eth_sendRawTransaction",
                    params = listOf(signedTx),
                    id = generateId()
                )
                val resultTx = async { service.sendJsonRpcRequest(request) }.await()

                if (resultTx.error != null) {
                    throw Exception("Route transaction failed: ${resultTx.error.message}")
                } else {
                    resultTx.result as String
                }
            }
        }
    }

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

    private fun generateId(): Int = ("${(100..999).random()}").toInt()

    private fun containsHexPrefix(input: String): Boolean = input.startsWith("0x")
    private const val HEX = 16
    private const val DEC = 10
}