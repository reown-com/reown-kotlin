package com.reown.appkit.domain.usecase

import com.reown.appkit.domain.SessionRepository

internal class DeleteSessionDataUseCase(
    private val repository: SessionRepository
) {
    suspend operator fun invoke() {
        repository.deleteSession()
    }
}