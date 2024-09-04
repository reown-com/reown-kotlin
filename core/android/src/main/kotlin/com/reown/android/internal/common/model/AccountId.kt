@file:JvmSynthetic

package com.reown.android.internal.common.model

import com.reown.android.internal.utils.CoreValidator


@JvmInline
value class AccountId(val value: String) {
    fun isValid(): Boolean = CoreValidator.isAccountIdCAIP10Compliant(value)
    fun address() = value.split(":").last()
}