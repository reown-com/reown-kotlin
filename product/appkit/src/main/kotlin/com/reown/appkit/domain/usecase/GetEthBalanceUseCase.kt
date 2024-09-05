package com.reown.appkit.domain.usecase

import com.reown.appkit.client.Modal
import com.reown.appkit.data.BalanceRpcRepository

internal class GetEthBalanceUseCase(
    private val balanceRpcRepository: BalanceRpcRepository,
) {
    suspend operator fun invoke(
        token: Modal.Model.Token,
        rpcUrl: String,
        address: String
    ) = balanceRpcRepository.getBalance(
        token = token,
        rpcUrl = rpcUrl,
        address = address
    )
}
