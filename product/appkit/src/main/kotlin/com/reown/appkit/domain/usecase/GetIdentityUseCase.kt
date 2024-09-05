package com.reown.appkit.domain.usecase

import com.reown.appkit.data.BlockchainRepository
import com.reown.appkit.domain.model.Identity

internal class GetIdentityUseCase(
    private val blockchainRepository: BlockchainRepository
) {
    suspend operator fun invoke(address: String, chainId: String) = try {
        blockchainRepository.getIdentity(address = address, chainId = chainId)
    } catch (e: Throwable) {
        null
    }
}
