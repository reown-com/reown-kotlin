package com.reown.sample.wallet

import android.annotation.SuppressLint
import com.google.firebase.messaging.RemoteMessage
import com.reown.android.Core
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.wcKoinApp
import com.reown.android.push.notifications.PushMessagingService
import com.reown.foundation.util.Logger
import com.reown.sample.wallet.domain.NotificationHandler
import kotlinx.coroutines.runBlocking
import org.koin.core.qualifier.named

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class WalletKitMessageService : PushMessagingService() {
    private val logger: Logger by lazy { wcKoinApp.koin.get(named(AndroidCommonDITags.LOGGER)) }

    override fun onMessage(message: Core.Model.Message, originalMessage: RemoteMessage) {
        runBlocking { NotificationHandler.addNotification(message) }
    }

    override fun newToken(token: String) {
        logger.log("Registering New Token Success:\t$token")
    }

    override fun registeringFailed(token: String, throwable: Throwable) {
        logger.log("Registering New Token Failed:\t$token")
    }

    override fun onDefaultBehavior(message: RemoteMessage) {
        logger.log("onDefaultBehavior: ${message.to}")
    }

    override fun onError(throwable: Throwable, defaultMessage: RemoteMessage) {
        logger.error("onError Message To: ${defaultMessage.to}, throwable: $throwable")
    }
}