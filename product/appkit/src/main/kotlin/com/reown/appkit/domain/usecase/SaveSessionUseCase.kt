package com.reown.appkit.domain.usecase

import com.reown.appkit.domain.SessionRepository
import com.reown.appkit.domain.model.Session

internal class SaveSessionUseCase(
    private val repository: SessionRepository
) {
    suspend operator fun invoke(session: Session) = repository.saveSession(session)
}