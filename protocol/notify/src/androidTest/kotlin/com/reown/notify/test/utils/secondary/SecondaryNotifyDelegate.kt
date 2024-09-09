package com.reown.notify.test.utils.secondary

import com.reown.notify.client.Notify
import com.reown.notify.client.NotifyInterface
import com.reown.notify.test.utils.globalOnError

open class SecondaryNotifyDelegate : NotifyInterface.Delegate {

    override fun onNotifyNotification(notifyNotification: Notify.Event.Notification) {
    }

    override fun onSubscriptionsChanged(subscriptionsChanged: Notify.Event.SubscriptionsChanged) {
    }

    override fun onError(error: Notify.Model.Error) {
        globalOnError(error)
    }
}