package com.walletconnect.sample.pos

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder
import com.walletconnect.pos.PosClient
import timber.log.Timber

class POSApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging (if available)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize the lightweight POS SDK
        val deviceId = "sample_pos_device_${Build.MODEL}_${Build.SERIAL}"

        PosClient.init(
            apiKey = BuildConfig.MERCHANT_API_KEY,
            merchantId = "merchant-1768556907",
            deviceId = deviceId,
        )

        // Set the delegate to receive payment events
        PosClient.setDelegate(PosSampleDelegate)

        Timber.d("POSClient initialized successfully")
    }

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }
}
