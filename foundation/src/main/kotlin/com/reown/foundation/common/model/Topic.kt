package com.reown.foundation.common.model

import com.squareup.moshi.JsonClass
import com.reown.util.Empty

@JsonClass(generateAdapter = false)
data class Topic(val value: String = String.Empty)