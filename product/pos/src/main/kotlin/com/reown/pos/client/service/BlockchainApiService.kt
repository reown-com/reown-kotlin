package com.reown.pos.client.service

import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.pos.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit


internal fun createBlockchainApiModule(projectId: String, deviceId: String) = module {
    single(named(AndroidCommonDITags.POC_OK_HTTP)) {
        OkHttpClient.Builder()
            .writeTimeout(timeout = 15_000, unit = TimeUnit.MILLISECONDS)
            .readTimeout(timeout = 15_000, unit = TimeUnit.MILLISECONDS)
            .callTimeout(timeout = 15_000, unit = TimeUnit.MILLISECONDS)
            .connectTimeout(timeout = 15_000, unit = TimeUnit.MILLISECONDS)
            .apply {
                val queryParameterInterceptor = Interceptor { chain ->
                    val originalRequest = chain.request()
                    val originalHttpUrl = originalRequest.url
                    val newUrl = originalHttpUrl.newBuilder()
                        .addQueryParameter("projectId", projectId)
                        .addQueryParameter("st", "mobile")
                        .addQueryParameter("sv", "pos-kotlin-${BuildConfig.SDK_VERSION}")
                        .addQueryParameter("deviceId", deviceId)
                        .build()

                    val newRequest = originalRequest.newBuilder()
                        .url(newUrl)
                        .build()

                    chain.proceed(newRequest)
                }

                addInterceptor(queryParameterInterceptor)
            }
            .build()
    }

    single(named(AndroidCommonDITags.POC_RETROFIT)) {
        Retrofit.Builder()
            .baseUrl("https://rpc.walletconnect.org")
            .client(get(named(AndroidCommonDITags.POC_OK_HTTP)))
            .addConverterFactory(MoshiConverterFactory.create(get(named(AndroidCommonDITags.MOSHI))))
            .build()
    }

    single { get<Retrofit>(named(AndroidCommonDITags.POC_RETROFIT)).create(BlockchainApi::class.java) }
}