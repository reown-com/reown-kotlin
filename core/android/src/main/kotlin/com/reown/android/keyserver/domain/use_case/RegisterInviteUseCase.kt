package com.reown.android.keyserver.domain.use_case

import com.reown.android.keyserver.data.service.KeyServerService
import com.reown.android.keyserver.model.KeyServerBody

class RegisterInviteUseCase(
    private val service: KeyServerService,
) {
    suspend operator fun invoke(idAuth: String): Result<Unit> = runCatching {
        service.registerInvite(KeyServerBody.RegisterInvite(idAuth)).unwrapUnit()
    }
}