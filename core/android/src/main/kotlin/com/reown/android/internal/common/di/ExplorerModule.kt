@file:JvmSynthetic

package com.reown.android.internal.common.di

import com.reown.android.internal.common.explorer.ExplorerRepository
import com.reown.android.internal.common.explorer.data.network.ExplorerService
import com.reown.android.internal.common.explorer.domain.usecase.GetNotifyConfigUseCase
import com.reown.android.internal.common.explorer.domain.usecase.GetProjectsWithPaginationUseCase
import com.reown.android.internal.common.model.ProjectId
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@JvmSynthetic
fun explorerModule(projectId: String) = module {

    single(named(AndroidCommonDITags.EXPLORER_URL)) { "https://registry.walletconnect.org/" }

    single(named(AndroidCommonDITags.EXPLORER_RETROFIT)) {
        Retrofit.Builder()
            .baseUrl(get<String>(named(AndroidCommonDITags.EXPLORER_URL)))
            .client(get(named(AndroidCommonDITags.OK_HTTP)))
            .addConverterFactory(MoshiConverterFactory.create(get(named(AndroidCommonDITags.MOSHI))))
            .build()
    }

    single { get<Retrofit>(named(AndroidCommonDITags.EXPLORER_RETROFIT)).create(ExplorerService::class.java) }

    single {
        ExplorerRepository(
            explorerService = get(),
            projectId = ProjectId(projectId),
        )
    }

    single { GetProjectsWithPaginationUseCase(get()) }
    single { GetNotifyConfigUseCase(get()) }
}