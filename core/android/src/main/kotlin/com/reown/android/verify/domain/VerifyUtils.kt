package com.reown.android.verify.domain

import com.reown.android.internal.common.model.Validation
import com.walletconnect.utils.compareDomains

fun getValidation(metadataUrl: String, origin: String) = if (compareDomains(metadataUrl, origin)) Validation.VALID else Validation.INVALID