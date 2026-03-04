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
 * Translucent activity that intercepts NFC payment intents and redirects
 * to WalletKitActivity where the user can pick a payment option.
 *
 * Launched when the user taps the phone on a POS terminal from the home screen.
 * Extracts the payment URL from the NFC intent and opens the full payment modal.
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
        Timber.d("NFC Payment: Redirecting to full payment modal with URL: %s", paymentUrl)
        openFullPaymentModal(paymentUrl)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val paymentUrl = extractPaymentUrl(intent) ?: return
        openFullPaymentModal(paymentUrl)
        finish()
    }

    /**
     * Open WalletKitActivity with the payment URL to show the full payment modal.
     */
    private fun openFullPaymentModal(paymentUrl: String) {
        val intent = Intent(this, WalletKitActivity::class.java).apply {
            action = PaymentHceService.ACTION_PAYMENT_URL_RECEIVED
            putExtra(PaymentHceService.EXTRA_PAYMENT_URL, paymentUrl)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    /**
     * Extract payment URL from the intent.
     *
     * Supports:
     * - NDEF_DISCOVERED: NDEF tag with URI record (from POS tag emulation)
     * - TECH_DISCOVERED: Fallback for ISO-DEP / NFC-A/B tags where NDEF_DISCOVERED didn't fire.
     *   Reads NDEF manually from the tag.
     * - HCE delivery: ACTION_PAYMENT_URL_RECEIVED from PaymentHceService
     * - ACTION_VIEW: Android App Link fallback (lab.reown.com/wallet?payUrl=...)
     */
    private fun extractPaymentUrl(intent: Intent?): String? {
        if (intent == null) return null

        Timber.d("NFC Payment: Intent action=%s, data=%s", intent.action, intent.data)

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
            Timber.d("NFC Payment: TECH_DISCOVERED — reading NDEF from tag")
            return extractFromTechIntent(intent)
                ?: extractFromNdefIntent(intent) // Some tags include NDEF extras even in TECH
        }

        // ACTION_VIEW: Android App Link fallback
        if (intent.action == Intent.ACTION_VIEW) {
            val rawUri = intent.data?.toString()
            if (rawUri != null) {
                Timber.d("NFC Payment: ACTION_VIEW — extracting from: %s", rawUri)
                return unwrapPaymentUrl(rawUri)
            }
        }

        return null
    }

    private fun extractFromNdefIntent(intent: Intent): String? {
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

        val rawUri = intent.data?.toString()
        if (rawUri != null) {
            return unwrapPaymentUrl(rawUri)
        }

        return null
    }

    private fun extractUrlFromRecord(record: NdefRecord): String? {
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
            Timber.e(e, "NFC Payment: Error reading NDEF from tag")
            null
        } finally {
            try { ndef.close() } catch (_: Exception) {}
        }
    }

    private fun isPaymentUrl(url: String): Boolean =
        url.contains("pay.walletconnect.com") ||
            (url.contains("lab.reown.com/wallet") && url.contains("payUrl="))

    private fun unwrapPaymentUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            uri.getQueryParameter("payUrl") ?: url
        } catch (e: Exception) {
            url
        }
    }
}
