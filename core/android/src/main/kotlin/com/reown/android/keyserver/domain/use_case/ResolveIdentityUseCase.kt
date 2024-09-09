package com.reown.android.keyserver.domain.use_case

import com.reown.android.keyserver.data.service.KeyServerService
import com.reown.android.keyserver.model.KeyServerResponse

class ResolveIdentityUseCase(
    private val service: KeyServerService,
) {
    suspend operator fun invoke(identityKey: String): Result<KeyServerResponse.ResolveIdentity> = runCatching {
        service.resolveIdentity(identityKey).unwrapValue()
    }
}