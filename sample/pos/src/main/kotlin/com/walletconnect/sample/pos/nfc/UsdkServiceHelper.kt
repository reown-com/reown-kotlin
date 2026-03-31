@file:JvmSynthetic

package com.walletconnect.sample.pos.nfc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.usdk.apiservice.aidl.DeviceServiceData
import com.usdk.apiservice.aidl.ModuleName
import com.usdk.apiservice.aidl.UDeviceService
import com.walletconnect.sample.pos.log.PosLogStore
import com.usdk.apiservice.aidl.constants.RFDeviceName
import com.usdk.apiservice.aidl.rfreader.UNFCTag
import com.usdk.apiservice.aidl.rfreader.URFReader
import com.usdk.apiservice.aidl.vectorprinter.UVectorPrinter
import com.usdk.apiservice.limited.DeviceServiceLimited
import timber.log.Timber

/**
 * Singleton managing the AIDL connection to the Ingenico USDK `com.usdk.apiservice`.
 * Provides access to NFC hardware modules (RF reader and NFC tag emulation).
 */
internal object UsdkServiceHelper {

    private const val USDK_SERVICE_ACTION = "com.usdk.apiservice"
    private const val USDK_SERVICE_PACKAGE = "com.usdk.apiservice"
    private const val MAX_RETRIES = 3
    private const val RETRY_INTERVAL_MS = 3000L
    private const val RECONNECT_DELAY_MS = 2000L

    @Volatile
    private var deviceService: UDeviceService? = null

    @Volatile
    var isServiceBound: Boolean = false
        private set

    @Volatile
    var isRegistered: Boolean = false
        private set

    private var retryCount = 0
    @Volatile
    private var wasDisconnected = false
    private lateinit var appContext: Context
    private val handler = Handler(Looper.getMainLooper())

    /** Callback invoked when the USDK service reconnects after a disconnect. */
    var onServiceReconnected: (() -> Unit)? = null

    /** Binder token for USDK registration — must be kept alive to prevent GC deregistration. */
    private val binderToken = Binder()

    /** Detects USDK service process death and triggers reconnection. */
    private val deathRecipient = object : IBinder.DeathRecipient {
        override fun binderDied() {
            Timber.w("USDK: Binder death detected — service process crashed")
            PosLogStore.error("USDK service process crashed — reconnecting", source = "UsdkService")
            deviceService = null
            isServiceBound = false
            isRegistered = false
            wasDisconnected = true
            scheduleReconnect()
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val svc = UDeviceService.Stub.asInterface(service)
            deviceService = svc
            isServiceBound = true
            val isReconnect = wasDisconnected
            wasDisconnected = false
            retryCount = 0
            Timber.d("USDK: Service connected")
            PosLogStore.info(
                if (isReconnect) "USDK service reconnected" else "USDK service connected",
                source = "UsdkService"
            )

            // Monitor the binder for unexpected process death
            try {
                service?.linkToDeath(deathRecipient, 0)
            } catch (e: Exception) {
                Timber.w(e, "USDK: Failed to link death recipient")
            }

            enableDebugLog(svc)

            DeviceServiceLimited.bind(
                appContext,
                svc,
                object : DeviceServiceLimited.ServiceBindListener {
                    override fun onSuccess() {
                        Timber.d("USDK: DeviceServiceLimited bind success")
                        register(svc)
                        if (isReconnect) notifyReconnected()
                    }

                    override fun onFail() {
                        Timber.e("USDK: DeviceServiceLimited bind failed — registering anyway")
                        register(svc)
                        if (isReconnect) notifyReconnected()
                    }
                }
            )
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            deviceService = null
            isServiceBound = false
            isRegistered = false
            wasDisconnected = true
            Timber.w("USDK: Service disconnected — scheduling reconnect")
            PosLogStore.error("USDK service disconnected — scheduling reconnect", source = "UsdkService")
            scheduleReconnect()
        }
    }

    /**
     * Binds to the USDK service with retry logic (up to [MAX_RETRIES] attempts, [RETRY_INTERVAL_MS] apart).
     */
    fun bind(context: Context) {
        if (isServiceBound) {
            Timber.d("USDK: Service already bound")
            return
        }
        appContext = context.applicationContext
        retryCount = 0
        attemptBind(context)
    }

    private fun scheduleReconnect() {
        if (!::appContext.isInitialized) return
        handler.postDelayed({
            if (!isServiceBound) {
                Timber.d("USDK: Attempting reconnect")
                retryCount = 0
                attemptBind(appContext)
            }
        }, RECONNECT_DELAY_MS)
    }

    private fun notifyReconnected() {
        Timber.d("USDK: Service reconnected — notifying listeners")
        onServiceReconnected?.invoke()
    }

