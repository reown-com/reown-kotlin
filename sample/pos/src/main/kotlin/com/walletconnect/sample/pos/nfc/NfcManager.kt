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
 *
 * The raw payment URL (e.g. https://pay.walletconnect.com/pay_abc123)
 * is emitted directly in the NDEF message. Wallets register for the
 * payment domain via:
 * - **iOS**: Universal Links (applinks:pay.walletconnect.com) — Background
 *   Tag Reading opens the wallet directly from the home screen.
 * - **Android**: NDEF_DISCOVERED intent filter — system shows a chooser
 *   when multiple wallets are installed.
 *
 * The NDEF message contains a single URI record with the payment URL.
 * Android opens the URL via the browser, which auto-redirects to the
 * wallet via verified App Links (if installed) or loads the payment
 * page directly (if not). No system "New tag scanned" dialog.
 * iOS Background Tag Reading uses the URI record for Universal Links.
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
            emitNdef(uri)
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
        emitNdef(uri)
    }

    /**
     * Disables NFC tag emulation. Call from Activity.onPause().
     */
    fun disable() {
        isEnabled = false
        IngenicoNfcTagEmulator.disable()
    }

    // TODO: Remove staging rewrite once production AASA is deployed
    private fun rewriteForStaging(uri: String): String =
        uri.replace("pay.walletconnect.com", "staging.pay.walletconnect.com")

    private fun emitNdef(paymentUri: String) {
        val stagingUri = rewriteForStaging(paymentUri)
        Timber.d("NFC: Emitting payment URI: %s (staging: %s)", paymentUri, stagingUri)
        val ndefMessage = NdefMessage(
            arrayOf(
                NdefRecord.createUri(stagingUri)
            )
        )
        Timber.d("NFC: NDEF message size: %d bytes", ndefMessage.toByteArray().size)
        IngenicoNfcTagEmulator.enable(ndefMessage.toByteArray(), timeoutSeconds = 30) {
            Timber.d("NFC: Tag being read")
        }
    }
}
