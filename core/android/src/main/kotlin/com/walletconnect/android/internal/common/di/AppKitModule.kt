package com.walletconnect.android.internal.common.di

import com.walletconnect.android.BuildConfig
import com.walletconnect.android.internal.common.modal.AppKitApiRepository
import com.walletconnect.android.internal.common.modal.data.network.AppKitService
import com.walletconnect.android.internal.common.modal.domain.usecase.EnableAnalyticsUseCase
import com.walletconnect.android.internal.common.modal.domain.usecase.EnableAnalyticsUseCaseInterface
import com.walletconnect.android.internal.common.modal.domain.usecase.GetInstalledWalletsIdsUseCase
import com.walletconnect.android.internal.common.modal.domain.usecase.GetInstalledWalletsIdsUseCaseInterface
import com.walletconnect.android.internal.common.modal.domain.usecase.GetSampleWalletsUseCase
import com.walletconnect.android.internal.common.modal.domain.usecase.GetSampleWalletsUseCaseInterface
import com.walletconnect.android.internal.common.modal.domain.usecase.GetWalletsUseCase
import com.walletconnect.android.internal.common.modal.domain.usecase.GetWalletsUseCaseInterface
import com.walletconnect.android.internal.common.model.ProjectId
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@JvmSynthetic
internal fun appKitModule() = module {
    single(named(AndroidCommonDITags.WEB3MODAL_URL)) { "https://api.web3modal.com/" }

    single(named(AndroidCommonDITags.APPKIT_INTERCEPTOR)) {
        Interceptor { chain ->
            val updatedRequest = chain.request().newBuilder()
                .addHeader("x-project-id", get<ProjectId>().value)
                .addHeader("x-sdk-version", "kotlin-${BuildConfig.SDK_VERSION}")
                .build()
            chain.proceed(updatedRequest)
        }
    }

    single(named(AndroidCommonDITags.APPKIT_OKHTTP)) {
        get<OkHttpClient>(named(AndroidCommonDITags.OK_HTTP))
            .newBuilder()
            .addInterceptor(get<Interceptor>(named(AndroidCommonDITags.APPKIT_INTERCEPTOR)))
            .build()
    }

    single(named(AndroidCommonDITags.APPKIT_RETROFIT)) {
        Retrofit.Builder()
            .baseUrl(get<String>(named(AndroidCommonDITags.WEB3MODAL_URL)))
            .client(get(named(AndroidCommonDITags.APPKIT_OKHTTP)))
            .addConverterFactory(MoshiConverterFactory.create(get(named(AndroidCommonDITags.MOSHI))))
            .build()
    }

    single { get<Retrofit>(named(AndroidCommonDITags.APPKIT_RETROFIT)).create(AppKitService::class.java) }

    single {
        AppKitApiRepository(
            web3ModalApiUrl = get(named(AndroidCommonDITags.WEB3MODAL_URL)),
            appKitService = get(),
            context = androidContext()
        )
    }

    single<GetInstalledWalletsIdsUseCaseInterface> { GetInstalledWalletsIdsUseCase(appKitApiRepository = get()) }
    single<GetWalletsUseCaseInterface> { GetWalletsUseCase(appKitApiRepository = get()) }
    single<GetSampleWalletsUseCaseInterface> { GetSampleWalletsUseCase(context = get()) }
    single<EnableAnalyticsUseCaseInterface> { EnableAnalyticsUseCase(repository = get()) }
}
