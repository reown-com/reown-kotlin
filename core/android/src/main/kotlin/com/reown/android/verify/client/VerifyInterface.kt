package com.reown.android.verify.client

import com.reown.android.verify.domain.VerifyResult

interface VerifyInterface {
    fun initialize()
    fun register(attestationId: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit)
    fun resolve(attestationId: String, metadataUrl: String, onSuccess: (VerifyResult) -> Unit, onError: (Throwable) -> Unit)
    fun resolveV2(attestationId: String, attestationJWT: String, metadataUrl: String, onSuccess: (VerifyResult) -> Unit, onError: (Throwable) -> Unit)
}