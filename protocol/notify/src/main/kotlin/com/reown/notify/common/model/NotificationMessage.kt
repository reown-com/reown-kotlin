@file:JvmSynthetic

package com.reown.notify.common.model

internal data class NotificationMessage(
    val title: String,
    val body: String,
    val icon: String?,
    val url: String?,
    val type: String,
)