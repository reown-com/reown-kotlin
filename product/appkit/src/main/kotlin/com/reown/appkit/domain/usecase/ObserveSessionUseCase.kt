package com.reown.appkit.domain.usecase

import com.reown.appkit.domain.SessionRepository

internal class ObserveSessionUseCase(
    private val repository: SessionRepository
) {
    operator fun invoke() = repository.session
}