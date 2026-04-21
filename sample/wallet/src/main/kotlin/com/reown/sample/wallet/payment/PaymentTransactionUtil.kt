@file:JvmSynthetic

package com.reown.sample.wallet.payment

import android.util.Log
import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.blockchain.JsonRpcRequest
import com.reown.sample.wallet.blockchain.createBlockChainApiService
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.walletkit.client.Wallet
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

internal object PaymentTransactionUtil {

    private const val TAG = "PaymentTransactionUtil"

    private val POLYGON_MIN_PRIORITY_FEE_WEI = BigInteger("30000000000") // 30 gwei
    private val DEFAULT_PRIORITY_FEE_WEI = BigInteger("1500000000")      // 1.5 gwei (ethers.js v5 default)
    private const val TX_CONFIRMATION_TIMEOUT_MS = 120_000L
    private const val TX_RECEIPT_POLL_INTERVAL_MS = 3_000L
    private const val GAS_ESTIMATION_RPC_TIMEOUT_MS = 15_000L
    private const val POLYGON_CHAIN_ID = "eip155:137"

    private val NATIVE_SYMBOL_BY_CHAIN_ID = mapOf(
        "eip155:1" to "ETH",
        "eip155:10" to "ETH",
        "eip155:11155420" to "ETH",
        "eip155:42161" to "ETH",
        "eip155:8453" to "ETH",
        "eip155:1313161554" to "ETH",
        "eip155:7777777" to "ETH",
        "eip155:137" to "POL",
        "eip155:56" to "BNB",
        "eip155:43114" to "AVAX",
        "eip155:43113" to "AVAX",
        "eip155:250" to "FTM",
        "eip155:100" to "XDAI",
        "eip155:9001" to "EVMOS",
        "eip155:324" to "ETH",
        "eip155:314" to "FIL",
        "eip155:4689" to "IOTX",
        "eip155:1088" to "METIS",
        "eip155:1284" to "GLMR",
        "eip155:1285" to "MOVR",
        "eip155:42220" to "CELO",
        "eip155:143" to "MON",
    )

    private data class FeeData(
        val gasPrice: BigInteger?,
        val baseFeePerGas: BigInteger?,
        val maxPriorityFeePerGas: BigInteger,
        val maxFeePerGas: BigInteger?,
    )

    private data class FreshTx(
        val chainId: String,
        val to: String,
        val data: String,
        val value: BigInteger,
        val gasLimit: BigInteger,
        val maxFeePerGas: BigInteger,
        val maxPriorityFeePerGas: BigInteger,
    )

    /**
     * Estimate the fee for the given approval action, returning a human-readable
     * string like `~0.0012 POL` or null if estimation fails.
     */
    suspend fun estimateApprovalFee(action: Wallet.Model.WalletRpcAction): String? = runCatching {
        val tx = parseTxParam(action.params) ?: return null
        val from = tx.optString("from").ifBlank { EthAccountDelegate.address }

        coroutineScope {
            val gasLimitDeferred = async {
                withTimeout(GAS_ESTIMATION_RPC_TIMEOUT_MS) { rpcEstimateGas(action.chainId, tx, from) }
            }
            val feeDataDeferred = async {
                withTimeout(GAS_ESTIMATION_RPC_TIMEOUT_MS) { fetchFeeData(action.chainId) }
            }

            val gasLimit = gasLimitDeferred.await()
            val feeData = feeDataDeferred.await()
            val fresh = applyFreshFees(action.chainId, tx, feeData)
            val feePerGas = fresh.maxFeePerGas

            formatGasEstimate(gasLimit.multiply(feePerGas), action.chainId)
        }
    }.getOrElse { error ->
        Log.w(TAG, "estimateApprovalFee failed for ${action.chainId}: ${error.message}")
        null
    }

