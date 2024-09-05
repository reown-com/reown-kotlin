package com.reown.appkit.client.models

import com.reown.appkit.client.Modal

data class Account(
    val address: String,
    val chain: Modal.Model.Chain
)