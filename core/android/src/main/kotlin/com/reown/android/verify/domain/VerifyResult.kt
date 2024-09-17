package com.reown.android.verify.domain

import com.reown.android.internal.common.model.Validation

data class VerifyResult(
    val validation: Validation,
    val isScam: Boolean?,
    val origin: String
)