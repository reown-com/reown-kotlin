@file:JvmName("Expiration")

package com.reown.android.pairing.model

import com.reown.android.internal.utils.currentTimeInSeconds
import com.reown.android.internal.utils.fiveMinutesInSeconds

val pairingExpiry: Long get() = currentTimeInSeconds + fiveMinutesInSeconds