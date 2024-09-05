package com.reown.appkit.domain.usecase

import com.reown.appkit.domain.SessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

internal class GetSelectedChainUseCase(
    private val repository: SessionRepository
) {
    operator fun invoke() = runBlocking { repository.session.first()?.chain }
}