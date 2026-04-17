@file:JvmSynthetic

package com.reown.sample.wallet.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.tech.Ndef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Reads payment URLs from NFC tags using Android's foreground dispatch API.
 *
 * When enabled, the foreground activity has priority for NFC tags — the system
 * dispatches matching tags directly to this activity instead of showing a chooser.
 */
internal class NfcPaymentReader(
    private val activity: Activity,
    private val scope: CoroutineScope,
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

        val url = NfcPaymentUrlExtractor.extractFromNdefExtras(intent)
        if (url != null) {
            Timber.d("NFC Reader: Payment URL found: %s", url)
            onPaymentUrl(url)
            return true
        }

        // Tag I/O is blocking RF — dispatch off the main thread
        scope.launch(Dispatchers.IO) {
            val tagUrl = NfcPaymentUrlExtractor.extractFromTag(intent)
            if (tagUrl != null) {
                Timber.d("NFC Reader: Payment URL found from tag: %s", tagUrl)
                onPaymentUrl(tagUrl)
            } else {
                Timber.d("NFC Reader: No payment URL in NFC intent")
            }
        }
        return true
    }
}
