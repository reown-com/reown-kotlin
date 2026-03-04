@file:JvmSynthetic

package com.reown.sample.wallet.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import timber.log.Timber

/**
 * Reads payment URLs from NFC tags using Android's foreground dispatch API.
 *
 * Foreground dispatch uses the same NFC stack as the home screen tag dispatch,
 * ensuring consistent behaviour with USDK card emulation (which doesn't always
 * work with the lower-level enableReaderMode API on Samsung devices).
 *
 * When enabled, the foreground activity has priority for NFC tags — the system
 * dispatches matching tags directly to this activity instead of showing a chooser.
 */
internal class NfcPaymentReader(
    private val activity: Activity,
    private val onPaymentUrl: (String) -> Unit,
) {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getActivity(
            activity, 0,
            Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
    }

    private val intentFilters: Array<IntentFilter> by lazy {
        arrayOf(
            // TECH_DISCOVERED: catches NDEF tags from POS tag emulation
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        )
    }

    private val techLists: Array<Array<String>> by lazy {
        arrayOf(
            arrayOf(Ndef::class.java.name),
            arrayOf(android.nfc.tech.IsoDep::class.java.name)
        )
    }

    /**
     * Enables NFC foreground dispatch. Call from Activity.onResume().
     * Gives this activity priority for NFC tag delivery.
     */
    fun enable() {
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, intentFilters, techLists)
        Timber.d("NFC Reader: Foreground dispatch enabled")
    }

    /**
     * Disables NFC foreground dispatch. Call from Activity.onPause().
     */
    fun disable() {
        nfcAdapter?.disableForegroundDispatch(activity)
        Timber.d("NFC Reader: Foreground dispatch disabled")
    }

    /**
     * Process an NFC intent received via Activity.onNewIntent().
     * Returns true if a payment URL was extracted and delivered.
     */
    fun handleIntent(intent: Intent): Boolean {
        val action = intent.action
        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            action != NfcAdapter.ACTION_TAG_DISCOVERED
        ) return false

        Timber.d("NFC Reader: Handling intent action=%s, data=%s", action, intent.data)

        // Try NDEF extras first (works for NDEF_DISCOVERED and sometimes TECH_DISCOVERED)
        val url = extractFromNdefExtras(intent)
            ?: extractFromTag(intent)

        if (url != null) {
            Timber.d("NFC Reader: Payment URL found: %s", url)
            onPaymentUrl(url)
            return true
        }

        Timber.d("NFC Reader: No payment URL in NFC intent")
        return false
    }

    private fun extractFromNdefExtras(intent: Intent): String? {
        val ndefMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            ?: return null
        for (msg in ndefMessages) {
            val ndefMessage = msg as? NdefMessage ?: continue
            for (record in ndefMessage.records) {
                val url = extractUrlFromRecord(record)
                if (url != null) return url
            }
        }
        return null
    }

    private fun extractFromTag(intent: Intent): String? {
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
            Timber.e(e, "NFC Reader: Error reading NDEF from tag")
            null
        } finally {
            try { ndef.close() } catch (_: Exception) {}
        }
    }

    private fun extractUrlFromRecord(record: NdefRecord): String? {
        val uri = record.toUri()?.toString() ?: return null
        if (isPaymentUrl(uri)) return unwrapPaymentUrl(uri)
        return null
    }

    private fun isPaymentUrl(url: String): Boolean =
        url.contains("pay.walletconnect.com") ||
            (url.contains("lab.reown.com/wallet") && url.contains("payUrl="))

    private fun unwrapPaymentUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            uri.getQueryParameter("payUrl") ?: url
        } catch (_: Exception) {
            url
        }
    }
}
