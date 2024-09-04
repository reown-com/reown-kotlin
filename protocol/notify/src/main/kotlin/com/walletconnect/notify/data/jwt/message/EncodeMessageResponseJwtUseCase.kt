@file:JvmSynthetic

package com.walletconnect.notify.data.jwt.message

import com.walletconnect.android.internal.common.jwt.did.EncodeDidJwtPayloadUseCase
import com.walletconnect.android.internal.common.model.AccountId
import com.reown.foundation.common.model.PublicKey
import com.reown.foundation.util.jwt.encodeDidPkh
import com.reown.foundation.util.jwt.encodeDidWeb
import com.reown.foundation.util.jwt.encodeEd25519DidKey

internal class EncodeMessageResponseJwtUseCase(
    private val app: String,
    private val accountId: AccountId,
    private val authenticationKey: PublicKey,
) : EncodeDidJwtPayloadUseCase<MessageResponseJwtClaim> {

    override fun invoke(params: EncodeDidJwtPayloadUseCase.Params): MessageResponseJwtClaim = with(params) {
        MessageResponseJwtClaim(
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