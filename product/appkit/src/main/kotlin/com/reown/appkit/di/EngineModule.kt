package com.reown.appkit.di

import android.content.Context
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.appkit.domain.usecase.ConnectionEventRepository
import com.reown.appkit.engine.AppKitEngine
import com.reown.appkit.engine.coinbase.CoinbaseClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal fun engineModule() = module {

    single {
        ConnectionEventRepository(sharedPreferences = androidContext().getSharedPreferences("ConnectionEvents", Context.MODE_PRIVATE))
    }

    single {
        AppKitEngine(
            getSessionUseCase = get(),
            getSelectedChainUseCase = get(),
            deleteSessionDataUseCase = get(),
            saveSessionUseCase = get(),
            connectionEventRepository = get(),
            enableAnalyticsUseCase = get(),
            sendEventUseCase = get(),
            logger = get(named(AndroidCommonDITags.LOGGER)),
        )
    }
    single {
        CoinbaseClient(
            context = get(),
            appMetaData = get()
        )
    }
}
