package com.reown.android.internal.common.signing.message

import com.reown.android.cacao.signature.SignatureType
import com.reown.android.internal.common.model.ProjectId
import com.reown.android.internal.common.signing.signature.Signature
import com.reown.android.internal.common.signing.signature.verify


class MessageSignatureVerifier(private val projectId: ProjectId) {
    fun verify(signature: String, originalMessage: String, address: String, chainId: String, type: SignatureType): Boolean =
        Signature.fromString(signature).verify(originalMessage, address, chainId, type.header, projectId)
}