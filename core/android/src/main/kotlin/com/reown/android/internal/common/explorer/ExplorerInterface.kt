package com.reown.android.internal.common.explorer

import com.reown.android.internal.common.explorer.data.model.Project

interface ExplorerInterface {
    suspend fun getProjects(page: Int, entries: Int, isVerified: Boolean, isFeatured: Boolean): Result<List<Project>>
}