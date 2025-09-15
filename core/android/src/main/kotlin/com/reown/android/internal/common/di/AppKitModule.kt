package com.reown.android.internal.common.di

import android.os.Build
import com.reown.android.BuildConfig
import com.reown.android.internal.common.modal.AppKitApiRepository
import com.reown.android.internal.common.modal.data.network.AppKitService
import com.reown.android.internal.common.modal.domain.usecase.EnableAnalyticsUseCase
import com.reown.android.internal.common.modal.domain.usecase.EnableAnalyticsUseCaseInterface
import com.reown.android.internal.common.modal.domain.usecase.GetInstalledWalletsIdsUseCase
import com.reown.android.internal.common.modal.domain.usecase.GetInstalledWalletsIdsUseCaseInterface
import com.reown.android.internal.common.modal.domain.usecase.GetSampleWalletsUseCase
import com.reown.android.internal.common.modal.domain.usecase.GetSampleWalletsUseCaseInterface
import com.reown.android.internal.common.modal.domain.usecase.GetWalletsUseCase
import com.reown.android.internal.common.modal.domain.usecase.GetWalletsUseCaseInterface
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

@JvmSynthetic
 fun coreAppKitModule(projectId: String) = module {

    factory(named(AndroidCommonDITags.USER_AGENT)) {
        """wc-2/reown-kotlin-${BuildConfig.SDK_VERSION}/android-${Build.VERSION.RELEASE}"""
    }

    single(named(AndroidCommonDITags.SHARED_INTERCEPTOR)) {
        Interceptor { chain ->
            val updatedRequest = chain.request().newBuilder()
                .addHeader("User-Agent", get(named(AndroidCommonDITags.USER_AGENT)))
                .build()

            chain.proceed(updatedRequest)
        }
    }

    single<Interceptor>(named(AndroidCommonDITags.LOGGING_INTERCEPTOR)) {
        HttpLoggingInterceptor().apply { setLevel(HttpLoggingInterceptor.Level.BODY) }
    }

    single(named(AndroidCommonDITags.OK_HTTP)) {
        OkHttpClient.Builder()
            .addInterceptor(get<Interceptor>(named(AndroidCommonDITags.SHARED_INTERCEPTOR)))
//            .authenticator((get(named(AndroidCommonDITags.AUTHENTICATOR))))
            .writeTimeout(15_000L, TimeUnit.MILLISECONDS)
            .readTimeout(15_000L, TimeUnit.MILLISECONDS)
            .callTimeout(15_000L, TimeUnit.MILLISECONDS)
            .connectTimeout(15_000L, TimeUnit.MILLISECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    val loggingInterceptor = get<Interceptor>(named(AndroidCommonDITags.LOGGING_INTERCEPTOR))
                    addInterceptor(loggingInterceptor)
                }
            }
            .retryOnConnectionFailure(true)
            .build()
    }

    single {
        KotlinJsonAdapterFactory()
    }

    single<Moshi.Builder>(named(AndroidCommonDITags.MOSHI)) {
        Moshi.Builder()
            .addLast(get<KotlinJsonAdapterFactory>())
    }

    single(named(AndroidCommonDITags.WEB3MODAL_URL)) { "https://api.web3modal.com/" }

    single(named(AndroidCommonDITags.APPKIT_INTERCEPTOR)) {
        Interceptor { chain ->
            val updatedRequest = chain.request().newBuilder()
                .addHeader("x-project-id", projectId)
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
    single<GetSampleWalletsUseCaseInterface> { GetSampleWalletsUseCase(context = androidContext()) }
    single<EnableAnalyticsUseCaseInterface> { EnableAnalyticsUseCase(repository = get()) }
}
