package com.walletconnect.sample.pos.nfc

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.cardemulation.CardEmulation
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import timber.log.Timber
import java.lang.ref.WeakReference
import java.net.URLEncoder

/**
 * Manages NFC payment link delivery for the POS payment flow.
 *
 * The Ingenico AXIUM DX8000 does NOT support HCE (card emulation).
 * Its NFC hardware is reader-only. We use NFC Reader Mode to detect
 * phone taps and deliver the payment URL through multiple mechanisms:
 *
 * 1. **Custom APDU** (primary) — Sends the payment URL via a proprietary
 *    PUSH_URL command to wallet HCE services registered for the Reown Pay AID.
 *    Single tap, no system dialog — best UX for Android wallets.
 *
 * 2. **NDEF Type 4 Write** (fallback) — Manually performs the NFC Forum Type 4
 *    Tag write protocol over IsoDep. For wallets that implement standard NDEF
 *    Type 4 Tag emulation via HCE.
 *
 * 3. **Physical NDEF tag** — Writes to physical NFC tags. This enables iOS
 *    wallets to receive payment links: the POS writes the URL to a physical
 *    tag, and the iOS phone reads from it using CoreNFC.
 */
internal object NfcManager {

    /** Custom AID for Reown Pay NFC communication (primary). */
    private const val REOWN_PAY_AID = "F052454F574E504159" // hex(F0) + hex("REOWNPAY")

    /** Standard NDEF Tag Application AID per NFC Forum Type 4 spec (fallback). */
    private const val NDEF_AID = "D2760000850101"

    private const val MAX_APDU_DATA = 250
    private const val INS_PUSH_URL: Byte = 0x01

