package com.reown.android.internal.common.di

import com.reown.android.verify.data.VerifyService
import com.reown.android.verify.domain.JWTRepository
import com.reown.android.verify.domain.ResolveAttestationIdUseCase
import com.reown.android.verify.domain.VerifyPublicKeyStorageRepository
import com.reown.android.verify.domain.VerifyRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@JvmSynthetic
internal fun verifyModule() = module {
    factory(named(AndroidCommonDITags.VERIFY_URL)) { "https://verify.walletconnect.org/" }

    single(named(AndroidCommonDITags.VERIFY_RETROFIT)) {
        Retrofit.Builder()
            .baseUrl(get<String>(named(AndroidCommonDITags.VERIFY_URL)))
            .client(get(named(AndroidCommonDITags.OK_HTTP)))
            .addConverterFactory(MoshiConverterFactory.create(get(named(AndroidCommonDITags.MOSHI))))
            .build()
    }

    single { get<Retrofit>(named(AndroidCommonDITags.VERIFY_RETROFIT)).create(VerifyService::class.java) }

    single { ResolveAttestationIdUseCase(get(), get(), get(named(AndroidCommonDITags.VERIFY_URL))) }

    single { VerifyPublicKeyStorageRepository(get()) }

    single { JWTRepository() }

    single {
        VerifyRepository(
            verifyService = get(),
            jwtRepository = get(),
            moshi = get(named(AndroidCommonDITags.MOSHI)),
            verifyPublicKeyStorageRepository = get()
        )
    }
}