    /**
     * Sign and broadcast `eth_sendTransaction` with freshly fetched fee data.
     * Returns the transaction hash. The caller is responsible for awaiting confirmation.
     */
    suspend fun sendTransactionWithFreshFees(action: Wallet.Model.WalletRpcAction): String {
        val tx = parseTxParam(action.params) ?: error("Invalid eth_sendTransaction params")
        val from = tx.optString("from").ifBlank { EthAccountDelegate.address }

        val feeData = runCatching { fetchFeeData(action.chainId) }
            .onFailure { Log.w(TAG, "Failed to fetch fresh fees for ${action.chainId}: ${it.message}") }
            .getOrNull()

        val baseFresh = if (feeData != null) {
            applyFreshFees(action.chainId, tx, feeData)
        } else {
            // Fall back to whatever the merchant supplied in the action.
            FreshTx(
                chainId = action.chainId,
                to = tx.getString("to"),
                data = tx.optString("data", "0x").ifBlank { "0x" },
                value = parseHexBigInteger(tx.optString("value"), default = BigInteger.ZERO),
                gasLimit = parseTxGasLimit(tx),
                maxFeePerGas = parseHexBigInteger(tx.optString("maxFeePerGas"), default = BigInteger.ZERO),
                maxPriorityFeePerGas = parseHexBigInteger(tx.optString("maxPriorityFeePerGas"), default = BigInteger.ZERO),
            )
        }

        // Merchants commonly omit gas/gasLimit — estimate it ourselves and add a
        // 20% buffer so the tx can't fail with `intrinsic gas too low`.
        val fresh = if (baseFresh.gasLimit <= BigInteger.ZERO) {
            val estimated = rpcEstimateGas(action.chainId, tx, from)
            val withBuffer = estimated.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100))
            baseFresh.copy(gasLimit = withBuffer)
        } else {
            baseFresh
        }

        val nonce = rpcGetTransactionCount(action.chainId, from)

        val numericChainId = action.chainId.substringAfter(":").toLong()
        val rawTx = RawTransaction.createTransaction(
            numericChainId,
            nonce,
            fresh.gasLimit,
            fresh.to,
            fresh.value,
            fresh.data,
            fresh.maxPriorityFeePerGas,
            fresh.maxFeePerGas,
        )
        val signed = Numeric.toHexString(
            TransactionEncoder.signMessage(rawTx, Credentials.create(EthAccountDelegate.privateKey))
        )

        return rpcSendRawTransaction(action.chainId, signed)
    }

    /**
     * Poll `eth_getTransactionReceipt` until the transaction has at least one confirmation
     * or the timeout elapses.
     */
    suspend fun waitForTransactionConfirmation(
        chainId: String,
        txHash: String,
        timeoutMs: Long = TX_CONFIRMATION_TIMEOUT_MS,
    ) {
        withTimeout(timeoutMs) {
            while (true) {
                val receipt = rpcGetTransactionReceipt(chainId, txHash)
                if (receipt != null) {
                    val status = receipt.optString("status")
                    if (status == "0x0") error("Transaction $txHash reverted on chain $chainId")
                    return@withTimeout
                }
                delay(TX_RECEIPT_POLL_INTERVAL_MS)
            }
        }
    }

    // ---- RPC helpers --------------------------------------------------------

    private suspend fun rpcEstimateGas(chainId: String, tx: JSONObject, from: String): BigInteger {
        val callObject = JSONObject().apply {
            put("from", from)
            tx.optString("to").takeIf { it.isNotBlank() }?.let { put("to", it) }
            tx.optString("data").takeIf { it.isNotBlank() }?.let { put("data", it) }
            tx.optString("value").takeIf { it.isNotBlank() }?.let { put("value", it) }
        }
        val service = createBlockChainApiService(BuildConfig.PROJECT_ID, chainId)
        val response = service.sendJsonRpcRequest(
            JsonRpcRequest(method = "eth_estimateGas", params = listOf(callObject.toMap()), id = randomId())
        )
        if (response.error != null) error("eth_estimateGas failed: ${response.error.message}")
        return parseHexBigInteger(response.result as? String, default = BigInteger.ZERO)
    }

    private suspend fun fetchFeeData(chainId: String): FeeData = coroutineScope {
        val service = createBlockChainApiService(BuildConfig.PROJECT_ID, chainId)
        val gasPriceDeferred = async {
            val resp = service.sendJsonRpcRequest(
                JsonRpcRequest(method = "eth_gasPrice", params = emptyList(), id = randomId())
            )
            (resp.result as? String)?.let { parseHexBigInteger(it, default = BigInteger.ZERO) }
        }
        val blockDeferred = async {
            val resp = service.sendJsonRpcRequest(
                JsonRpcRequest(method = "eth_getBlockByNumber", params = listOf("latest", false), id = randomId())
            )
            @Suppress("UNCHECKED_CAST")
            (resp.result as? Map<String, Any?>)?.get("baseFeePerGas") as? String
        }

        val gasPrice = gasPriceDeferred.await()
        val baseFee = blockDeferred.await()?.let { parseHexBigInteger(it, default = BigInteger.ZERO) }

        FeeData(
            gasPrice = gasPrice,
            baseFeePerGas = baseFee,
            maxPriorityFeePerGas = DEFAULT_PRIORITY_FEE_WEI,
            maxFeePerGas = null,
        )
    }

    private suspend fun rpcGetTransactionCount(chainId: String, from: String): BigInteger {
        val service = createBlockChainApiService(BuildConfig.PROJECT_ID, chainId)
        val response = service.sendJsonRpcRequest(
            JsonRpcRequest(method = "eth_getTransactionCount", params = listOf(from, "pending"), id = randomId())
        )
        if (response.error != null) error("eth_getTransactionCount failed: ${response.error.message}")
        return parseHexBigInteger(response.result as? String, default = BigInteger.ZERO)
    }

    private suspend fun rpcSendRawTransaction(chainId: String, signedTx: String): String {
        val service = createBlockChainApiService(BuildConfig.PROJECT_ID, chainId)
        val response = service.sendJsonRpcRequest(
            JsonRpcRequest(method = "eth_sendRawTransaction", params = listOf(signedTx), id = randomId())
        )
        if (response.error != null) error("eth_sendRawTransaction failed: ${response.error.message}")
        return response.result as? String ?: error("eth_sendRawTransaction returned no result")
    }

    private suspend fun rpcGetTransactionReceipt(chainId: String, txHash: String): JSONObject? {
        val service = createBlockChainApiService(BuildConfig.PROJECT_ID, chainId)
        val response = service.sendJsonRpcRequest(
            JsonRpcRequest(method = "eth_getTransactionReceipt", params = listOf(txHash), id = randomId())
        )
        if (response.error != null) error("eth_getTransactionReceipt failed: ${response.error.message}")
        @Suppress("UNCHECKED_CAST")
        val map = response.result as? Map<String, Any?> ?: return null
        return JSONObject(map)
    }

    // ---- Fee math -----------------------------------------------------------

    private fun applyFreshFees(chainId: String, originalTx: JSONObject, feeData: FeeData): FreshTx {
        val chainFloor = if (chainId == POLYGON_CHAIN_ID) POLYGON_MIN_PRIORITY_FEE_WEI else null
        val originalMaxPriority = parseHexBigInteger(originalTx.optString("maxPriorityFeePerGas"), default = BigInteger.ZERO)
        val originalMaxFee = parseHexBigInteger(originalTx.optString("maxFeePerGas"), default = BigInteger.ZERO)
        val originalGasLimit = parseTxGasLimit(originalTx)

        val priorityFee = listOfNotNull(chainFloor, feeData.maxPriorityFeePerGas, originalMaxPriority.takeIf { it > BigInteger.ZERO })
            .maxOrNull() ?: DEFAULT_PRIORITY_FEE_WEI

        val maxFee = listOfNotNull(
            feeData.baseFeePerGas?.multiply(BigInteger.TWO)?.add(priorityFee),
            feeData.gasPrice,
            originalMaxFee.takeIf { it > BigInteger.ZERO },
            priorityFee,
        ).maxOrNull() ?: priorityFee

        return FreshTx(
            chainId = chainId,
            to = originalTx.getString("to"),
            data = originalTx.optString("data", "0x").ifBlank { "0x" },
            value = parseHexBigInteger(originalTx.optString("value"), default = BigInteger.ZERO),
            gasLimit = originalGasLimit,
            maxFeePerGas = maxFee,
            maxPriorityFeePerGas = priorityFee,
        )
    }

    private fun formatGasEstimate(totalFeeWei: BigInteger, chainId: String): String {
        val symbol = NATIVE_SYMBOL_BY_CHAIN_ID[chainId] ?: "ETH"
        val ether = Convert.fromWei(BigDecimal(totalFeeWei), Convert.Unit.ETHER)
        if (ether <= BigDecimal.ZERO) return "~$ether $symbol"
        val scale = if (ether >= BigDecimal("0.01")) 4 else 6
        val rounded = ether.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        return "~$rounded $symbol"
    }

    // ---- Parsing helpers ----------------------------------------------------

    private fun parseTxParam(paramsJson: String): JSONObject? = runCatching {
        val arr = JSONArray(paramsJson)
        if (arr.length() == 0) null else arr.getJSONObject(0)
    }.getOrNull()

    private fun parseTxGasLimit(tx: JSONObject): BigInteger {
        // EIP-1474 uses `gas`; some merchants emit `gasLimit`.
        val gas = parseHexBigInteger(tx.optString("gas"), default = BigInteger.ZERO)
        if (gas > BigInteger.ZERO) return gas
        return parseHexBigInteger(tx.optString("gasLimit"), default = BigInteger.ZERO)
    }

    private fun parseHexBigInteger(value: String?, default: BigInteger): BigInteger {
        if (value.isNullOrBlank()) return default
        val trimmed = value.trim()
        return runCatching {
            if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
                BigInteger(Numeric.cleanHexPrefix(trimmed), 16)
            } else {
                BigInteger(trimmed)
            }
        }.getOrDefault(default)
    }

    private fun JSONObject.toMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        keys().forEach { key -> result[key] = get(key) }
        return result
    }

    private fun randomId(): Int = (100..9_999).random()
}
