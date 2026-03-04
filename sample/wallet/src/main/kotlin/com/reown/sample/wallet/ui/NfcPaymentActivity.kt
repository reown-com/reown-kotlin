package com.reown.sample.wallet.ui

import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.reown.sample.wallet.nfc.PaymentHceService
import timber.log.Timber

/**
 * Translucent activity that intercepts payment URLs and redirects
 * to WalletKitActivity where the user can pick a payment option.
 *
 * Entry points:
 * - **App Links**: Browser redirects `pay.walletconnect.com` URLs here (primary NFC flow)
 * - **HCE**: PaymentHceService delivers payment URLs via custom APDU protocol
 * - **NDEF/TECH**: Fallback for foreground dispatch when wallet is already open
 */
class NfcPaymentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val paymentUrl = extractPaymentUrl(intent)
        if (paymentUrl == null) {
            Timber.w("NFC Payment: No payment URL found in intent")
            finish()
            return
        }
        Timber.d("NFC Payment: Redirecting with URL: %s", paymentUrl)
        openPaymentModal(paymentUrl)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val paymentUrl = extractPaymentUrl(intent) ?: return
        openPaymentModal(paymentUrl)
        finish()
    }

    private fun openPaymentModal(paymentUrl: String) {
        val intent = Intent(this, WalletKitActivity::class.java).apply {
            action = PaymentHceService.ACTION_PAYMENT_URL_RECEIVED
            putExtra(PaymentHceService.EXTRA_PAYMENT_URL, paymentUrl)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun extractPaymentUrl(intent: Intent?): String? {
        if (intent == null) return null

        Timber.d("NFC Payment: action=%s, data=%s", intent.action, intent.data)

        return when (intent.action) {
            // App Links or direct URL
            Intent.ACTION_VIEW -> intent.data?.let { unwrapPaymentUrl(it.toString()) }
            // HCE delivery
            PaymentHceService.ACTION_PAYMENT_URL_RECEIVED ->
                intent.getStringExtra(PaymentHceService.EXTRA_PAYMENT_URL)
            // NDEF tag dispatch
            NfcAdapter.ACTION_NDEF_DISCOVERED -> extractFromNdefExtras(intent)
            // TECH fallback (foreground dispatch)
            NfcAdapter.ACTION_TECH_DISCOVERED ->
                extractFromTag(intent) ?: extractFromNdefExtras(intent)
            else -> null
        }
    }

    private fun extractFromNdefExtras(intent: Intent): String? {
        val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (messages != null) {
            for (msg in messages) {
                val ndefMessage = msg as? NdefMessage ?: continue
                for (record in ndefMessage.records) {
                    val uri = record.toUri()?.toString() ?: continue
                    if (isPaymentUrl(uri)) return unwrapPaymentUrl(uri)
                }
            }
        }
        return intent.data?.toString()?.let { unwrapPaymentUrl(it) }
    }

    private fun extractFromTag(intent: Intent): String? {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return null
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            val ndefMessage = ndef.ndefMessage ?: return null
            ndefMessage.records.firstNotNullOfOrNull { record ->
                record.toUri()?.toString()?.takeIf { isPaymentUrl(it) }?.let { unwrapPaymentUrl(it) }
            }
        } catch (e: Exception) {
            Timber.e(e, "NFC Payment: Error reading NDEF from tag")
            null
        } finally {
            try { ndef.close() } catch (_: Exception) {}
        }
    }

    private fun isPaymentUrl(url: String): Boolean =
        url.contains("pay.walletconnect.com")

    private fun unwrapPaymentUrl(url: String): String =
        try { Uri.parse(url).getQueryParameter("payUrl") ?: url } catch (_: Exception) { url }
}
