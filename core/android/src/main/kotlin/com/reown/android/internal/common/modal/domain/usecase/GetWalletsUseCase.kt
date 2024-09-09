package com.reown.android.internal.common.modal.domain.usecase

import com.reown.android.internal.common.modal.AppKitApiRepository
import com.reown.android.internal.common.modal.data.model.WalletListing

interface GetWalletsUseCaseInterface {
    suspend operator fun invoke(
        sdkType: String,
        page: Int,
        search: String? = null,
        excludeIds: List<String>? = null,
        includes: List<String>? = null
    ): WalletListing
}

internal class GetWalletsUseCase(
    private val appKitApiRepository: AppKitApiRepository
) : GetWalletsUseCaseInterface {
    override suspend fun invoke(
        sdkType: String,
        page: Int,
        search: String?,
        excludeIds: List<String>?,
        includes: List<String>?
    ): WalletListing = appKitApiRepository.getWallets(sdkType, page, search, excludeIds, includes).getOrThrow()
}
