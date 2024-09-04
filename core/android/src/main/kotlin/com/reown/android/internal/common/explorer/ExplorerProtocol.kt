package com.reown.android.internal.common.explorer

import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.explorer.data.model.Project
import com.reown.android.internal.common.explorer.domain.usecase.GetProjectsWithPaginationUseCase
import com.reown.android.internal.common.wcKoinApp
import com.reown.foundation.util.Logger
import org.koin.core.KoinApplication
import org.koin.core.qualifier.named


//discuss: Opening more endpoints to SDK consumers
class ExplorerProtocol(
    private val koinApp: KoinApplication = wcKoinApp,
) : ExplorerInterface {
    private val getProjectsWithPaginationUseCase: GetProjectsWithPaginationUseCase by lazy { koinApp.koin.get() }
    private val logger: Logger by lazy { koinApp.koin.get(named(AndroidCommonDITags.LOGGER)) }

    override suspend fun getProjects(page: Int, entries: Int, isVerified: Boolean, isFeatured: Boolean): Result<List<Project>> = getProjectsWithPaginationUseCase(page, entries, isVerified, isFeatured)
}


