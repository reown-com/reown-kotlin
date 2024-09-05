package com.reown.appkit.domain.model

import com.reown.appkit.client.Modal

internal data class AccountData(
    val address: String,
    val chains: List<Modal.Model.Chain>,
    val identity: Identity? = null
)
