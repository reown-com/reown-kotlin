package com.reown.appkit.data

import com.reown.android.internal.common.model.ProjectId
import com.reown.appkit.data.model.IdentityDTO
import com.reown.appkit.data.network.BlockchainService
import com.reown.appkit.domain.model.Identity

internal class BlockchainRepository(
    private val blockchainService: BlockchainService,
    private val projectId: ProjectId
) {

    suspend fun getIdentity(address: String, chainId: String) = with(
        blockchainService.getIdentity(address = address, chainId = chainId, projectId = projectId.value)
    ) {
        if (isSuccessful && body() != null) {
            body()!!.toIdentity()
        } else {
            throw Throwable(errorBody()?.string())
        }
    }
}

private fun IdentityDTO.toIdentity() = Identity(name, avatar)
