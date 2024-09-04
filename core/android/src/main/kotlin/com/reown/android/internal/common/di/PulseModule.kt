package com.reown.android.internal.common.di

import com.squareup.moshi.Moshi
import com.reown.android.internal.common.model.TelemetryEnabled
import com.reown.android.pulse.data.PulseService
import com.reown.android.pulse.domain.InsertEventUseCase
import com.reown.android.pulse.domain.InsertTelemetryEventUseCase
import com.reown.android.pulse.domain.SendBatchEventUseCase
import com.reown.android.pulse.domain.SendEventInterface
import com.reown.android.pulse.domain.SendEventUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@JvmSynthetic
fun pulseModule(bundleId: String) = module {
    single(named(AndroidCommonDITags.PULSE_URL)) { "https://pulse.walletconnect.org" }

    single(named(AndroidCommonDITags.PULSE_RETROFIT)) {
        Retrofit.Builder()
            .baseUrl(get<String>(named(AndroidCommonDITags.PULSE_URL)))
            .client(get(named(AndroidCommonDITags.APPKIT_OKHTTP)))
            .addConverterFactory(MoshiConverterFactory.create(get<Moshi.Builder>(named(AndroidCommonDITags.MOSHI)).build()))
            .build()
    }

    single {
        get<Retrofit>(named(AndroidCommonDITags.PULSE_RETROFIT)).create(PulseService::class.java)
    }

    single<SendEventInterface> {
        SendEventUseCase(
            pulseService = get(),
            logger = get(named(AndroidCommonDITags.LOGGER)),
            bundleId = bundleId
        )
    }

    single {
        SendBatchEventUseCase(
            pulseService = get(),
            logger = get(named(AndroidCommonDITags.LOGGER)),
            telemetryEnabled = get<TelemetryEnabled>(named(AndroidCommonDITags.TELEMETRY_ENABLED)),
            eventsRepository = get(),
        )
    }

    single {
        InsertTelemetryEventUseCase(
            logger = get(named(AndroidCommonDITags.LOGGER)),
            eventsRepository = get(),
        )
    }

    single {
        InsertEventUseCase(
            logger = get(named(AndroidCommonDITags.LOGGER)),
            eventsRepository = get(),
        )
    }
}