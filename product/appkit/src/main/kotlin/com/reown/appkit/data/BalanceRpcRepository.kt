package com.reown.appkit.data

import com.reown.foundation.util.Logger
import com.reown.appkit.client.Modal
import com.reown.appkit.data.json_rpc.balance.BalanceRequest
import com.reown.appkit.data.json_rpc.balance.BalanceRpcResponse
import com.reown.appkit.data.network.BalanceService
import com.reown.appkit.domain.model.Balance

internal class BalanceRpcRepository(
    private val balanceService: BalanceService,
    private val logger: Logger,
) {

    suspend fun getBalance(
        token: Modal.Model.Token, rpcUrl: String, address: String
    ) = runCatching {
        balanceService.getBalance(
            url = rpcUrl, body = BalanceRequest(address = address)
        )
    }.mapCatching { response ->
        response.body()!!.mapResponse(token)
    }.onFailure {
        logger.error(it)
    }.getOrNull()
}

private fun BalanceRpcResponse.mapResponse(token: Modal.Model.Token) = when {
    result != null -> Balance(token, result)
    error != null -> throw Throwable(error.message)
    else -> throw Throwable("Invalid balance response")
}
