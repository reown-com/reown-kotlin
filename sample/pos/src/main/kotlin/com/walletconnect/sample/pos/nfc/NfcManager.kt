package com.walletconnect.sample.pos.nfc

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import timber.log.Timber

/**
 * Manages NFC payment link delivery for the POS payment flow.
 *
 * The Ingenico AXIUM DX8000 does NOT support HCE (card emulation).
 * Its NFC hardware is reader-only. We use NFC Reader Mode to detect
 * phone taps and deliver the payment URL through two mechanisms:
 *
 * 1. **NDEF Write** — If the phone presents as an NDEF-writable target
 *    (e.g., via an NFC tag attached to the phone case), write the URL directly.
 *
 * 2. **IsoDep APDU** — If the phone responds via IsoDep (HCE), send the
 *    payment URL to a wallet HCE service registered for the Reown Pay AID.
 */
internal object NfcManager {

    /**
     * Custom AID for Reown Pay NFC communication (POS reader → wallet HCE).
     * "F" prefix = proprietary AID per ISO 7816-5.
     * The wallet app must register an HCE service for this AID.
     */
    private const val REOWN_PAY_AID = "F052454F574E504159" // hex(F0) + hex("REOWNPAY")

    private const val MAX_APDU_DATA = 250
    private const val INS_PUSH_URL: Byte = 0x01

    @Volatile
    private var currentUri: String? = null

    @Volatile
    private var currentNdefMessage: NdefMessage? = null

    /**
     * Sets the payment URI to deliver via NFC.
     * Call when the payment QR code is generated.
     */
    fun updatePaymentUri(uri: String) {
        try {
            currentUri = uri
            val ndefRecord = NdefRecord.createUri(uri)
            currentNdefMessage = NdefMessage(arrayOf(ndefRecord))
            // Also set on HCE service for devices that support card emulation
            NdefHostApduService.currentNdefMessage = currentNdefMessage!!.toByteArray()
            Timber.d("NFC: Payment URI set: %s", uri)
        } catch (e: Exception) {
            Timber.e(e, "NFC: Failed to create NDEF message for URI")
        }
    }

    /**
     * Clears the payment URI. Call when payment completes, errors, or is cancelled.
     */
    fun clearPaymentUri() {
        currentUri = null
        currentNdefMessage = null
        NdefHostApduService.currentNdefMessage = null
        Timber.d("NFC: Payment URI cleared")
    }

    /**
     * Enables NFC reader mode to detect phone taps and deliver payment URLs.
     * Call from Activity.onResume().
     */
    fun enable(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity)
        if (adapter == null || !adapter.isEnabled) {
            Timber.w("NFC: Adapter not available or disabled")
            return
        }

