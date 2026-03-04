@file:JvmSynthetic

package com.reown.sample.wallet.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import timber.log.Timber

/**
 * Reads payment URLs from NFC tags using Android's foreground dispatch API.
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
        arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
    }

    private val techLists: Array<Array<String>> by lazy {
        arrayOf(
            arrayOf(Ndef::class.java.name),
            arrayOf(android.nfc.tech.IsoDep::class.java.name)
        )
    }

    fun enable() {
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, intentFilters, techLists)
        Timber.d("NFC Reader: Foreground dispatch enabled")
    }

    fun disable() {
        nfcAdapter?.disableForegroundDispatch(activity)
        Timber.d("NFC Reader: Foreground dispatch disabled")
    }

    fun handleIntent(intent: Intent): Boolean {
        val action = intent.action
        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            action != NfcAdapter.ACTION_TAG_DISCOVERED
        ) return false

        Timber.d("NFC Reader: action=%s, data=%s", action, intent.data)

        val url = extractFromNdefExtras(intent) ?: extractFromTag(intent)
        if (url != null) {
            Timber.d("NFC Reader: Payment URL found: %s", url)
            onPaymentUrl(url)
            return true
        }

        Timber.d("NFC Reader: No payment URL in NFC intent")
        return false
    }

    private fun extractFromNdefExtras(intent: Intent): String? {
        val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            ?: return null
        for (msg in messages) {
            val ndefMessage = msg as? NdefMessage ?: continue
            for (record in ndefMessage.records) {
                val uri = record.toUri()?.toString() ?: continue
                if (isPaymentUrl(uri)) return unwrapPaymentUrl(uri)
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
            ndefMessage.records.firstNotNullOfOrNull { record ->
                record.toUri()?.toString()?.takeIf { isPaymentUrl(it) }?.let { unwrapPaymentUrl(it) }
            }
        } catch (e: Exception) {
            Timber.e(e, "NFC Reader: Error reading NDEF from tag")
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
