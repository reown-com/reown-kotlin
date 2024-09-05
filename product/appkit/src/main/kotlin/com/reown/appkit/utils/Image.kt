package com.reown.appkit.utils

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import coil.request.ImageRequest
import com.reown.android.BuildConfig
import com.reown.android.internal.common.model.ProjectId
import com.reown.android.internal.common.wcKoinApp

internal fun ImageRequest.Builder.imageHeaders() = apply {
    addHeader("x-project-id", wcKoinApp.koin.get<ProjectId>().value)
    addHeader("x-sdk-version", BuildConfig.SDK_VERSION)
    addHeader("x-sdk-type", "w3m")
}

internal val grayColorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