        try {
            adapter.enableReaderMode(
                activity,
                ReaderCallback(),
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                Bundle().apply {
                    putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 1500)
                }
            )
            Timber.d("NFC: Reader mode enabled")
        } catch (e: Exception) {
            Timber.e(e, "NFC: Failed to enable reader mode")
        }
    }

    /**
     * Disables NFC reader mode. Call from Activity.onPause().
     */
    fun disable(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        try {
            adapter.disableReaderMode(activity)
            Timber.d("NFC: Reader mode disabled")
        } catch (_: Exception) { }
    }

    fun isNfcAvailable(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
    }

    fun isNfcEnabled(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return false
        return adapter.isEnabled
    }

    fun isHceSupported(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
    }

    /**
     * Reader mode callback — handles detected NFC tags/phones.
     */
    private class ReaderCallback : NfcAdapter.ReaderCallback {
        override fun onTagDiscovered(tag: Tag) {
            val uri = currentUri
            val ndefMsg = currentNdefMessage

            Timber.d("NFC: Tag discovered — techs: [%s]", tag.techList.joinToString())

            if (uri == null || ndefMsg == null) {
                Timber.d("NFC: Tag discovered but no payment URI set, ignoring")
                return
            }

            // Try IsoDep first (phone with wallet HCE service)
            val isoDep = IsoDep.get(tag)
            if (isoDep != null) {
                sendViaIsoDep(isoDep, uri)
                return
            }

            // Try NDEF write (physical NFC tag or NDEF-capable target)
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                writeNdef(ndef, ndefMsg)
                return
            }

            // Try formatting + writing (unformatted NFC tag)
            val formatable = NdefFormatable.get(tag)
            if (formatable != null) {
                formatAndWriteNdef(formatable, ndefMsg)
                return
            }

            Timber.w("NFC: Tag has no supported technology for URL delivery")
        }
    }

    /**
     * Sends payment URL to wallet app's HCE service via IsoDep APDUs.
     */
    private fun sendViaIsoDep(isoDep: IsoDep, uri: String) {
        try {
            isoDep.connect()
            isoDep.timeout = 5000
            Timber.d("NFC: IsoDep connected (maxTransceive=%d)", isoDep.maxTransceiveLength)

            // Select Reown Pay AID on the wallet's HCE service
            val selectResponse = isoDep.transceive(buildSelectApdu(REOWN_PAY_AID))
            if (!isStatusOk(selectResponse)) {
                Timber.w(
                    "NFC: Wallet SELECT failed — SW=%s. " +
                        "The wallet app may not have a Reown Pay HCE service installed.",
                    selectResponse.statusWord()
                )
                return
            }
            Timber.d("NFC: Wallet HCE service selected")

            // Send payment URL in chunks if needed
            val urlBytes = uri.toByteArray(Charsets.UTF_8)
            val totalChunks = (urlBytes.size + MAX_APDU_DATA - 1) / MAX_APDU_DATA

            for (i in 0 until totalChunks) {
                val offset = i * MAX_APDU_DATA
                val length = minOf(MAX_APDU_DATA, urlBytes.size - offset)
                val chunk = urlBytes.copyOfRange(offset, offset + length)
                val isLast = i == totalChunks - 1

                val dataApdu = buildDataApdu(
                    ins = INS_PUSH_URL,
                    p1 = if (isLast) 0x00 else 0x01,
                    data = chunk
                )
                val response = isoDep.transceive(dataApdu)
                if (!isStatusOk(response)) {
                    Timber.w(
                        "NFC: Wallet rejected URL chunk %d/%d — SW=%s",
                        i + 1, totalChunks, response.statusWord()
                    )
                    return
                }
            }
            Timber.d("NFC: Payment URL sent via IsoDep (%d bytes)", urlBytes.size)
        } catch (e: Exception) {
            Timber.e(e, "NFC: IsoDep communication failed")
        } finally {
            try { isoDep.close() } catch (_: Exception) { }
        }
    }

    /**
     * Writes NDEF message to an existing NDEF tag.
     */
    private fun writeNdef(ndef: Ndef, message: NdefMessage) {
        try {
            ndef.connect()
            if (!ndef.isWritable) {
                Timber.w("NFC: NDEF tag is read-only")
                return
            }
            if (ndef.maxSize < message.toByteArray().size) {
                Timber.w("NFC: NDEF tag too small (max=%d, need=%d)", ndef.maxSize, message.toByteArray().size)
                return
            }
            ndef.writeNdefMessage(message)
            Timber.d("NFC: Payment URL written to NDEF tag")
        } catch (e: Exception) {
            Timber.e(e, "NFC: Failed to write NDEF tag")
        } finally {
            try { ndef.close() } catch (_: Exception) { }
        }
    }

    /**
     * Formats and writes NDEF message to an unformatted tag.
     */
    private fun formatAndWriteNdef(formatable: NdefFormatable, message: NdefMessage) {
        try {
            formatable.connect()
            formatable.format(message)
            Timber.d("NFC: Payment URL written to formatted NDEF tag")
        } catch (e: Exception) {
            Timber.e(e, "NFC: Failed to format and write NDEF tag")
        } finally {
            try { formatable.close() } catch (_: Exception) { }
        }
    }

    private fun buildSelectApdu(aid: String): ByteArray {
        val aidBytes = aid.hexToBytes()
        return byteArrayOf(
            0x00,
            0xA4.toByte(),
            0x04,
            0x00,
            aidBytes.size.toByte(),
            *aidBytes,
            0x00
        )
    }

    private fun buildDataApdu(ins: Byte, p1: Byte, data: ByteArray): ByteArray {
        return byteArrayOf(
            0x00,
            ins,
            p1,
            0x00,
            data.size.toByte(),
            *data
        )
    }

    private fun isStatusOk(response: ByteArray): Boolean {
        return response.size >= 2 &&
            response[response.size - 2] == 0x90.toByte() &&
            response[response.size - 1] == 0x00.toByte()
    }

    private fun ByteArray.statusWord(): String {
        if (size < 2) return "??"
        return "%02X%02X".format(this[size - 2], this[size - 1])
    }

    private fun String.hexToBytes(): ByteArray {
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
