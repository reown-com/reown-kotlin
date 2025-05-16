package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass
import org.bouncycastle.crypto.digests.SHA512tDigest
import org.bouncycastle.util.encoders.Base32
import org.bouncycastle.util.encoders.Base64

@JsonClass(generateAdapter = true)
data class SignTxnResponse(
    val result: List<String>
)

fun calculateTxIDs(signedTxnBase64List: List<String>): List<String> {
    return signedTxnBase64List.map { signedTxnBase64 ->
        val signedTxnBytes = try {
            Base64.decode(signedTxnBase64)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid base64 string: ${e.message}")
        }

        val hasher = SHA512tDigest(256)
        hasher.update(signedTxnBytes, 0, signedTxnBytes.size)
        val txidHash = ByteArray(32) // SHA-512/256 produces 32 bytes
        hasher.doFinal(txidHash, 0)

        val checksumHasher = SHA512tDigest(256)
        checksumHasher.update(txidHash, 0, txidHash.size)
        val checksumHash = ByteArray(32)
        checksumHasher.doFinal(checksumHash, 0)
        val checksum = checksumHash.takeLast(4).toByteArray()

        val txidWithChecksum = txidHash + checksum
        Base32.toBase32String(txidWithChecksum).trimEnd('=')
    }
}