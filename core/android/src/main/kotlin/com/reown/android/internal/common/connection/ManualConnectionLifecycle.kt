@file:JvmSynthetic

package com.reown.android.internal.common.connection

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.reown.foundation.network.ConnectionLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ManualConnectionLifecycle(
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(),
) : Lifecycle by lifecycleRegistry, ConnectionLifecycle {

    private val _onResume = MutableStateFlow<Boolean?>(null)
    override val onResume: StateFlow<Boolean?> = _onResume.asStateFlow()

    fun connect() {
        lifecycleRegistry.onNext(Lifecycle.State.Started)
        _onResume.value = true
    }

    fun disconnect() {
        lifecycleRegistry.onNext(Lifecycle.State.Stopped.WithReason())
        _onResume.value = false
    }
    override fun reconnect() {
        disconnect()
        connect()
    }
}