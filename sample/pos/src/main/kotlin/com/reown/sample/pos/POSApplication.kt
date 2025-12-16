package com.reown.sample.pos

import android.app.Application
import com.walletconnect.pos.PosClient
import timber.log.Timber

class POSApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging (if available)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize the lightweight POS SDK
        val deviceId = "sample_pos_device_${android.os.Build.MODEL}_${android.os.Build.SERIAL}"

        PosClient.init(
            apiKey = BuildConfig.MERCHANT_API_KEY,
            merchantId = "wc_merchant_id_test_1",
            deviceId = deviceId,
        )

        // Set the delegate to receive payment events
        PosClient.setDelegate(PosSampleDelegate)

        Timber.d("POSClient initialized successfully")
    }
}
