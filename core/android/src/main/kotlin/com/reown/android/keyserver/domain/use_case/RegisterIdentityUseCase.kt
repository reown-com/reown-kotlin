package com.reown.android.keyserver.domain.use_case

import com.reown.android.internal.common.signing.cacao.Cacao
import com.reown.android.keyserver.data.service.KeyServerService
import com.reown.android.keyserver.model.KeyServerBody

class RegisterIdentityUseCase(
    private val service: KeyServerService,
) {
    suspend operator fun invoke(cacao: Cacao): Result<Unit> = runCatching {
        service.registerIdentity(KeyServerBody.RegisterIdentity(cacao)).unwrapUnit()
    }
}