package com.reown.android.verify.model

import com.reown.android.internal.common.model.Validation

data class VerifyContext(
    val id: Long,
    val origin: String,
    val validation: Validation,
    val verifyUrl: String,
    val isScam: Boolean?
)