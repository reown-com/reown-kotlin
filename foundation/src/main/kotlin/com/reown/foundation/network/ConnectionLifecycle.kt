package com.reown.foundation.network

import kotlinx.coroutines.flow.StateFlow

interface ConnectionLifecycle {
    val onResume: StateFlow<Boolean?>
    fun reconnect()
}