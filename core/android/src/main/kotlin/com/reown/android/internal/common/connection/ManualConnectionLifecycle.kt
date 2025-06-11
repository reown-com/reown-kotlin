@file:JvmSynthetic

package com.reown.android.internal.common.connection

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.reown.foundation.network.ConnectionLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ManualConnectionLifecycle(
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(),
) : Lifecycle by lifecycleRegistry, ConnectionLifecycle {
    private val job = SupervisorJob()
    private var scope = CoroutineScope(job + Dispatchers.Default)
    private val connectionMutex = Mutex() // For thread safety
    private val _onResume = MutableStateFlow<Boolean?>(null)
    override val onResume: StateFlow<Boolean?> = _onResume.asStateFlow()

    fun connect() {
        scope.launch {
            connectionMutex.withLock {
                _onResume.value = true
            }
        }
    }

    fun disconnect() {
        scope.launch {
            connectionMutex.withLock {
                _onResume.value = false
                lifecycleRegistry.onNext(Lifecycle.State.Stopped.WithReason())
                _onResume.value = false
            }
        }
    }

    override fun reconnect() {
        scope.launch {
            connectionMutex.withLock {
                lifecycleRegistry.onNext(Lifecycle.State.Stopped.WithReason())
                delay(100)
                lifecycleRegistry.onNext(Lifecycle.State.Started)
            }
        }
    }
}