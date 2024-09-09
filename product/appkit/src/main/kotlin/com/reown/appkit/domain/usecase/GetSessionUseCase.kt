package com.reown.appkit.domain.usecase

import com.reown.appkit.domain.SessionRepository
import kotlinx.coroutines.runBlocking

internal class GetSessionUseCase(
    private val repository: SessionRepository
) {
    operator fun invoke() = runBlocking { repository.getSession() }
}
