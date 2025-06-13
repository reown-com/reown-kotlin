package com.reown.android.internal.common.signing.cacao

import com.reown.android.cacao.signature.SignatureType
import com.reown.android.internal.common.model.ProjectId
import com.reown.android.internal.common.signing.signature.verify
import org.web3j.utils.Numeric

class CacaoVerifier(private val projectId: ProjectId) {
    fun verify(cacao: Cacao): Boolean =
        when (cacao.signature.t) {
            SignatureType.EIP191.header, SignatureType.EIP1271.header -> {
                val plainMessage = cacao.payload.toCAIP222Message()
                val hexMessage = Numeric.toHexString(cacao.payload.toCAIP222Message().toByteArray())
                val issuer = Issuer(cacao.payload.iss)
                if (cacao.signature.toSignature().verify(plainMessage, issuer.address, issuer.chainId, cacao.signature.t, projectId)) {
                    true
                } else {
                    cacao.signature.toSignature().verify(hexMessage, issuer.address, issuer.chainId, cacao.signature.t, projectId)
                }
            }

            else -> throw RuntimeException("Invalid header")
        }
}

