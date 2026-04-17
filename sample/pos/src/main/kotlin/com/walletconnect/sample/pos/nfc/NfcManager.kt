@file:JvmSynthetic

package com.walletconnect.sample.pos.nfc

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import com.walletconnect.sample.pos.log.PosLogStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

/**
 * Manages NFC payment link delivery for the POS payment flow.
 *
 * Uses Android Host Card Emulation (HCE) to emulate an NFC Forum Type 4 Tag.
 * The POS acts as an NDEF tag that both Android and iOS wallets can read
 * by tapping the device.
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
 */
internal object NfcManager {

    private val _tapEventFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val tapEventFlow: SharedFlow<Unit> = _tapEventFlow.asSharedFlow()

    @Volatile
    private var currentUri: String? = null

    @Volatile
    private var isEnabled: Boolean = false

    private var appContext: Context? = null
    private var foregroundActivity: Activity? = null

    /**
     * Whether HCE NFC tag emulation is available on this device.
     */
    val isAvailable: Boolean
        get() {
            val ctx = appContext ?: return false
            val adapter = NfcAdapter.getDefaultAdapter(ctx) ?: return false
            return adapter.isEnabled &&
                ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
        }

    /**
     * Initializes the NFC manager. Call once during Activity.onCreate().
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Sets the foreground activity for HCE preferred service routing.
     * Call with the activity from onResume(), and null from onPause().
     */
    fun setActivity(activity: Activity?) {
        val previousActivity = foregroundActivity
        foregroundActivity = activity
        val ctx = appContext ?: return
        val adapter = NfcAdapter.getDefaultAdapter(ctx) ?: return
        val cardEmulation = CardEmulation.getInstance(adapter)
        val component = ComponentName(ctx, NdefHostApduService::class.java)
        if (activity != null) {
            cardEmulation.setPreferredService(activity, component)
        } else {
            previousActivity?.let { cardEmulation.unsetPreferredService(it) }
        }
    }

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
        NdefHostApduService.currentNdefBytes = null
        NdefHostApduService.onBeingReadCallback = null
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
        NdefHostApduService.currentNdefBytes = null
        NdefHostApduService.onBeingReadCallback = null
    }

    private fun emitNdef(paymentUri: String) {
        val ndefMessage = NdefMessage(
            arrayOf(
                NdefRecord.createUri(paymentUri)
            )
        )
        val ndefBytes = ndefMessage.toByteArray()
        PosLogStore.info(
            "NFC emulation enabled",
            source = "NfcManager",
            data = "uri: $paymentUri\nNDEF size: ${ndefBytes.size} bytes"
        )
        NdefHostApduService.currentNdefBytes = ndefBytes
        NdefHostApduService.onBeingReadCallback = {
            PosLogStore.info("NFC tag read by external device", source = "NfcManager")
            _tapEventFlow.tryEmit(Unit)
        }
    }
}
