package com.reown.appkit.di

import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.explorer.data.model.Project
import com.reown.android.internal.common.model.ProjectId
import com.reown.appkit.data.BlockchainRepository
import com.reown.appkit.data.network.BlockchainService
import com.reown.appkit.domain.usecase.GetIdentityUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

internal fun blockchainApiModule(projectId: String) = module {

    single(named(AppKitDITags.BLOCKCHAIN_RETROFIT)) {
        Retrofit.Builder()
            .baseUrl("https://rpc.walletconnect.org/v1/")
            .client(get(named(AndroidCommonDITags.OK_HTTP)))
            .addConverterFactory(MoshiConverterFactory.create(get(named(AndroidCommonDITags.MOSHI))))
            .build()
    }

    single {
        get<Retrofit>(named(AppKitDITags.BLOCKCHAIN_RETROFIT)).create(BlockchainService::class.java)
    }

    single {
        BlockchainRepository(blockchainService = get(), projectId = ProjectId(projectId))
    }

    single { GetIdentityUseCase(blockchainRepository = get()) }
}
