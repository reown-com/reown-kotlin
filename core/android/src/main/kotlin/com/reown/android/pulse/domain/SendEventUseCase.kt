package com.reown.android.pulse.domain

import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.wcKoinApp
import com.reown.android.internal.utils.currentTimeInSeconds
import com.reown.android.pulse.data.PulseService
import com.reown.android.pulse.model.Event
import com.reown.android.pulse.model.SDKType
import com.reown.android.pulse.model.properties.Props
import com.reown.foundation.util.Logger
import com.walletconnect.util.generateId
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.core.qualifier.named

class SendEventUseCase(
    private val pulseService: PulseService,
    private val logger: Logger,
    private val bundleId: String,
) : SendEventInterface {
    private val enableW3MAnalytics: Boolean by lazy { wcKoinApp.koin.get(named(AndroidCommonDITags.ENABLE_WEB_3_MODAL_ANALYTICS)) }

    override fun send(props: Props, sdkType: SDKType, timestamp: Long?, id: Long?) {
        if (enableW3MAnalytics) {
            scope.launch {
                supervisorScope {
                    try {
                        val event = Event(props = props, bundleId = bundleId, timestamp = timestamp ?: currentTimeInSeconds, eventId = id ?: generateId())
                        val response = pulseService.sendEvent(body = event, sdkType = sdkType.type)
                        if (!response.isSuccessful) {
                            logger.error("Failed to send event: ${event.props.type}")
                        } else {
                            logger.log("Event sent successfully: ${event.props.type}")
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to send event: ${props.type}, error: $e")
                    }
                }
            }
        }
    }
}

interface SendEventInterface {
    fun send(props: Props, sdkType: SDKType = SDKType.APPKIT, timestamp: Long? = currentTimeInSeconds, id: Long? = generateId())
}