@file:JvmSynthetic

package com.walletconnect.sample.pos.nfc


import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import timber.log.Timber
import java.io.ByteArrayOutputStream

/**
 * Host-based Card Emulation service that emulates an NFC Forum Type 4 Tag
 * containing an NDEF URI record with the payment gateway URL.
 *
 * When a payment QR code is generated, the same URI is set on this service
 * so that NFC-enabled phones can tap to receive the payment link instead
 * of scanning the QR code.
 *
 * Implements the NFC Forum Type 4 Tag Operation specification:
 * SELECT NDEF App → SELECT CC → READ CC → SELECT NDEF → READ NDEF
 */
internal class NdefHostApduService : HostApduService() {

    companion object {
        // NDEF Tag Application AID (D2760000850101)
        private val NDEF_AID = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x85.toByte(), 0x01.toByte(), 0x01.toByte()
        )

        // File IDs
        private val CC_FILE_ID = byteArrayOf(0xE1.toByte(), 0x03.toByte())
        private val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04.toByte())

        // Status words
        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val SW_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        private val SW_WRONG_PARAMS = byteArrayOf(0x6B.toByte(), 0x00.toByte())
        private val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D.toByte(), 0x00.toByte())

        // APDU instruction bytes
        private const val INS_SELECT = 0xA4.toByte()
        private const val INS_READ_BINARY = 0xB0.toByte()

        // Max NDEF message size (2 KB should be more than enough for a URI)
        private const val MAX_NDEF_SIZE = 2048

        /**
         * The current NDEF message bytes to serve. Set by [NfcManager].
         * When null, the service responds with an empty NDEF file.
         */
        @Volatile
        var currentNdefMessage: ByteArray? = null
    }

    private enum class SelectedFile { NONE, CC, NDEF }

    private var selectedFile: SelectedFile = SelectedFile.NONE

    /**
     * Builds the Capability Container (CC) file bytes.
     * The CC describes the NDEF file and its access conditions.
     */
    private fun buildCapabilityContainer(ndefFileSize: Int): ByteArray {
        val ccLen = 15 // CC is always 15 bytes for a single NDEF file TLV
        return byteArrayOf(
            // CC length (2 bytes)
            0x00.toByte(), ccLen.toByte(),
            // Mapping version 2.0
            0x20.toByte(),
            // Max R-APDU data size (2 bytes) — 256 bytes
            0x00.toByte(), 0xFF.toByte(),
            // Max C-APDU data size (2 bytes) — 256 bytes
            0x00.toByte(), 0xFF.toByte(),
            // NDEF File Control TLV
            0x04.toByte(), // T: NDEF message TLV
            0x06.toByte(), // L: 6 bytes
            // NDEF file ID
            0xE1.toByte(), 0x04.toByte(),
            // Max NDEF file size (2 bytes)
            ((ndefFileSize shr 8) and 0xFF).toByte(),
            (ndefFileSize and 0xFF).toByte(),
            // Read access: no security
            0x00.toByte(),
            // Write access: no write (read-only)
            0xFF.toByte()
        )
    }

    /**
     * Wraps the NDEF message bytes in the NDEF file format (2-byte length prefix).
     */
    private fun buildNdefFile(): ByteArray {
        val ndefBytes = currentNdefMessage
        if (ndefBytes == null || ndefBytes.isEmpty()) {
            // Empty NDEF file: length = 0
            return byteArrayOf(0x00, 0x00)
        }
        val out = ByteArrayOutputStream()
        // 2-byte length prefix
        out.write((ndefBytes.size shr 8) and 0xFF)
        out.write(ndefBytes.size and 0xFF)
        out.write(ndefBytes)
        return out.toByteArray()
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (commandApdu.size < 4) {
            return SW_WRONG_PARAMS
        }

        val ins = commandApdu[1]

        return when (ins) {
            INS_SELECT -> handleSelect(commandApdu)
            INS_READ_BINARY -> handleReadBinary(commandApdu)
            else -> {
                Timber.w("NFC: Unsupported INS: 0x%02X", ins)
                SW_INS_NOT_SUPPORTED
            }
        }
    }

    private fun handleSelect(apdu: ByteArray): ByteArray {
        // P1 determines select type
        val p1 = apdu[2]
        val dataLen = if (apdu.size > 4) apdu[4].toInt() and 0xFF else 0
        val data = if (dataLen > 0 && apdu.size >= 5 + dataLen) {
            apdu.copyOfRange(5, 5 + dataLen)
        } else {
            byteArrayOf()
        }

        return when {
            // Select by AID (P1=0x04)
            p1 == 0x04.toByte() && data.contentEquals(NDEF_AID) -> {
                Timber.d("NFC: NDEF Application selected")
                selectedFile = SelectedFile.NONE
                SW_OK
            }
            // Select by File ID (P1=0x00)
            p1 == 0x00.toByte() && data.contentEquals(CC_FILE_ID) -> {
                Timber.d("NFC: CC file selected")
                selectedFile = SelectedFile.CC
                SW_OK
            }
            p1 == 0x00.toByte() && data.contentEquals(NDEF_FILE_ID) -> {
                Timber.d("NFC: NDEF file selected")
                selectedFile = SelectedFile.NDEF
                SW_OK
            }
            else -> {
                Timber.w("NFC: SELECT not found (P1=0x%02X, data=%s)", p1, data.toHexString())
                SW_NOT_FOUND
            }
        }
    }

    private fun handleReadBinary(apdu: ByteArray): ByteArray {
        val offset = ((apdu[2].toInt() and 0xFF) shl 8) or (apdu[3].toInt() and 0xFF)
        val le = if (apdu.size > 4) apdu[4].toInt() and 0xFF else 0

        val fileData = when (selectedFile) {
            SelectedFile.CC -> {
                val ndefFile = buildNdefFile()
                buildCapabilityContainer(ndefFile.size.coerceAtMost(MAX_NDEF_SIZE))
            }
            SelectedFile.NDEF -> buildNdefFile()
            SelectedFile.NONE -> {
                Timber.w("NFC: READ BINARY with no file selected")
                return SW_NOT_FOUND
            }
        }

        if (offset >= fileData.size) {
            return SW_WRONG_PARAMS
        }

        val end = (offset + le).coerceAtMost(fileData.size)
        val responseData = fileData.copyOfRange(offset, end)

        return responseData + SW_OK
    }

    override fun onDeactivated(reason: Int) {
        Timber.d("NFC: Deactivated, reason: %d", reason)
        selectedFile = SelectedFile.NONE
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02X".format(it) }
}
