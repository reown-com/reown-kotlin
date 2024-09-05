package com.reown.appkit.domain.usecase

import com.reown.appkit.domain.SessionRepository
import kotlinx.coroutines.flow.map

internal class ObserveSelectedChainUseCase(
    private val repository: SessionRepository
) {
    operator fun invoke() = repository.session.map { it?.chain }
}