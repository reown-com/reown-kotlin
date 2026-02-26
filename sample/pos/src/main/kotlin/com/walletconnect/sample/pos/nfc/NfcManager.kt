@file:JvmSynthetic

package com.walletconnect.sample.pos.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import timber.log.Timber

/**
 * Manages NFC payment link delivery for the POS payment flow.
 *
 * Uses Ingenico USDK NFC tag emulation — the POS acts as an NDEF tag
 * that both Android and iOS wallets can read by tapping the terminal.
 */
internal object NfcManager {

    @Volatile
    private var currentUri: String? = null

    @Volatile
    private var isEnabled: Boolean = false

    /**
     * Sets the payment URI to deliver via NFC tag emulation.
     * Call when the payment QR code is generated.
     */
    fun updatePaymentUri(uri: String) {
        currentUri = uri
        Timber.d("NFC: Payment URI set: %s", uri)
        if (isEnabled) {
            val ndefBytes = NdefMessage(arrayOf(NdefRecord.createUri(uri))).toByteArray()
            IngenicoNfcTagEmulator.enable(ndefBytes, timeoutSeconds = 30) {
                Timber.d("NFC: Tag being read")
            }
        }
    }

    /**
     * Clears the payment URI. Call when payment completes, errors, or is cancelled.
     */
    fun clearPaymentUri() {
        currentUri = null
        Timber.d("NFC: Payment URI cleared")
    }

    /**
     * Enables NFC tag emulation. Call from Activity.onResume().
     */
    fun enable() {
        isEnabled = true
        val uri = currentUri ?: return
        val ndefBytes = NdefMessage(arrayOf(NdefRecord.createUri(uri))).toByteArray()
        IngenicoNfcTagEmulator.enable(ndefBytes, timeoutSeconds = 30) {
            Timber.d("NFC: Tag being read")
        }
    }

    /**
     * Disables NFC tag emulation. Call from Activity.onPause().
     */
    fun disable() {
        isEnabled = false
        IngenicoNfcTagEmulator.disable()
    }
}
