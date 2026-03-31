@file:JvmSynthetic

package com.walletconnect.sample.pos.nfc

import android.os.Handler
import android.os.Looper
import com.usdk.apiservice.aidl.data.BytesValue
import com.usdk.apiservice.aidl.rfreader.IoCtrlCmd
import com.usdk.apiservice.aidl.rfreader.OnNFCTagListener
import com.usdk.apiservice.aidl.rfreader.RFError
import com.walletconnect.sample.pos.log.PosLogStore
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Wraps the Ingenico USDK NFC tag emulation API.
 *
 * When enabled, the POS device acts as an NDEF tag — both Android and iOS
 * wallets can read the payment URI by tapping the terminal.
 *
 * Uses [URFReader.ioControl] with [IoCtrlCmd.SetCardEmuSwitch], [IoCtrlCmd.SetCardEmuTimeout],
 * and [IoCtrlCmd.SetCardEmuInfo] to control the hardware emulation, plus [UNFCTag.startEventListener]
 * for read/timeout callbacks.
 */
internal object IngenicoNfcTagEmulator {

    private const val DEFAULT_TIMEOUT_SECONDS = 30

    /** Watchdog fires a few seconds after the USDK timeout to catch missed callbacks. */
    private const val WATCHDOG_PADDING_MS = 5_000L

    /** Lock protecting state transitions to prevent races between disable() and AIDL callbacks. */
    private val lock = ReentrantLock()

    @Volatile
    private var currentNdefBytes: ByteArray? = null

    @Volatile
    private var currentTimeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS

    @Volatile
    private var onBeingReadCallback: (() -> Unit)? = null

    @Volatile
    private var active: Boolean = false

    private val handler = Handler(Looper.getMainLooper())

    private val watchdogRunnable = Runnable {
        lock.withLock {
            if (active) {
                Timber.d("NFC Tag Emulation: Watchdog — re-arming (callback may have been missed)")
                rearmLocked()
            }
        }
    }

    /**
     * Whether the USDK NFC tag emulation module is available on this device.
     */
    val isAvailable: Boolean
        get() = UsdkServiceHelper.isServiceBound &&
            UsdkServiceHelper.getRFReader() != null &&
            UsdkServiceHelper.getNFCTag() != null

    private val nfcTagListener = object : OnNFCTagListener.Stub() {
        override fun onTimeout() {
            lock.withLock {
                if (!active) return
                Timber.d("NFC Tag Emulation: Timeout — re-arming")
                rearmLocked()
            }
        }

        override fun onBeingRead() {
            lock.withLock {
                if (!active) return
                Timber.d("NFC Tag Emulation: Being read by external device — re-arming")
                onBeingReadCallback?.invoke()
                rearmLocked()
            }
        }
    }

    /**
     * Enables NFC tag emulation with the given NDEF payload.
     *
     * @param ndefBytes The raw NDEF message bytes to advertise.
     * @param timeoutSeconds How long to keep emulating before auto-timeout (re-arms automatically).
     * @param onBeingRead Optional callback invoked when an external device reads the tag.
     */
    fun enable(ndefBytes: ByteArray, timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS, onBeingRead: (() -> Unit)? = null) = lock.withLock {
        currentNdefBytes = ndefBytes
        currentTimeoutSeconds = timeoutSeconds
        onBeingReadCallback = onBeingRead
        active = true

        val rfReader = UsdkServiceHelper.getRFReader()
        val nfcTag = UsdkServiceHelper.getNFCTag()
        if (rfReader == null || nfcTag == null) {
            Timber.w("NFC Tag Emulation: Module not available — running on non-Ingenico device?")
            PosLogStore.error("NFC module not available", source = "NfcTagEmulator")
            return
        }

        try {
            // Stop any existing listener before starting a new one to prevent stale accumulation
            try { nfcTag.stopEventListener() } catch (_: Exception) { /* may not be running */ }

            // 1. Start event listener for onBeingRead / onTimeout callbacks
            nfcTag.startEventListener(nfcTagListener)

            val out = BytesValue()

            // 2. Enable card emulation mode
            var ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuSwitch, byteArrayOf(1), out)
            if (ret != RFError.SUCCESS) {
                Timber.e("NFC Tag Emulation: SetCardEmuSwitch(1) failed — error %d", ret)
                PosLogStore.error("SetCardEmuSwitch(1) failed — error $ret", source = "NfcTagEmulator")
                return
            }

            // 3. Set timeout
            ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuTimeout, byteArrayOf(timeoutSeconds.toByte()), out)
            if (ret != RFError.SUCCESS) {
                Timber.e("NFC Tag Emulation: SetCardEmuTimeout(%d) failed — error %d", timeoutSeconds, ret)
                PosLogStore.error("SetCardEmuTimeout($timeoutSeconds) failed — error $ret", source = "NfcTagEmulator")
                return
            }

