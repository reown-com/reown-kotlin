@file:JvmName("Expiration")

package com.reown.android.internal.utils

val PROPOSAL_EXPIRY: Long get() = currentTimeInSeconds + fiveMinutesInSeconds
val ACTIVE_SESSION: Long get() = currentTimeInSeconds + weekInSeconds