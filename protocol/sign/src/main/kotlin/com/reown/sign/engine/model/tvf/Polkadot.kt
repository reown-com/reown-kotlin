package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass
import org.bouncycastle.crypto.digests.Blake2bDigest
import java.math.BigInteger
import com.reown.sign.engine.model.tvf.TVF.Companion.toBase58
import com.reown.util.bytesToHex
import com.reown.util.hexToBytes
import io.ipfs.multibase.Base58
import java.nio.ByteBuffer

object PolkadotSignTransaction {
    @JsonClass(generateAdapter = true)
    data class SignatureResponse(
        val id: Long,
        val signature: String
    )

    @JsonClass(generateAdapter = true)
    data class TransactionPayload(
        val method: String,
        val specVersion: String?,
        val transactionVersion: String?,
        val genesisHash: String?,
        val blockHash: String?,
        val era: String? = "",
        val nonce: String?,
        val tip: String?,
        val mode: String?,
        val metadataHash: String?,
        val address: String?,
        val version: Int?
    )

    @JsonClass(generateAdapter = true)
    data class RequestParams(
        val address: String?,
        val transactionPayload: TransactionPayload
    )

    fun calculatePolkadotHash(signatureResponse: SignatureResponse, requestParams: RequestParams): String? {
        return try {
            val publicKey = ss58AddressToPublicKey(requestParams.address ?: requestParams.transactionPayload.address ?: "")
            val signedExtrinsic = addSignatureToExtrinsic(publicKey, signatureResponse.signature, requestParams.transactionPayload)
            deriveExtrinsicHash(signedExtrinsic)
        } catch (e: Exception) {
            null
        }
    }

    private fun deriveExtrinsicHash(signed: ByteArray): String {
        try {
            // Use Blake2b with 256-bit output (32 bytes)
            val digest = Blake2bDigest(null, 32, null, null)
            digest.reset()
            digest.update(signed, 0, signed.size)
            val output = ByteArray(digest.digestSize)
            digest.doFinal(output, 0)
            return output.bytesToHex()
        } catch (e: Exception) {
            return "error"
        }
    }

    private fun ss58AddressToPublicKey(ss58Address: String): ByteArray {
        val decoded = Base58.decode(ss58Address)

        if (decoded.size < 33) {
            throw IllegalArgumentException("Too short to contain a public key")
        }

        return decoded.sliceArray(1..32)
    }

    // SCALE-encoding of the unsigned payload along with the signature
    private fun addSignatureToExtrinsic(
        publicKey: ByteArray,
        hexSignature: String,
        transactionPayload: TransactionPayload
    ): ByteArray {
        val method = transactionPayload.method.hexToBytes()
        val signature = hexSignature.hexToBytes()

        val signedFlag = 0x80 // For signed extrinsics
        val versionValue = transactionPayload.version.toString() ?: "4"
        val version = versionValue.toInt()
        // Extrinsic version = signed flag + version
        val extrinsicVersion = signedFlag or version // 0x80 + 0x04 = 0x84

        // Detect signature type by evaluating the address if possible
        val ss58Address = transactionPayload.address
        val signatureType = guessSignatureTypeFromAddress(ss58Address ?: "")

        // Era - handle era properly for Polkadot 
        val eraValue = normalizeHex(transactionPayload.era ?: "")
        val eraBytes = if (eraValue.isEmpty() || eraValue == "00") {
            byteArrayOf(0x00) // Immortal
        } else {
            // For mortal era, just use the hex bytes as-is
            eraValue.hexToBytes()
        }

        // Nonce - use raw hex value, not compact encoded
        val nonceValue = parseHex(transactionPayload.nonce)
        val nonceBytes = byteArrayOf(nonceValue.toByte())

        // Tip - use compact encoding  
        val tipValue = BigInteger(normalizeHex(transactionPayload.tip ?: "0"), 16)
        val tipBytes = compactEncodeBigInt(tipValue)

        // Handle method - only insert 00 byte if not already present
        val methodBytes = method
        val finalMethod = if (methodBytes.size >= 3 &&
            methodBytes[0] == 0x05.toByte() &&
            methodBytes[1] == 0x03.toByte() &&
            methodBytes[2] != 0x00.toByte()
        ) {
            // Method needs 00 byte inserted after first two bytes (05 03 -> 05 03 00)
            ByteBuffer.allocate(methodBytes.size + 1).apply {
                put(methodBytes[0]) // 05
                put(methodBytes[1]) // 03  
                put(0x00.toByte())  // 00
                put(methodBytes.sliceArray(2 until methodBytes.size)) // rest
            }.array()
        } else {
            // Method already has correct format or different structure
            methodBytes
        }

        val extrinsicBody = ByteBuffer.allocate(1024) // Adjust size as needed
        extrinsicBody.put(0x00.toByte()) // MultiAddress::Id
        extrinsicBody.put(publicKey)
        extrinsicBody.put(signatureType.toByte())
        extrinsicBody.put(signature)
        extrinsicBody.put(eraBytes)
        extrinsicBody.put(nonceBytes)
        extrinsicBody.put(tipBytes)
        extrinsicBody.put(0x00.toByte()) // Additional 00 byte before method
        extrinsicBody.put(finalMethod)

        val bodyBytes = extrinsicBody.array().sliceArray(0 until extrinsicBody.position())

        val lengthPrefix = compactEncodeInt(bodyBytes.size + 1) // +1 for version byte

        val result = ByteBuffer.allocate(lengthPrefix.size + 1 + bodyBytes.size)
        result.put(lengthPrefix)
        result.put(extrinsicVersion.toByte())
        result.put(bodyBytes)

        return result.array().sliceArray(0 until result.position())
    }

