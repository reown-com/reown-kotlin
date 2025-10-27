package com.reown.pos.client.use_case

import org.koin.dsl.module

internal fun createPOSModule() = module {
    single { CheckTransactionStatusUseCase(get()) }
    single { BuildTransactionUseCase(get()) }
    single { GetSupportedNetworksUseCase(get()) }
}