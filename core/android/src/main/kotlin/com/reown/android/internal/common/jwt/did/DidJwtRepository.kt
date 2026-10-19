@file:JvmSynthetic
@file:JvmName("DidJwtRepository")

package com.reown.android.internal.common.jwt.did

import com.reown.android.internal.common.model.DidJwt
import com.reown.foundation.common.model.PrivateKey
import com.reown.foundation.util.jwt.*

fun <R : JwtClaims> encodeDidJwt(
    identityPrivateKey: PrivateKey,
    encodeDidJwtPayloadUseCase: EncodeDidJwtPayloadUseCase<R>,
    useCaseParams: EncodeDidJwtPayloadUseCase.Params,
): Result<DidJwt> = runCatching {
    val claims = encodeDidJwtPayloadUseCase(params = useCaseParams)
    val data = encodeData(JwtHeader.EdDSA.encoded, claims).toByteArray()
    val signature = signJwt(identityPrivateKey, data).getOrThrow()
    DidJwt(encodeJWT(JwtHeader.EdDSA.encoded, claims, signature))
}
