package com.reown.walletkit.di

import android.os.Build
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.model.ProjectId
import com.reown.walletkit.BuildConfig
import com.reown.walletkit.use_cases.PrepareChainAbstractionUseCase
import com.reown.walletkit.use_cases.EstimateGasUseCase
import com.reown.walletkit.use_cases.ChainAbstractionStatusUseCase
import com.reown.walletkit.use_cases.ExecuteChainAbstractionUseCase
import com.reown.walletkit.use_cases.GetERC20TokenBalanceUseCase
import com.reown.walletkit.use_cases.GetTransactionDetailsUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.PulseMetadata

@JvmSynthetic
internal fun walletKitModule() = module {
    single {
        ChainAbstractionClient(
            projectId = get<ProjectId>().value,
            pulseMetadata = PulseMetadata(
                packageName = get(named(AndroidCommonDITags.PACKAGE_NAME)),
                sdkPlatform = "android-${Build.VERSION.RELEASE}",
                sdkVersion = "reown-kotlin-${BuildConfig.SDK_VERSION}",
                bundleId = null,
                url = null
            )
        )
    }

    single { PrepareChainAbstractionUseCase(get()) }

    single { ChainAbstractionStatusUseCase(get()) }

    single { EstimateGasUseCase(get()) }

    single { GetTransactionDetailsUseCase(get()) }

    single { GetERC20TokenBalanceUseCase(get()) }

    single { ExecuteChainAbstractionUseCase(get()) }
}