package com.reown.sample.wallet.ui

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.reown.sample.wallet.nfc.NfcPaymentUrlExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Translucent activity that intercepts payment URLs and redirects
 * to WalletKitActivity where the user can pick a payment option.
 *
 * Entry points:
 * - **App Links**: Browser redirects `pay.walletconnect.com` URLs here
 * - **NDEF**: POS terminal emits payment URI as an NDEF tag
 * - **TECH**: Fallback for ISO-DEP / NFC-A/B tag dispatch
 */
class NfcPaymentActivity : AppCompatActivity() {

    companion object {
        const val ACTION_PAYMENT_URL_RECEIVED = "com.reown.wallet.NFC_PAYMENT_URL"
        const val EXTRA_PAYMENT_URL = "payment_url"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNfcIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) { finish(); return }

        Timber.d("NFC Payment: action=%s, data=%s", intent.action, intent.data)

        // Fast path: check non-blocking sources first
        val url = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.let { NfcPaymentUrlExtractor.unwrapPaymentUrl(it.toString()) }
            NfcAdapter.ACTION_NDEF_DISCOVERED -> NfcPaymentUrlExtractor.extractFromNdefExtras(intent)
            NfcAdapter.ACTION_TECH_DISCOVERED -> NfcPaymentUrlExtractor.extractFromNdefExtras(intent)
            else -> null
        }

        if (url != null && NfcPaymentUrlExtractor.isPaymentUrl(url)) {
            openPaymentModal(url)
            finish()
            return
        }

        // Slow path: tag I/O is blocking RF — dispatch off main thread
        if (intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            scope.launch(Dispatchers.IO) {
                val tagUrl = NfcPaymentUrlExtractor.extractFromTag(intent)
                if (tagUrl != null) {
                    Timber.d("NFC Payment: Redirecting with URL: %s", tagUrl)
                    openPaymentModal(tagUrl)
                } else {
                    Timber.w("NFC Payment: No payment URL found in intent")
                }
                finish()
            }
            return
        }

        Timber.w("NFC Payment: No payment URL found in intent")
        finish()
    }

    private fun openPaymentModal(paymentUrl: String) {
        val intent = Intent(this, WalletKitActivity::class.java).apply {
            action = ACTION_PAYMENT_URL_RECEIVED
            putExtra(EXTRA_PAYMENT_URL, paymentUrl)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }
}
