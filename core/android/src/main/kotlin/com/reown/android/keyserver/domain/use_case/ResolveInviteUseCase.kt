package com.reown.android.keyserver.domain.use_case

import com.reown.android.internal.common.model.AccountId
import com.reown.android.keyserver.data.service.KeyServerService
import com.reown.android.keyserver.model.KeyServerResponse

class ResolveInviteUseCase(
    private val service: KeyServerService
) {
    suspend operator fun invoke(accountId: AccountId): Result<KeyServerResponse.ResolveInvite> = runCatching {
        service.resolveInvite(accountId.value).unwrapValue()
    }
}