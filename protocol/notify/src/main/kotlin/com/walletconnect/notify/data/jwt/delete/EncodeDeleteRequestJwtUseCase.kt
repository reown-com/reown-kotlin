@file:JvmSynthetic

package com.walletconnect.notify.data.jwt.delete

import com.reown.android.internal.common.jwt.did.EncodeDidJwtPayloadUseCase
import com.reown.android.internal.common.model.AccountId
import com.reown.foundation.common.model.PublicKey
import com.reown.foundation.util.jwt.encodeDidPkh
import com.reown.foundation.util.jwt.encodeDidWeb
import com.reown.foundation.util.jwt.encodeEd25519DidKey

internal class EncodeDeleteRequestJwtUseCase(
    private val app: String,
    private val accountId: AccountId,
    private val authenticationKey: PublicKey,
) : EncodeDidJwtPayloadUseCase<DeleteRequestJwtClaim> {

    override fun invoke(params: EncodeDidJwtPayloadUseCase.Params): DeleteRequestJwtClaim = with(params) {
        DeleteRequestJwtClaim(
            issuedAt = issuedAt,
            expiration = expiration,
            issuer = issuer,
            keyserverUrl = keyserverUrl,
            audience = encodeEd25519DidKey(authenticationKey.keyAsBytes),
            subject = encodeDidPkh(accountId.value),
            app = encodeDidWeb(app)
        )
    }
}