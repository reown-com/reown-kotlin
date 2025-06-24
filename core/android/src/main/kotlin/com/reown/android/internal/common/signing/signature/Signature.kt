@file:JvmSynthetic

package com.reown.android.internal.common.signing.signature

import com.reown.android.cacao.signature.SignatureType
import com.reown.android.internal.common.model.ProjectId
import com.reown.android.internal.common.signing.cacao.guaranteeNoHexPrefix
import com.reown.android.internal.common.signing.eip1271.EIP1271Verifier
import com.reown.android.internal.common.signing.eip191.EIP191Verifier
import com.reown.android.internal.common.signing.eip6492.EIP6492Verifier
import com.reown.util.bytesToHex
import com.reown.util.hexToBytes
import com.reown.utils.HexPrefix
import org.web3j.crypto.Sign

@JvmSynthetic

internal fun Sign.SignatureData.toSignature(): Signature = Signature(v, r, s)

fun Signature.toHexSignature(): String = String.HexPrefix + r.bytesToHex() + s.bytesToHex() + v.bytesToHex()

@JvmSynthetic
internal fun Signature.toSignatureData(): Sign.SignatureData = Sign.SignatureData(v, r, s)

@JvmSynthetic
internal fun Signature.verify(originalMessage: String, address: String, chainId: String, type: String, projectId: ProjectId): Boolean {
    EIP6492Verifier.init(chainId, projectId.value)
    return when (type) {
        SignatureType.EIP191.header -> EIP6492Verifier.verify6492(originalMessage, address, this.toHexSignature())
        SignatureType.EIP1271.header -> EIP6492Verifier.verify6492(originalMessage, address, this.toHexSignature())
        else -> throw RuntimeException("Invalid signature type")
    }
}

data class Signature(val v: ByteArray, val r: ByteArray, val s: ByteArray) {
    companion object {
        fun fromString(string: String): Signature = string.guaranteeNoHexPrefix().let { noPrefix ->
            check(noPrefix.chunked(64).size == 3)
            val (rRaw, sRaw, ivRaw) = noPrefix.chunked(64)
            val iv = Integer.parseUnsignedInt(ivRaw, 16).let { iv -> if (iv < 27) iv + 27 else iv }
            val v = Integer.toHexString(iv)
            Signature(v = v.hexToBytes(), r = rRaw.hexToBytes(), s = sRaw.hexToBytes())
        }
    }

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other == null || javaClass != other.javaClass -> false
        other is Signature -> !v.contentEquals(other.v) || !r.contentEquals(other.r) || s.contentEquals(other.s)
        else -> false
    }

    override fun hashCode(): Int {
        var result = v.contentHashCode()
        result = 31 * result + r.contentHashCode()
        result = 31 * result + s.contentHashCode()
        return result
    }
}



