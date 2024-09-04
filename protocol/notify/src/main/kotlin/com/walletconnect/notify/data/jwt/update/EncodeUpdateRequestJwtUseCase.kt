@file:JvmSynthetic

package com.walletconnect.notify.data.jwt.update

import com.walletconnect.android.internal.common.jwt.did.EncodeDidJwtPayloadUseCase
import com.walletconnect.android.internal.common.model.AccountId
import com.reown.foundation.common.model.PublicKey
import com.reown.foundation.util.jwt.encodeDidPkh
import com.reown.foundation.util.jwt.encodeDidWeb
import com.reown.foundation.util.jwt.encodeEd25519DidKey

internal class EncodeUpdateRequestJwtUseCase(
    private val accountId: AccountId,
    private val app: String,
    private val authenticationKey: PublicKey,
    private val scope: String,
) : EncodeDidJwtPayloadUseCase<UpdateRequestJwtClaim> {

    override fun invoke(params: EncodeDidJwtPayloadUseCase.Params): UpdateRequestJwtClaim = with(params) {
        UpdateRequestJwtClaim(
            issuedAt = issuedAt,
            expiration = expiration,
            issuer = issuer,
            keyserverUrl = keyserverUrl,
            audience = encodeEd25519DidKey(authenticationKey.keyAsBytes),
            subject = encodeDidPkh(accountId.value),
            app = encodeDidWeb(app),
            scope = scope,
        )
    }
}