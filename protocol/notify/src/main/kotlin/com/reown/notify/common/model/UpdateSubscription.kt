@file:JvmSynthetic

package com.reown.notify.common.model

import com.reown.android.internal.common.model.type.EngineEvent

internal sealed class UpdateSubscription : EngineEvent {

    data class Success(val subscription: Subscription.Active) : UpdateSubscription()

    data class Error(val throwable: Throwable) : UpdateSubscription()

    object Processing : UpdateSubscription()
}