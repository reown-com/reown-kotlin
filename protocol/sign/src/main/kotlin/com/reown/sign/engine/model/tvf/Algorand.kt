package com.reown.sign.engine.model.tvf

import org.bouncycastle.crypto.digests.SHA512tDigest
import org.bouncycastle.util.encoders.Base64
import org.msgpack.core.MessagePack
import org.msgpack.value.ValueFactory
import java.io.ByteArrayOutputStream

fun calculateTxIDs(signedTxnBase64List: List<String>): List<String> =
    try {
        signedTxnBase64List.map { signedTxnBase64 ->
            val signedTxnBytes = Base64.decode(signedTxnBase64)
            val canonicalTxnBytes = extractCanonicalTransaction(signedTxnBytes)
            val prefix = "TX".toByteArray(Charsets.US_ASCII)
            val prefixedBytes = prefix + canonicalTxnBytes
            val digest = SHA512tDigest(256)
            digest.update(prefixedBytes, 0, prefixedBytes.size)
            val hash = ByteArray(32)
            digest.doFinal(hash, 0)
            bytesToBase32(hash)
        }
    } catch (e: Exception) {
        listOf("")
    }


private fun bytesToBase32(bytes: ByteArray): String {
    val base32Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    val bitString = bytes.joinToString("") { byte ->
        String.format("%8s", Integer.toBinaryString(byte.toInt() and 0xFF)).replace(' ', '0')
    }

    val result = StringBuilder()
    var i = 0

    while (i < bitString.length) {
        val chunk = bitString.substring(i, minOf(i + 5, bitString.length))
        if (chunk.length == 5) {
            val index = chunk.toInt(2)
            result.append(base32Alphabet[index])
        } else if (chunk.isNotEmpty()) {
            val paddedChunk = chunk.padEnd(5, '0')
            val index = paddedChunk.toInt(2)
            result.append(base32Alphabet[index])
        }
        i += 5
    }

    return result.toString()
}

private fun extractCanonicalTransaction(signedTxnBytes: ByteArray): ByteArray {
    val unpacker = MessagePack.newDefaultUnpacker(signedTxnBytes)

    try {
        val signedTxnMap = unpacker.unpackValue().asMapValue()
        val txnKey = ValueFactory.newString("txn")
        val txnValue = signedTxnMap.map()[txnKey]
            ?: throw IllegalArgumentException("No 'txn' field found in signed transaction")
        val outputStream = ByteArrayOutputStream()
        val packer = MessagePack.newDefaultPacker(outputStream)
        packer.packValue(txnValue)
        packer.close()
        return outputStream.toByteArray()
    } catch (e: Exception) {
        throw IllegalArgumentException("Failed to parse signed transaction MessagePack", e)
    } finally {
        unpacker.close()
    }
}