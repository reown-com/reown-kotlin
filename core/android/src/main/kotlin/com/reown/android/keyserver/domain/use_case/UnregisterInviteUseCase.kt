package com.reown.android.keyserver.domain.use_case

import com.reown.android.keyserver.data.service.KeyServerService
import com.reown.android.keyserver.model.KeyServerBody

class UnregisterInviteUseCase(
    private val service: KeyServerService,
) {
    suspend operator fun invoke(idAuth: String): Result<Unit> = runCatching {
        service.unregisterInvite(KeyServerBody.UnregisterInvite(idAuth)).unwrapUnit()
    }
}