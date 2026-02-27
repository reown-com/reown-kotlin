package com.reown.sample.wallet.ui

import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reown.sample.wallet.nfc.PaymentHceService
import timber.log.Timber

/**
 * Translucent overlay activity for NFC quick-pay (Apple Pay / Google Pay-like UX).
 *
 * Launched when the user taps the phone on a POS terminal from the home screen.
 * Intercepts NDEF_DISCOVERED intents containing the payment URL, then runs the
 * full auto-pay pipeline without user interaction.
 *
 * If all payment options require collectData (information capture), falls back
 * to the full WalletKitActivity payment flow.
 */
class NfcPaymentActivity : AppCompatActivity() {

    companion object {
        /** Must match the MIME type used by POS NfcManager */
        private const val REOWN_PAY_MIME = "application/vnd.reown.pay"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val paymentUrl = extractPaymentUrl(intent)
        if (paymentUrl == null) {
            Timber.w("NFC QuickPay: No payment URL found in intent")
            finish()
            return
        }
        Timber.d("NFC QuickPay: Starting with URL: %s", paymentUrl)

        setContent {
            val vm: NfcQuickPayViewModel = viewModel()

            // Start payment on first composition
            androidx.compose.runtime.LaunchedEffect(Unit) {
                vm.startPayment(paymentUrl)
            }

            NfcQuickPayScreen(
                viewModel = vm,
                onDone = { finish() },
                onFallback = { url -> fallbackToFullApp(url) }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Ignore additional taps while payment is in progress
    }

    /**
     * Extract payment URL from the intent.
     *
     * Supports:
     * - NDEF_DISCOVERED: NDEF tag with URI record (from POS tag emulation)
     *   The URI may be a Universal Link wrapper: https://lab.reown.com/wallet?payUrl=<encoded>
     * - TECH_DISCOVERED: Fallback for ISO-DEP / NFC-A/B tags where NDEF_DISCOVERED didn't fire.
     *   Reads NDEF manually from the tag.
     * - HCE delivery: ACTION_PAYMENT_URL_RECEIVED from PaymentHceService
     * - ACTION_VIEW: Android App Link fallback (lab.reown.com/wallet?payUrl=...)
     */
    private fun extractPaymentUrl(intent: Intent?): String? {
        if (intent == null) return null

        Timber.d("NFC QuickPay: Intent action=%s, data=%s", intent.action, intent.data)

        // HCE-delivered payment URL
        if (intent.action == PaymentHceService.ACTION_PAYMENT_URL_RECEIVED) {
            return intent.getStringExtra(PaymentHceService.EXTRA_PAYMENT_URL)
        }

        // NDEF_DISCOVERED: extract URI from intent data or NDEF extras
        if (intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
            return extractFromNdefIntent(intent)
        }

        // TECH_DISCOVERED fallback: manually read NDEF from the tag
        if (intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            Timber.d("NFC QuickPay: TECH_DISCOVERED — reading NDEF from tag")
            return extractFromTechIntent(intent)
                ?: extractFromNdefIntent(intent) // Some tags include NDEF extras even in TECH
        }

        // ACTION_VIEW: Android App Link fallback — if the NFC URL opens in the browser,
        // App Links verification redirects lab.reown.com/wallet to this activity
        if (intent.action == Intent.ACTION_VIEW) {
            val rawUri = intent.data?.toString()
            if (rawUri != null) {
                Timber.d("NFC QuickPay: ACTION_VIEW — extracting from: %s", rawUri)
                return unwrapPaymentUrl(rawUri)
            }
        }

        return null
    }

    private fun extractFromNdefIntent(intent: Intent): String? {
        // Parse NDEF messages from extras (handles both MIME and URI records)
        val ndefMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (ndefMessages != null) {
            for (msg in ndefMessages) {
                val ndefMessage = msg as? NdefMessage ?: continue
                for (record in ndefMessage.records) {
                    val url = extractUrlFromRecord(record)
                    if (url != null) return url
                }
            }
        }

        // Fallback: check intent.data for URI-based dispatch
        val rawUri = intent.data?.toString()
        if (rawUri != null) {
            return unwrapPaymentUrl(rawUri)
        }

        return null
    }

    /**
     * Extracts a payment URL from a single NDEF record.
     * Handles both MIME records (application/vnd.reown.pay) and URI records.
     */
    private fun extractUrlFromRecord(record: NdefRecord): String? {
        // MIME record: application/vnd.reown.pay — payload is the raw payment URL
        if (record.tnf == NdefRecord.TNF_MIME_MEDIA) {
            val mimeType = String(record.type, Charsets.US_ASCII)
            if (mimeType == REOWN_PAY_MIME) {
                val paymentUrl = String(record.payload, Charsets.UTF_8)
                Timber.d("NFC QuickPay: Found payment URL in MIME record: %s", paymentUrl)
                return paymentUrl
            }
        }

        // URI record
        val uri = record.toUri()?.toString() ?: return null
        val paymentUrl = unwrapPaymentUrl(uri)
        if (isPaymentUrl(paymentUrl)) return paymentUrl

        return null
    }

    private fun extractFromTechIntent(intent: Intent): String? {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return null
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            val ndefMessage = ndef.ndefMessage ?: return null
            for (record in ndefMessage.records) {
                val url = extractUrlFromRecord(record)
                if (url != null) return url
            }
            null
        } catch (e: Exception) {
            Timber.e(e, "NFC QuickPay: Error reading NDEF from tag")
            null
        } finally {
            try { ndef.close() } catch (_: Exception) {}
        }
    }

    private fun isPaymentUrl(url: String): Boolean =
        url.contains("pay.walletconnect.com") ||
            (url.contains("lab.reown.com/wallet") && url.contains("payUrl="))

    /**
     * Unwrap the payment URL from a Universal Link wrapper if present.
     * Input:  https://lab.reown.com/wallet?payUrl=https%3A%2F%2Fpay.walletconnect.com%2Fpay_xxx
     * Output: https://pay.walletconnect.com/pay_xxx
     *
     * If the URL is already a direct payment URL, returns it as-is.
     */
    private fun unwrapPaymentUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            uri.getQueryParameter("payUrl") ?: url
        } catch (e: Exception) {
            url
        }
    }

    /**
     * Fall back to full WalletKitActivity when auto-pay isn't possible
     * (e.g., all options require collectData).
     */
    private fun fallbackToFullApp(paymentUrl: String) {
        Timber.d("NFC QuickPay: Falling back to full app for: %s", paymentUrl)
        val intent = Intent(this, WalletKitActivity::class.java).apply {
            action = PaymentHceService.ACTION_PAYMENT_URL_RECEIVED
            putExtra(PaymentHceService.EXTRA_PAYMENT_URL, paymentUrl)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }
}
