package com.reown.notify.common.model

import com.reown.android.internal.common.model.type.EngineEvent

internal data class SubscriptionChanged(
    val subscriptions: List<Subscription.Active>,
) : EngineEvent
