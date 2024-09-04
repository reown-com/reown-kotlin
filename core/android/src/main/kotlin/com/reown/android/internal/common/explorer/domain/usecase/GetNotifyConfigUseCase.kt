package com.reown.android.internal.common.explorer.domain.usecase

import com.reown.android.internal.common.explorer.ExplorerRepository
import com.reown.android.internal.common.explorer.data.model.NotifyConfig

class GetNotifyConfigUseCase(private val explorerRepository: ExplorerRepository) {
    suspend operator fun invoke(appDomain: String): Result<NotifyConfig> = runCatching { explorerRepository.getNotifyConfig(appDomain) }
}