    private fun guessSignatureTypeFromAddress(address: String): Int {
        return try {
            val decoded = toBase58(address.toByteArray())
            if (decoded.isEmpty()) return 0x01 // default to sr25519

            val prefix = decoded[0].toInt() and 0xFF

            // https://github.com/paritytech/ss58-registry/blob/main/ss58-registry.json
            when (prefix) {
                42 -> 0x00 // Ed25519
                60 -> 0x02 // Secp256k1
                else -> 0x01 // Sr25519 for most chains, Polkadot, Kusama, etc
            }
        } catch (e: Exception) {
            if (address.startsWith("0x")) {
                0x02 // ecdsa
            } else {
                0x01 // fallback
            }
        }
    }

    private fun normalizeHex(input: String): String {
        return if (input.startsWith("0x")) {
            input.removePrefix("0x")
        } else {
            input
        }

    }

    private fun parseHex(input: Any?): Int {
        val raw = normalizeHex(input.toString())
        return raw.toInt(16)
    }

    private fun compactEncodeInt(value: Any): ByteArray {
        val bigValue = when (value) {
            is BigInteger -> value
            else -> BigInteger.valueOf(value.toString().toLong())
        }

        return when {
            bigValue < BigInteger.valueOf(1L shl 6) -> {
                byteArrayOf((bigValue.toInt() shl 2).toByte())
            }

            bigValue < BigInteger.valueOf(1L shl 14) -> {
                byteArrayOf(
                    (((bigValue.toInt() shl 2) or 0x01) and 0xff).toByte(),
                    ((bigValue.toInt() shr 6) and 0xff).toByte()
                )
            }

            bigValue < BigInteger.valueOf(1L shl 30) -> {
                val value = bigValue.toInt() shl 2 or 0x02
                byteArrayOf(
                    (value and 0xff).toByte(),
                    ((value shr 8) and 0xff).toByte(),
                    ((value shr 16) and 0xff).toByte(),
                    ((value shr 24) and 0xff).toByte()
                )
            }

            else -> {
                // big-integer mode
                val bytes = bigIntToLEBytes(bigValue)
                val len = bytes.size
                if (len > 67) {
                    throw IllegalArgumentException("Compact encoding supports max 2^536-1")
                }

                byteArrayOf(
                    (((len - 4) shl 2) or 0x03).toByte(),
                    *bytes
                )
            }
        }
    }

    // SCALE-encodes a BigInteger using compact encoding
    private fun compactEncodeBigInt(value: BigInteger): ByteArray {
        return when {
            value < BigInteger.valueOf(1L shl 6) -> {
                // single-byte mode
                byteArrayOf((value.toInt() shl 2).toByte())
            }

            value < BigInteger.valueOf(1L shl 14) -> {
                // two-byte mode
                val intValue = value.toInt() shl 2 or 0x01
                byteArrayOf(
                    (intValue and 0xFF).toByte(),
                    ((intValue shr 8) and 0xFF).toByte()
                )
            }

            value < BigInteger.valueOf(1L shl 30) -> {
                // four-byte mode
                val intValue = value.toInt() shl 2 or 0x02
                byteArrayOf(
                    (intValue and 0xFF).toByte(),
                    ((intValue shr 8) and 0xFF).toByte(),
                    ((intValue shr 16) and 0xFF).toByte(),
                    ((intValue shr 24) and 0xFF).toByte()
                )
            }

            else -> {
                // big-integer mode
                val bytes = bigIntToLEBytes(value)
                val len = bytes.size
                if (len > 67) {
                    throw IllegalArgumentException("Compact encoding supports max 2^536-1")
                }

                byteArrayOf(
                    (((len - 4) shl 2) or 0x03).toByte(),
                    *bytes
                )
            }
        }
    }

    private fun bigIntToLEBytes(value: BigInteger): ByteArray {
        val bytes = mutableListOf<Byte>()
        var current = value
        while (current > BigInteger.ZERO) {
            bytes.add((current and BigInteger.valueOf(0xff)).toByte())
            current = current shr 8
        }
        return bytes.toByteArray()
    }
}

