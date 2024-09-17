@file:JvmSynthetic

package com.reown.notify.common.model

import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.type.EngineEvent

internal data class Notification(
    val id: String,
    val topic: String,
    val sentAt: Long,
    val notificationMessage: NotificationMessage,
    val metadata: AppMetaData?,
) : EngineEvent