            // 4. Set NDEF data
            ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuInfo, ndefBytes, out)
            if (ret != RFError.SUCCESS) {
                Timber.e("NFC Tag Emulation: SetCardEmuInfo failed — error %d", ret)
                PosLogStore.error("SetCardEmuInfo failed — error $ret", source = "NfcTagEmulator")
                return
            }

            scheduleWatchdog()

            Timber.d("NFC Tag Emulation: Enabled (timeout=%ds, payload=%d bytes, hex=%s)",
                timeoutSeconds, ndefBytes.size, ndefBytes.toHexString())
        } catch (e: Exception) {
            Timber.e(e, "NFC Tag Emulation: Failed to enable")
            PosLogStore.error("NFC enable failed: ${e.message}", source = "NfcTagEmulator")
        }
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }

    /**
     * Disables NFC tag emulation.
     */
    fun disable() = lock.withLock {
        active = false
        currentNdefBytes = null
        onBeingReadCallback = null
        handler.removeCallbacks(watchdogRunnable)

        val rfReader = UsdkServiceHelper.getRFReader()
        val nfcTag = UsdkServiceHelper.getNFCTag()
        if (rfReader == null || nfcTag == null) {
            Timber.d("NFC Tag Emulation: Module not available — nothing to disable")
            return
        }

        try {
            val out = BytesValue()
            val ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuSwitch, byteArrayOf(0), out)
            if (ret != RFError.SUCCESS) {
                Timber.w("NFC Tag Emulation: SetCardEmuSwitch(0) failed — error %d", ret)
            }
            nfcTag.stopEventListener()
            Timber.d("NFC Tag Emulation: Disabled")
        } catch (e: Exception) {
            Timber.e(e, "NFC Tag Emulation: Failed to disable")
        }
    }

    /**
     * Updates the NDEF payload while emulation is active.
     * If not currently active, stores the data for the next [enable] call.
     */
    fun updateNdefData(ndefBytes: ByteArray) {
        currentNdefBytes = ndefBytes
        if (!active) return

        val rfReader = UsdkServiceHelper.getRFReader() ?: run {
            Timber.w("NFC Tag Emulation: RF reader not available — cannot update")
            return
        }

        try {
            val out = BytesValue()
            val ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuInfo, ndefBytes, out)
            if (ret != RFError.SUCCESS) {
                Timber.w("NFC Tag Emulation: SetCardEmuInfo update failed — error %d", ret)
                return
            }
            Timber.d("NFC Tag Emulation: Updated payload (%d bytes)", ndefBytes.size)
        } catch (e: Exception) {
            Timber.e(e, "NFC Tag Emulation: Failed to update NDEF data")
        }
    }

    /**
     * Re-arms emulation after a timeout or read. Uses the last known NDEF payload.
     * Re-registers the event listener to ensure callbacks keep working across cycles.
     *
     * Must be called while [lock] is held.
     */
    private fun rearmLocked() {
        if (!active) return
        val bytes = currentNdefBytes ?: return

        val rfReader = UsdkServiceHelper.getRFReader() ?: return
        val nfcTag = UsdkServiceHelper.getNFCTag()
        try {
            // Stop then re-register event listener to prevent stale binder accumulation
            try { nfcTag?.stopEventListener() } catch (_: Exception) { /* may already be stopped */ }
            nfcTag?.startEventListener(nfcTagListener)

            val out = BytesValue()

            var ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuSwitch, byteArrayOf(1), out)
            if (ret != RFError.SUCCESS) {
                Timber.w("NFC Tag Emulation: Re-arm SetCardEmuSwitch failed — error %d", ret)
                return
            }

            ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuTimeout, byteArrayOf(currentTimeoutSeconds.toByte()), out)
            if (ret != RFError.SUCCESS) {
                Timber.w("NFC Tag Emulation: Re-arm SetCardEmuTimeout failed — error %d", ret)
                return
            }

            ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuInfo, bytes, out)
            if (ret != RFError.SUCCESS) {
                Timber.w("NFC Tag Emulation: Re-arm SetCardEmuInfo failed — error %d", ret)
                return
            }

            scheduleWatchdog()

            Timber.d("NFC Tag Emulation: Re-armed (timeout=%ds)", currentTimeoutSeconds)
        } catch (e: Exception) {
            Timber.e(e, "NFC Tag Emulation: Failed to re-arm")
            PosLogStore.error("NFC re-arm failed: ${e.message}", source = "NfcTagEmulator")
        }
    }

    /**
     * Schedules a watchdog timer that re-arms emulation if the USDK callback
     * is missed (e.g. AIDL listener binder went stale).
     */
    private fun scheduleWatchdog() {
        handler.removeCallbacks(watchdogRunnable)
        val delayMs = currentTimeoutSeconds * 1_000L + WATCHDOG_PADDING_MS
        handler.postDelayed(watchdogRunnable, delayMs)
    }
}
