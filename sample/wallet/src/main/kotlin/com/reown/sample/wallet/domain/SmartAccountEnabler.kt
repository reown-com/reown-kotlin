package com.reown.sample.wallet.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SmartAccountEnabler {
    private var _isSmartAccountEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isSmartAccountEnabled = _isSmartAccountEnabled.asStateFlow()

    fun enableSmartAccount(isEnabled: Boolean) {
        _isSmartAccountEnabled.value = isEnabled
    }
}