    private fun attemptBind(context: Context) {
        val intent = Intent(USDK_SERVICE_ACTION).apply {
            setPackage(USDK_SERVICE_PACKAGE)
        }
        try {
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (!bound) {
                retryCount++
                if (retryCount <= MAX_RETRIES) {
                    Timber.w("USDK: Bind failed, retrying (%d/%d) in %dms", retryCount, MAX_RETRIES, RETRY_INTERVAL_MS)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (!isServiceBound) attemptBind(context)
                    }, RETRY_INTERVAL_MS)
                } else {
                    Timber.e("USDK: Failed to bind after %d retries — USDK service not available", MAX_RETRIES)
                }
            }
        } catch (e: SecurityException) {
            Timber.e(e, "USDK: SecurityException binding to service")
        }
    }

    /**
     * Unbinds from the USDK service.
     */
    fun unbind(context: Context) {
        handler.removeCallbacksAndMessages(null)
        if (!isServiceBound) return
        if (isRegistered) {
            try {
                deviceService?.unregister(null)
                isRegistered = false
                Timber.d("USDK: Unregistered from device service")
            } catch (e: Exception) {
                Timber.w(e, "USDK: Error unregistering from device service")
            }
        }
        try {
            deviceService?.asBinder()?.unlinkToDeath(deathRecipient, 0)
        } catch (_: Exception) { /* already unlinked */ }
        try {
            DeviceServiceLimited.unbind(context)
        } catch (e: Exception) {
            Timber.w(e, "USDK: Error unbinding DeviceServiceLimited")
        }
        try {
            context.unbindService(serviceConnection)
        } catch (e: Exception) {
            Timber.w(e, "USDK: Error unbinding service")
        }
        deviceService = null
        isServiceBound = false
        onServiceReconnected = null
        Timber.d("USDK: Service unbound")
    }

    private fun enableDebugLog(svc: UDeviceService) {
        try {
            val logOption = Bundle().apply {
                putBoolean(DeviceServiceData.COMMON_LOG, true)
                putBoolean(DeviceServiceData.MASTERCONTROL_LOG, true)
            }
            svc.debugLog(logOption)
            Timber.d("USDK: Debug logging enabled")
        } catch (e: Exception) {
            Timber.w(e, "USDK: Failed to enable debug logging")
        }
    }

    private fun register(svc: UDeviceService) {
        try {
            val param = Bundle().apply {
                putBoolean(DeviceServiceData.USE_EPAY_MODULE, true)
            }
            svc.register(param, binderToken)
            isRegistered = true
            Timber.d("USDK: Registered with device service")

            // Release the RF hardware from reader mode immediately after registration.
            // The ePay module claims the RF card on register; closing it here frees the
            // hardware so NFC tag emulation can use it later without conflict.
            try {
                val bundle = Bundle().apply { putString("rfDeviceName", RFDeviceName.INNER) }
                URFReader.Stub.asInterface(svc.getRFReader(bundle))?.closeDevice()
                Timber.d("USDK: RF reader closed after registration")
            } catch (_: Exception) {
                // May fail if RF not open — safe to ignore.
            }
        } catch (e: Exception) {
            isRegistered = false
            Timber.e(e, "USDK: Failed to register — %s", e.message)
        }
    }

    /**
     * Returns the USDK RF reader for the inner NFC antenna, or null if the service is not bound.
     */
    fun getRFReader(): URFReader? {
        val service = deviceService ?: run {
            Timber.w("USDK: Service not bound — cannot get RF reader")
            return null
        }
        return try {
            val bundle = Bundle().apply { putString("rfDeviceName", RFDeviceName.INNER) }
            URFReader.Stub.asInterface(service.getRFReader(bundle))
        } catch (e: Exception) {
            Timber.e(e, "USDK: Failed to get RF reader")
            null
        }
    }

    /**
     * Returns the USDK vector printer, or null if not available.
     */
    fun getVectorPrinter(): UVectorPrinter? {
        val service = deviceService ?: run {
            Timber.w("USDK: Service not bound — cannot get vector printer")
            return null
        }
        return try {
            UVectorPrinter.Stub.asInterface(service.vectorPrinter)
        } catch (e: Exception) {
            Timber.e(e, "USDK: Failed to get vector printer")
            null
        }
    }

    /**
     * Returns the USDK NFC tag emulation module, or null if not available.
     */
    fun getNFCTag(): UNFCTag? {
        val service = deviceService ?: run {
            Timber.w("USDK: Service not bound — cannot get NFC tag module")
            return null
        }
        return try {
            UNFCTag.Stub.asInterface(
                service.getModule(ModuleName.NFC_TAG_EMULATION, null)
            )
        } catch (e: Exception) {
            Timber.e(e, "USDK: Failed to get NFC tag emulation module")
            null
        }
    }
}
