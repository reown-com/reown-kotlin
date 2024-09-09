package com.reown.sign.engine

import com.reown.sign.engine.model.EngineDO
import java.util.concurrent.ConcurrentLinkedQueue

internal val sessionRequestEventsQueue = ConcurrentLinkedQueue<EngineDO.SessionRequestEvent>()