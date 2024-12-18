package com.reown.walletkit.di

import com.reown.android.internal.common.model.ProjectId
import com.reown.walletkit.use_cases.PrepareChainAbstractionUseCase
import com.reown.walletkit.use_cases.EstimateGasUseCase
import com.reown.walletkit.use_cases.ChainAbstractionStatusUseCase
import com.reown.walletkit.use_cases.GetERC20TokenBalanceUseCase
import com.reown.walletkit.use_cases.GetTransactionDetailsUseCase
import org.koin.dsl.module
import uniffi.uniffi_yttrium.ChainAbstractionClient

@JvmSynthetic
internal fun walletKitModule() = module {
    single { ChainAbstractionClient(get<ProjectId>().value) }

    single { PrepareChainAbstractionUseCase(get()) }

    single { ChainAbstractionStatusUseCase(get()) }

    single { EstimateGasUseCase(get()) }

    single { GetTransactionDetailsUseCase(get()) }

    single { GetERC20TokenBalanceUseCase(get()) }
}