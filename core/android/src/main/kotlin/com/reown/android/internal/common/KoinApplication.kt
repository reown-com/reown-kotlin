package com.reown.android.internal.common

import org.koin.core.KoinApplication

var wcKoinApp: KoinApplication = KoinApplication.init().apply { createEagerInstances() }