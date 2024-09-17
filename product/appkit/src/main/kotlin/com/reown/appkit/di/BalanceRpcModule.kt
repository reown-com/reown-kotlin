package com.reown.appkit.di

import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.appkit.data.BalanceRpcRepository
import com.reown.appkit.data.network.BalanceService
import com.reown.appkit.domain.usecase.GetEthBalanceUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

internal fun balanceRpcModule() = module {

    single(named(AppKitDITags.BALANCE_RPC_RETROFIT)) {
        Retrofit.Builder()
            // Passing url to google to passing retrofit verification. The correct url to chain RPC is provided on the BalanceService::class
            .baseUrl("https://google.com/")
            .client(get(named(AndroidCommonDITags.OK_HTTP)))
            .addConverterFactory(MoshiConverterFactory.create(get(named(AndroidCommonDITags.MOSHI))))
            .build()
    }

    single { get<Retrofit>(named(AppKitDITags.BALANCE_RPC_RETROFIT)).create(BalanceService::class.java) }

    single { BalanceRpcRepository(balanceService = get(), logger = get()) }

    single { GetEthBalanceUseCase(balanceRpcRepository = get()) }
}