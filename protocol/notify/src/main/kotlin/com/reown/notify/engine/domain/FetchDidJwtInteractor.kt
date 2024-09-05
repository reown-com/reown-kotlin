@file:JvmSynthetic

package com.reown.notify.engine.domain

import com.reown.android.internal.common.jwt.did.EncodeDidJwtPayloadUseCase
import com.reown.android.internal.common.jwt.did.encodeDidJwt
import com.reown.android.internal.common.model.AccountId
import com.reown.android.internal.common.model.DidJwt
import com.reown.android.keyserver.domain.IdentitiesInteractor
import com.reown.foundation.common.model.PrivateKey
import com.reown.foundation.common.model.PublicKey
import com.reown.notify.data.jwt.delete.EncodeDeleteRequestJwtUseCase
import com.reown.notify.data.jwt.getNotifications.EncodeGetNotificationsRequestJwtUseCase
import com.reown.notify.data.jwt.message.EncodeMessageResponseJwtUseCase
import com.reown.notify.data.jwt.subscription.EncodeSubscriptionRequestJwtUseCase
import com.reown.notify.data.jwt.subscriptionsChanged.EncodeSubscriptionsChangedResponseJwtUseCase
import com.reown.notify.data.jwt.update.EncodeUpdateRequestJwtUseCase
import com.reown.notify.data.jwt.watchSubscriptions.EncodeWatchSubscriptionsRequestJwtUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal class FetchDidJwtInteractor(
    private val keyserverUrl: String,
    private val identitiesInteractor: IdentitiesInteractor,
) {

    suspend fun subscriptionRequest(
        account: AccountId,
        authenticationKey: PublicKey,
        app: String,
        scopes: List<String>,
    ): Result<DidJwt> = registerIdentityAndReturnIdentityKeyPair(account) { (identityPublicKey, identityPrivateKey) ->
        val concatenatedScopes = scopes.joinToString(SCOPES_DELIMITER)

        return@registerIdentityAndReturnIdentityKeyPair encodeDidJwt(
            identityPrivateKey,
            EncodeSubscriptionRequestJwtUseCase(app, account, authenticationKey, concatenatedScopes),
            EncodeDidJwtPayloadUseCase.Params(identityPublicKey, keyserverUrl, expirySourceDuration = FIVE_MINUTE_EXPIRY, expiryTimeUnit = FIVE_MINUTE_EXPIRY_TIME_UNIT)
        )
    }

    suspend fun deleteRequest(
        account: AccountId,
        app: String,
        authenticationKey: PublicKey,
    ): Result<DidJwt> = registerIdentityAndReturnIdentityKeyPair(account) { (identityPublicKey, identityPrivateKey) ->

        return@registerIdentityAndReturnIdentityKeyPair encodeDidJwt(
            identityPrivateKey,
            EncodeDeleteRequestJwtUseCase(app, account, authenticationKey),
            EncodeDidJwtPayloadUseCase.Params(identityPublicKey, keyserverUrl)
        )
    }

    suspend fun messageResponse(
        account: AccountId,
        app: String,
        authenticationKey: PublicKey,
    ): Result<DidJwt> = registerIdentityAndReturnIdentityKeyPair(account) { (identityPublicKey, identityPrivateKey) ->

        return@registerIdentityAndReturnIdentityKeyPair encodeDidJwt(
            identityPrivateKey,
            EncodeMessageResponseJwtUseCase(app, account, authenticationKey),
            EncodeDidJwtPayloadUseCase.Params(identityPublicKey, keyserverUrl)
        )
    }

    suspend fun updateRequest(
        account: AccountId,
        metadataUrl: String,
        authenticationKey: PublicKey,
        scopes: List<String>,
    ): Result<DidJwt> = registerIdentityAndReturnIdentityKeyPair(account) { (identityPublicKey, identityPrivateKey) ->
        val concatenatedScopes = scopes.joinToString(SCOPES_DELIMITER)

        return@registerIdentityAndReturnIdentityKeyPair encodeDidJwt(
            identityPrivateKey,
            EncodeUpdateRequestJwtUseCase(account, metadataUrl, authenticationKey, concatenatedScopes),
            EncodeDidJwtPayloadUseCase.Params(identityPublicKey, keyserverUrl, expirySourceDuration = FIVE_MINUTE_EXPIRY, expiryTimeUnit = FIVE_MINUTE_EXPIRY_TIME_UNIT)
        )
    }

    suspend fun watchSubscriptionsRequest(
        account: AccountId,
        authenticationKey: PublicKey,
        appDomain: String?,
    ): Result<DidJwt> = registerIdentityAndReturnIdentityKeyPair(account) { (identityPublicKey, identityPrivateKey) ->

        return@registerIdentityAndReturnIdentityKeyPair encodeDidJwt(
            identityPrivateKey,
            EncodeWatchSubscriptionsRequestJwtUseCase(account, authenticationKey, appDomain),
            EncodeDidJwtPayloadUseCase.Params(identityPublicKey, keyserverUrl, expirySourceDuration = FIVE_MINUTE_EXPIRY, expiryTimeUnit = FIVE_MINUTE_EXPIRY_TIME_UNIT)
        )
    }

    suspend fun subscriptionsChangedResponse(
        account: AccountId,
        authenticationKey: PublicKey,
    ): Result<DidJwt> = registerIdentityAndReturnIdentityKeyPair(account) { (identityPublicKey, identityPrivateKey) ->

        return@registerIdentityAndReturnIdentityKeyPair encodeDidJwt(
            identityPrivateKey,
            EncodeSubscriptionsChangedResponseJwtUseCase(account, authenticationKey),
            EncodeDidJwtPayloadUseCase.Params(identityPublicKey, keyserverUrl, expirySourceDuration = FIVE_MINUTE_EXPIRY, expiryTimeUnit = FIVE_MINUTE_EXPIRY_TIME_UNIT)
        )
    }

    suspend fun getNotificationsRequest(
        account: AccountId,
        authenticationKey: PublicKey,
        app: String,
        limit: Int,
        after: String?,
    ): Result<DidJwt> = registerIdentityAndReturnIdentityKeyPair(account) { (identityPublicKey, identityPrivateKey) ->

        return@registerIdentityAndReturnIdentityKeyPair encodeDidJwt(
            identityPrivateKey,
            EncodeGetNotificationsRequestJwtUseCase(app, account, authenticationKey, limit, after),
            EncodeDidJwtPayloadUseCase.Params(identityPublicKey, keyserverUrl, expirySourceDuration = FIVE_MINUTE_EXPIRY, expiryTimeUnit = FIVE_MINUTE_EXPIRY_TIME_UNIT)
        )
    }

    private suspend fun registerIdentityAndReturnIdentityKeyPair(
        account: AccountId,
        returnedKeys: suspend (Pair<PublicKey, PrivateKey>) -> Result<DidJwt>,
    ) = supervisorScope {
        withContext(Dispatchers.IO) {
            returnedKeys(identitiesInteractor.getIdentityKeyPair(account))
        }
    }

    companion object {
        const val SCOPES_DELIMITER = " "
        const val FIVE_MINUTE_EXPIRY = 5L
        val FIVE_MINUTE_EXPIRY_TIME_UNIT = TimeUnit.MINUTES
    }
}