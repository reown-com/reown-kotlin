package com.reown.appkit.domain.usecase

import com.reown.appkit.domain.SessionRepository

internal class SaveChainSelectionUseCase(
    private val repository: SessionRepository
) {
    suspend operator fun invoke(chain: String) {
        repository.updateChainSelection(chain)
    }
}