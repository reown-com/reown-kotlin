@file:JvmSynthetic

package com.walletconnect.sample.pos.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import timber.log.Timber
import java.net.URLEncoder

/**
 * Manages NFC payment link delivery for the POS payment flow.
 *
 * Uses Ingenico USDK NFC tag emulation — the POS acts as an NDEF tag
 * that both Android and iOS wallets can read by tapping the terminal.
 *
 * The payment URL is wrapped in a Universal Link so that:
 * - **iOS**: Background Tag Reading recognises the URL as a Universal Link
 *   (applinks:lab.reown.com) and opens the wallet app directly via a
 *   system notification — no Safari involved.
 * - **Android**: NDEF_DISCOVERED intent filter on `lab.reown.com/wallet`
 *   launches the NfcPaymentActivity quick-pay overlay from the home screen.
 *
 * Wrapper format:
 *   https://lab.reown.com/wallet?payUrl=<url-encoded payment URL>
 */
internal object NfcManager {

    private const val UNIVERSAL_LINK_BASE = "https://lab.reown.com/wallet"

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

    /**
     * Custom MIME type for Android NDEF_DISCOVERED dispatch.
     * Samsung's NFC handler intercepts HTTPS URLs and opens them in the browser,
     * bypassing NDEF_DISCOVERED intent filters. Using a custom MIME type as the
     * first NDEF record ensures Android dispatches to our app instead.
     */
    private const val REOWN_PAY_MIME = "application/vnd.reown.pay"

    /**
     * Emits an NDEF message with two records:
     * 1. MIME record (application/vnd.reown.pay) — Android dispatches NDEF_DISCOVERED
     *    based on the first record's MIME type, bypassing Samsung's HTTPS URL interception.
     *    Payload contains the raw payment URL.
     * 2. URI record — Universal Link for iOS Background Tag Reading. iOS checks all
     *    records for Universal Links, so it picks this up even as the second record.
     */
    private fun emitNdef(paymentUri: String) {
        val wrappedUri = wrapInUniversalLink(paymentUri)
        Timber.d("NFC: Emitting wrapped URI: %s", wrappedUri)
        val ndefMessage = NdefMessage(
            arrayOf(
                NdefRecord.createMime(REOWN_PAY_MIME, paymentUri.toByteArray(Charsets.UTF_8)),
                NdefRecord.createUri(wrappedUri)
            )
        )
        Timber.d("NFC: NDEF message size: %d bytes", ndefMessage.toByteArray().size)
        IngenicoNfcTagEmulator.enable(ndefMessage.toByteArray(), timeoutSeconds = 30) {
            Timber.d("NFC: Tag being read")
        }
    }

    /**
     * Wraps a raw payment URL inside the wallet's Universal Link.
     *
     * Input:  https://pay.walletconnect.com/pay_abc123
     * Output: https://lab.reown.com/wallet?payUrl=https%3A%2F%2Fpay.walletconnect.com%2Fpay_abc123
     */
    private fun wrapInUniversalLink(paymentUri: String): String {
        val encoded = URLEncoder.encode(paymentUri, "UTF-8")
        return "$UNIVERSAL_LINK_BASE?payUrl=$encoded"
    }
}