    // NDEF file ID for Type 4 Tag
    private val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04.toByte())

    private enum class NfcMode { NONE, READER, HCE }

    /**
     * When true, reader mode is disabled and the device acts purely as an
     * NFC card (HCE tag). This lets iOS phones read the payment URL from
     * the POS via CoreNFC, avoiding the reader-reader collision that
     * occurs when both devices try to poll simultaneously.
     */
    @Volatile
    var hceOnlyMode: Boolean = false

    @Volatile
    private var currentMode: NfcMode = NfcMode.NONE

    private var activityRef: WeakReference<Activity>? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var currentUri: String? = null

    @Volatile
    private var currentNdefMessage: NdefMessage? = null

    /**
     * Universal Link prefix for the iOS wallet app. When in HCE mode, the payment
     * URL is wrapped inside this Universal Link so iOS Background Tag Reading can
     * route the NDEF tag directly to the wallet app without showing a scan sheet.
     */
    var walletUniversalLink: String = "https://lab.reown.com/wallet"

    /**
     * Sets the payment URI to deliver via NFC.
     * Call when the payment QR code is generated.
     */
    fun updatePaymentUri(uri: String) {
        try {
            currentUri = uri
            val ndefRecord = NdefRecord.createUri(uri)
            currentNdefMessage = NdefMessage(arrayOf(ndefRecord))

            // For HCE, wrap the payment URL inside the wallet's Universal Link
            // so iOS Background Tag Reading delivers it directly to the app.
            val hceUri = "$walletUniversalLink?payUrl=${URLEncoder.encode(uri, "UTF-8")}"
            val hceRecord = NdefRecord.createUri(hceUri)
            val hceMessage = NdefMessage(arrayOf(hceRecord))
            NdefHostApduService.currentNdefMessage = hceMessage.toByteArray()

            Timber.d("NFC: Payment URI set: %s", uri)
            Timber.d("NFC: HCE Universal Link: %s", hceUri)
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
     * Enables the appropriate NFC mode based on [hceOnlyMode].
     * Call from Activity.onResume() or when the toggle changes.
     */
    fun enable(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity)
        if (adapter == null || !adapter.isEnabled) {
            Timber.w("NFC: Adapter not available or disabled")
            return
        }

        activityRef = WeakReference(activity)

        // Log NFC capabilities for diagnostics
        val hasNfc = isNfcAvailable(activity)
        val nfcEnabled = isNfcEnabled(activity)
        val hasHce = isHceSupported(activity)
        Timber.d("NFC: Device capabilities — NFC=%b, enabled=%b, HCE=%b", hasNfc, nfcEnabled, hasHce)

        if (hasHce) {
            try {
                val cardEmulation = CardEmulation.getInstance(adapter)
                val component = ComponentName(activity, NdefHostApduService::class.java)
                val isDefault = cardEmulation.isDefaultServiceForAid(component, NDEF_AID)
                Timber.d("NFC: NdefHostApduService is default for NDEF AID: %b", isDefault)
            } catch (e: Exception) {
                Timber.w(e, "NFC: Failed to query CardEmulation")
            }
        } else {
            Timber.w("NFC: HCE NOT supported on this device — HCE mode will not work")
        }

        val targetMode = if (hceOnlyMode) NfcMode.HCE else NfcMode.READER
        if (currentMode == targetMode) return

        // Always disable reader mode first to ensure a clean NFC controller state.
        // This is safe to call even if reader mode is not active (it's a no-op).
        try { adapter.disableReaderMode(activity) } catch (_: Exception) { }

        if (targetMode == NfcMode.HCE) {
            // Register as the preferred HCE service so Android routes the NDEF AID
            // directly to our service without showing a disambiguation dialog.
            setPreferredHceService(activity)
            currentMode = NfcMode.HCE
            Timber.d("NFC: HCE-only mode — reader disabled, acting as tag for external readers")
            return
        }

        // Leaving HCE mode — unset preferred service
        unsetPreferredHceService(activity)

        try {
            // Skip NDEF check — Android's built-in NDEF discovery doesn't work
            // with HCE services. We perform the NDEF Type 4 write manually via IsoDep.
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
            currentMode = NfcMode.READER
            Timber.d("NFC: Reader mode enabled")
        } catch (e: Exception) {
            currentMode = NfcMode.NONE
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
            unsetPreferredHceService(activity)
            currentMode = NfcMode.NONE
            activityRef = null
            Timber.d("NFC: NFC disabled")
        } catch (_: Exception) { }
    }

    /**
     * Re-arms HCE after a link-loss deactivation.
     * Samsung NFC controllers don't automatically resume HCE field detection after
     * DEACTIVATION_LINK_LOSS. Briefly toggling reader mode on/off resets the controller.
     */
    fun rearmHce() {
        if (!hceOnlyMode) return
        val activity = activityRef?.get() ?: return
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return

        mainHandler.postDelayed({
            val act = activityRef?.get() ?: return@postDelayed
            if (!hceOnlyMode) return@postDelayed
            try {
                // Briefly enable reader mode to kick the NFC controller
                adapter.enableReaderMode(
                    act,
                    { },
                    NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B,
                    null
                )
                Timber.d("NFC: Re-arm — reader mode toggled on")
            } catch (_: Exception) { }

            mainHandler.postDelayed({
                val act2 = activityRef?.get() ?: return@postDelayed
                if (!hceOnlyMode) return@postDelayed
                try {
                    adapter.disableReaderMode(act2)
                    currentMode = NfcMode.HCE
                    Timber.d("NFC: Re-arm — reader mode toggled off, HCE ready")
                } catch (_: Exception) { }
            }, 50L)
        }, 100L)
    }

    private fun setPreferredHceService(activity: Activity) {
        if (!isHceSupported(activity)) return
        try {
            val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
            val cardEmulation = CardEmulation.getInstance(adapter)
            val component = ComponentName(activity, NdefHostApduService::class.java)
            cardEmulation.setPreferredService(activity, component)
            Timber.d("NFC: Preferred HCE service set — no disambiguation dialog")
        } catch (e: Exception) {
            Timber.w(e, "NFC: Failed to set preferred HCE service")
        }
    }

    private fun unsetPreferredHceService(activity: Activity) {
        if (!isHceSupported(activity)) return
        try {
            val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
            val cardEmulation = CardEmulation.getInstance(adapter)
            cardEmulation.unsetPreferredService(activity)
            Timber.d("NFC: Preferred HCE service unset")
        } catch (e: Exception) {
            Timber.w(e, "NFC: Failed to unset preferred HCE service")
        }
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

            // Try IsoDep — phone with wallet HCE service
            val isoDep = IsoDep.get(tag)
            if (isoDep != null) {
                sendViaIsoDep(isoDep, uri)
                return
            }

            // Fallback: NDEF write (physical NFC tag — enables iOS wallets)
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                writeNdef(ndef, ndefMsg)
                return
            }

            // Fallback: format + write (unformatted physical NFC tag)
            val formatable = NdefFormatable.get(tag)
            if (formatable != null) {
                formatAndWriteNdef(formatable, ndefMsg)
                return
            }

            Timber.w("NFC: Tag has no supported technology for URL delivery")
        }
    }

    /**
     * Sends payment URL to wallet app via IsoDep.
     *
     * Tries two approaches within the same connection:
     * 1. Custom Reown Pay APDU protocol (SELECT custom AID → PUSH_URL)
     *    — single tap, no system dialog, best UX
     * 2. Standard NDEF Type 4 write (SELECT NDEF AID → UPDATE BINARY)
     *    — fallback for wallets that implement standard NDEF emulation
     */
    private fun sendViaIsoDep(isoDep: IsoDep, uri: String) {
        try {
            isoDep.connect()
            isoDep.timeout = 5000
            Timber.d("NFC: IsoDep connected (maxTransceive=%d)", isoDep.maxTransceiveLength)

            // Try custom Reown Pay protocol first (best UX — single tap, no dialog)
            if (tryCustomAidPush(isoDep, uri)) {
                Timber.d("NFC: Payment URL sent via Reown Pay protocol")
                return
            }

            // Fallback: standard NDEF Type 4 write (interoperable)
            if (tryNdefType4Write(isoDep, uri)) {
                Timber.d("NFC: Payment URL sent via NDEF Type 4 write")
                return
            }

            Timber.w("NFC: Neither custom AID nor NDEF worked")
        } catch (e: Exception) {
            Timber.e(e, "NFC: IsoDep communication failed")
        } finally {
            try { isoDep.close() } catch (_: Exception) { }
        }
    }

    /**
     * Performs the NFC Forum Type 4 Tag write protocol manually over IsoDep.
     *
     * Steps:
     * 1. SELECT NDEF Application (AID D2760000850101)
     * 2. SELECT NDEF File (E104)
     * 3. UPDATE BINARY — clear length (marks write-in-progress)
     * 4. UPDATE BINARY — write NDEF message bytes (may be chunked)
     * 5. UPDATE BINARY — set final length (marks write-complete)
     *
     * Returns true if the write succeeded.
     */
    private fun tryNdefType4Write(isoDep: IsoDep, uri: String): Boolean {
        // 1. SELECT NDEF Application
        val selectAppResp = isoDep.transceive(buildSelectApdu(NDEF_AID))
        if (!isStatusOk(selectAppResp)) {
            Timber.d("NFC: NDEF AID not available — SW=%s", selectAppResp.statusWord())
            return false
        }
        Timber.d("NFC: NDEF Application selected")

        // 2. SELECT NDEF File (E104)
        val selectFileApdu = byteArrayOf(
            0x00, 0xA4.toByte(), 0x00, 0x0C,
            0x02, NDEF_FILE_ID[0], NDEF_FILE_ID[1]
        )
        val selectFileResp = isoDep.transceive(selectFileApdu)
        if (!isStatusOk(selectFileResp)) {
            Timber.w("NFC: NDEF file SELECT failed — SW=%s", selectFileResp.statusWord())
            return false
        }
        Timber.d("NFC: NDEF file selected")

        // Build NDEF message
        val ndefRecord = NdefRecord.createUri(uri)
        val ndefMessage = NdefMessage(arrayOf(ndefRecord))
        val ndefBytes = ndefMessage.toByteArray()
        Timber.d("NFC: NDEF message size: %d bytes", ndefBytes.size)

        // 3. UPDATE BINARY — clear length (write-in-progress marker)
        val clearLenApdu = byteArrayOf(
            0x00, 0xD6.toByte(), 0x00, 0x00,
            0x02, 0x00, 0x00
        )
        val clearResp = isoDep.transceive(clearLenApdu)
        if (!isStatusOk(clearResp)) {
            Timber.w("NFC: Failed to clear NDEF length — SW=%s", clearResp.statusWord())
            return false
        }

        // 4. UPDATE BINARY — write NDEF message at offset 2 (after length prefix)
        val maxChunk = minOf(MAX_APDU_DATA, isoDep.maxTransceiveLength - 7)
        var offset = 0
        while (offset < ndefBytes.size) {
            val chunkSize = minOf(maxChunk, ndefBytes.size - offset)
            val chunk = ndefBytes.copyOfRange(offset, offset + chunkSize)
            val fileOffset = offset + 2 // skip 2-byte length prefix

            val updateApdu = byteArrayOf(
                0x00, 0xD6.toByte(),
                ((fileOffset shr 8) and 0xFF).toByte(),
                (fileOffset and 0xFF).toByte(),
                chunk.size.toByte(),
                *chunk
            )
            val updateResp = isoDep.transceive(updateApdu)
            if (!isStatusOk(updateResp)) {
                Timber.w("NFC: Failed to write NDEF data at offset %d — SW=%s", fileOffset, updateResp.statusWord())
                return false
            }
            offset += chunkSize
        }

        // 5. UPDATE BINARY — set final length (write-complete marker)
        val lenHi = ((ndefBytes.size shr 8) and 0xFF).toByte()
        val lenLo = (ndefBytes.size and 0xFF).toByte()
        val setLenApdu = byteArrayOf(
            0x00, 0xD6.toByte(), 0x00, 0x00,
            0x02, lenHi, lenLo
        )
        val setLenResp = isoDep.transceive(setLenApdu)
        if (!isStatusOk(setLenResp)) {
            Timber.w("NFC: Failed to set NDEF length — SW=%s", setLenResp.statusWord())
            return false
        }

        Timber.d("NFC: NDEF Type 4 write complete (%d bytes)", ndefBytes.size)
        return true
    }

    /**
     * Sends payment URL using the custom Reown Pay APDU protocol.
     * Returns true if the send succeeded.
     */
    private fun tryCustomAidPush(isoDep: IsoDep, uri: String): Boolean {
        val selectResponse = isoDep.transceive(buildSelectApdu(REOWN_PAY_AID))
        if (!isStatusOk(selectResponse)) {
            Timber.d("NFC: Reown Pay AID not available — SW=%s", selectResponse.statusWord())
            return false
        }
        Timber.d("NFC: Reown Pay HCE service selected")

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
                Timber.w("NFC: URL chunk %d/%d rejected — SW=%s", i + 1, totalChunks, response.statusWord())
                return false
            }
        }

        Timber.d("NFC: Payment URL sent via custom protocol (%d bytes)", urlBytes.size)
        return true
    }

    /**
     * Writes NDEF message to an existing NDEF tag.
     * Enables iOS wallets to receive payment links via physical NFC tags.
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
