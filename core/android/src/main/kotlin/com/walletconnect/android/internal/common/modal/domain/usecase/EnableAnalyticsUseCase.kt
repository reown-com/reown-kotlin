package com.walletconnect.android.internal.common.modal.domain.usecase

import com.walletconnect.android.internal.common.modal.AppKitApiRepository
import kotlinx.coroutines.runBlocking

interface EnableAnalyticsUseCaseInterface {
    fun fetchAnalyticsConfig(): Boolean
}

internal class EnableAnalyticsUseCase(private val repository: AppKitApiRepository) : EnableAnalyticsUseCaseInterface {
    override fun fetchAnalyticsConfig(): Boolean {
        return runBlocking {
            try {
                val response = repository.getAnalyticsConfig()
                if (response.isSuccess) {
                    response.getOrDefault(false)
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}