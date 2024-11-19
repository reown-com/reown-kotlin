package com.reown.walletkit.di

import com.reown.android.internal.common.model.ProjectId
import com.reown.walletkit.use_cases.CanFulfilUseCase
import com.reown.walletkit.use_cases.EstimateGasUseCase
import com.reown.walletkit.use_cases.FulfilmentStatusUseCase
import org.koin.dsl.module
import uniffi.uniffi_yttrium.ChainAbstractionClient

@JvmSynthetic
internal fun walletKitModule() = module {
    single { ChainAbstractionClient(get<ProjectId>().value) }

    single { CanFulfilUseCase(get()) }

    single { FulfilmentStatusUseCase(get()) }

    single { EstimateGasUseCase(get()) }
}