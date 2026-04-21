@file:JvmSynthetic

package com.walletconnect.sample.pos.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class LogLevel { INFO, ERROR }

data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val source: String? = null,
    val message: String,
    val data: String? = null
)

internal object PosLogStore {

    private const val MAX_ENTRIES = 200

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun info(message: String, source: String? = null, data: String? = null) {
        add(LogLevel.INFO, message, source, data)
    }

    fun error(message: String, source: String? = null, data: String? = null) {
        add(LogLevel.ERROR, message, source, data)
    }

    fun clear() {
        _logs.value = emptyList()
    }

    private fun add(level: LogLevel, message: String, source: String?, data: String?) {
        _logs.update { current ->
            val entry = LogEntry(level = level, source = source, message = message, data = data)
            (listOf(entry) + current).take(MAX_ENTRIES)
        }
    }
}
