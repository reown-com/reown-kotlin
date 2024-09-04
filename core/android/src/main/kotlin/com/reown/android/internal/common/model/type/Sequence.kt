package com.reown.android.internal.common.model.type

import com.reown.android.internal.common.model.Expiry
import com.reown.foundation.common.model.Topic

interface Sequence {
    val topic: Topic
    val expiry: Expiry
}