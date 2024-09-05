package com.reown.appkit.domain.usecase

import com.reown.appkit.domain.RecentWalletsRepository

internal class SaveRecentWalletUseCase(
    private val repository: RecentWalletsRepository
) {
    operator fun invoke(id: String) = repository.saveRecentWalletId(id)
}