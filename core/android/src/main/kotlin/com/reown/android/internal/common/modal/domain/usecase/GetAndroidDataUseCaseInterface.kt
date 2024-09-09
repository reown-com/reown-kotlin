package com.reown.android.internal.common.modal.domain.usecase

import com.reown.android.internal.common.modal.AppKitApiRepository

interface GetInstalledWalletsIdsUseCaseInterface {
    suspend operator fun invoke(
        sdkType: String
    ): List<String>
}

internal class GetInstalledWalletsIdsUseCase(
    private val appKitApiRepository: AppKitApiRepository
) : GetInstalledWalletsIdsUseCaseInterface {
    override suspend fun invoke(sdkType: String): List<String> = appKitApiRepository.getAndroidWalletsData(sdkType).map { it.map { walletAppData -> walletAppData.id } }.getOrThrow()
}
