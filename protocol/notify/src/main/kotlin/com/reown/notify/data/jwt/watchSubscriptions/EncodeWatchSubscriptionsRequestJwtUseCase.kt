@file:JvmSynthetic

package com.reown.notify.data.jwt.watchSubscriptions

import com.reown.android.internal.common.jwt.did.EncodeDidJwtPayloadUseCase
import com.reown.android.internal.common.model.AccountId
import com.reown.foundation.common.model.PublicKey
import com.reown.foundation.util.jwt.encodeDidPkh
import com.reown.foundation.util.jwt.encodeDidWeb
import com.reown.foundation.util.jwt.encodeEd25519DidKey

internal class EncodeWatchSubscriptionsRequestJwtUseCase(
    private val accountId: AccountId,
    private val authenticationKey: PublicKey,
    private val appDomain: String?,
) : EncodeDidJwtPayloadUseCase<WatchSubscriptionsRequestJwtClaim> {

    override fun invoke(params: EncodeDidJwtPayloadUseCase.Params): WatchSubscriptionsRequestJwtClaim = with(params) {
        WatchSubscriptionsRequestJwtClaim(
            issuedAt = issuedAt,
            expiration = expiration,
            issuer = issuer,
            keyserverUrl = keyserverUrl,
            audience = encodeEd25519DidKey(authenticationKey.keyAsBytes),
            subject = encodeDidPkh(accountId.value),
            appDidWeb = appDomain?.let { encodeDidWeb(it) }
        )
    }
}