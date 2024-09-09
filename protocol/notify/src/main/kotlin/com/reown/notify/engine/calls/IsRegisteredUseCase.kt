@file:JvmSynthetic

package com.reown.notify.engine.calls

import com.reown.android.internal.common.model.AccountId
import com.reown.android.keyserver.domain.IdentitiesInteractor
import com.reown.notify.data.storage.RegisteredAccountsRepository
import com.reown.notify.engine.domain.createAuthorizationReCaps

internal class IsRegisteredUseCase(
    private val registeredAccountsRepository: RegisteredAccountsRepository,
    private val identitiesInteractor: IdentitiesInteractor,
    private val identityServerUrl: String,
) : IsRegisteredUseCaseInterface {

    override suspend fun isRegistered(account: String, domain: String): Boolean {
        try {
            registeredAccountsRepository.getAccountByAccountId(account).let {
                return identitiesInteractor.getAlreadyRegisteredValidIdentity(
                    accountId = AccountId(account),
                    domain = domain,
                    resources = listOf(identityServerUrl, createAuthorizationReCaps())
                )
                    .map { true }
                    .getOrElse { false }
            }
        } catch (_: NullPointerException) {
            return false
        }
    }
}

internal interface IsRegisteredUseCaseInterface {
    suspend fun isRegistered(account: String, domain: String): Boolean
}