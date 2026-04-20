package com.walletconnect.sample.pos

import android.app.Application

import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder
import com.walletconnect.pos.Pos
import com.walletconnect.pos.PosClient
import com.walletconnect.sample.pos.credentials.MerchantCredentialsManager
import com.walletconnect.sample.pos.log.PosLogStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.Executors

class POSApplication : Application(), SingletonImageLoader.Factory {

    companion object {
        var initError: String? = null
            private set

        private val _initCompleted = MutableStateFlow(false)
        val initCompleted = _initCompleted.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        initSdk()
    }

    private fun initSdk() {
        val credentialsManager = MerchantCredentialsManager(this)
        val deviceId = credentialsManager.getDeviceId()
        Executors.newSingleThreadExecutor().execute {
            try {
                PosClient.init(
                    apiKey = credentialsManager.getApiKey(),
                    merchantId = credentialsManager.getMerchantId(),
                    deviceId = deviceId,
                    mtlsConfig = Pos.MtlsConfig.Disabled
                )
                PosClient.setDelegate(PosSampleDelegate)
                Timber.d("POSClient initialized successfully")
                PosLogStore.info("POS SDK initialized", source = "POSApplication")
            } catch (e: IllegalStateException) {
                initError = e.message ?: "Unknown initialization error"
                Timber.e(e, "POSClient initialization failed")
                PosLogStore.error("POS SDK init failed: ${e.message}", source = "POSApplication")
            } finally {
                _initCompleted.value = true
            }
        }
    }

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }
}
