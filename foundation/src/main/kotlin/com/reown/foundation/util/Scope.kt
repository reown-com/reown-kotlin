package com.reown.foundation.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val job = SupervisorJob()
internal var scope = CoroutineScope(job + Dispatchers.IO)