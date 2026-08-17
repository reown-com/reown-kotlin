package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.util.encoders.Base64

@JsonClass(generateAdapter = true)
data class StellarSignXDRResult(
    val signedXDR: String,
    val signerAddress: String?
)

@JsonClass(generateAdapter = true)
data class StellarSignXDRParams(
    val chain: String?
)

@JsonClass(generateAdapter = true)
data class StellarSignAndSubmitXDRResult(
    val tx_hash: String?,
    val signedXDR: String?
)

object StellarSignXDR {
    private const val PUBNET_PASSPHRASE = "Public Global Stellar Network ; September 2015"
    private const val TESTNET_PASSPHRASE = "Test SDF Network ; September 2015"

    // XDR EnvelopeType discriminants
    private const val ENVELOPE_TYPE_TX_V0 = 0
    private const val ENVELOPE_TYPE_TX = 2
    private const val ENVELOPE_TYPE_TX_FEE_BUMP = 5

    // DecoratedSignature with an ed25519 signature: hint (4) + length (4, =64) + signature (64)
    private const val DECORATED_SIGNATURE_LENGTH = 72
    private const val ED25519_SIGNATURE_LENGTH = 64
    private const val MAX_ENVELOPE_SIGNATURES = 20

    /**
     * Computes the Stellar transaction hash from a base64-encoded, signed TransactionEnvelope XDR
     * as sha256(network_id || envelope_type || transaction_body). Signatures are computed over the
     * hash, so the trailing signature array is stripped rather than hashed. For fee-bump envelopes
     * this yields the canonical fee-bump hash.
     *
     * @param signedXDR base64-encoded TransactionEnvelope XDR (V0, V1 or fee-bump)
     * @param chain CAIP-2 chain id (`stellar:pubnet` / `stellar:testnet`), defaults to pubnet
     * @return lowercase hex transaction hash (64 chars)
     */
    fun computeTransactionHash(signedXDR: String, chain: String?): String {
        val bytes = Base64.decode(signedXDR)
        require(bytes.size >= 8) { "Stellar envelope too short" }

        val discriminant = readUInt32BE(bytes, 0)
        val envelopeType: Int
        val bodyStart: Int
        when (discriminant) {
            // V0 transactions are hashed as ENVELOPE_TYPE_TX over the envelope bytes INCLUDING
            // the leading 4 zero bytes - they double as the legacy AccountID key-type tag
            ENVELOPE_TYPE_TX_V0 -> {
                envelopeType = ENVELOPE_TYPE_TX
                bodyStart = 0
            }

            ENVELOPE_TYPE_TX -> {
                envelopeType = ENVELOPE_TYPE_TX
                bodyStart = 4
            }

            ENVELOPE_TYPE_TX_FEE_BUMP -> {
                envelopeType = ENVELOPE_TYPE_TX_FEE_BUMP
                bodyStart = 4
            }

            else -> throw IllegalArgumentException("Unsupported Stellar envelope type: $discriminant")
        }

        val signatureArrayOffset = findSignatureArrayOffset(bytes)
        val networkId = sha256(passphraseFor(chain).toByteArray(Charsets.UTF_8))

        val payload = networkId +
                byteArrayOf(0, 0, 0, envelopeType.toByte()) +
                bytes.copyOfRange(bodyStart, signatureArrayOffset)

        return sha256(payload).joinToString("") { "%02x".format(it) }
    }

    private fun passphraseFor(chain: String?): String =
        when (chain?.substringAfterLast(':') ?: "pubnet") {
            "pubnet" -> PUBNET_PASSPHRASE
            "testnet" -> TESTNET_PASSPHRASE
            else -> throw IllegalArgumentException("Unknown Stellar network: $chain")
        }

    /**
     * Locates the start of the trailing `DecoratedSignature signatures<20>` XDR array without
     * parsing the transaction body. Assumes ed25519 signatures (fixed 72-byte entries), which is
     * what the WalletConnect Stellar RPC spec mandates wallets emit.
     */
    private fun findSignatureArrayOffset(bytes: ByteArray): Int {
        for (signatureCount in 0..MAX_ENVELOPE_SIGNATURES) {
            val offset = bytes.size - 4 - DECORATED_SIGNATURE_LENGTH * signatureCount
            if (offset < 4) break
            if (readUInt32BE(bytes, offset) != signatureCount) continue

            val isValid = (0 until signatureCount).all { i ->
                val entryOffset = offset + 4 + DECORATED_SIGNATURE_LENGTH * i
                // each entry's signature length field must be exactly 64 (ed25519)
                readUInt32BE(bytes, entryOffset + 4) == ED25519_SIGNATURE_LENGTH
            }
            if (isValid) return offset
        }
        throw IllegalArgumentException("Could not locate Stellar envelope signature array")
    }

    private fun readUInt32BE(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)

    private fun sha256(data: ByteArray): ByteArray {
        val digest = SHA256Digest()
        digest.update(data, 0, data.size)
        val hash = ByteArray(digest.digestSize)
        digest.doFinal(hash, 0)
        return hash
    }
}
