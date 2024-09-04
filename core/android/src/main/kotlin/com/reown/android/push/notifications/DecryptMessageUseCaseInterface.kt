package com.reown.android.push.notifications

import com.reown.android.Core

interface DecryptMessageUseCaseInterface {
    suspend fun decryptNotification(topic: String, message: String, onSuccess: (Core.Model.Message) -> Unit, onFailure: (Throwable) -> Unit)
}