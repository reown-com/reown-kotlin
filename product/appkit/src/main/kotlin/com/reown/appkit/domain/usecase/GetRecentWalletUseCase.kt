package com.reown.appkit.domain.usecase

import com.reown.appkit.domain.RecentWalletsRepository

internal class GetRecentWalletUseCase(private val repository: RecentWalletsRepository) {
    operator fun invoke() = repository.getRecentWalletId()
}