package com.reown.sample.wallet.domain

import com.reown.notify.client.Notify
import com.reown.notify.client.NotifyClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

object NotifyDelegate : NotifyClient.Delegate {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _notifyEvents = MutableSharedFlow<Notify.Event>()
    val notifyEvents = _notifyEvents.asSharedFlow()

    private val _notifyErrors = MutableSharedFlow<Notify.Model.Error>()
    val notifyErrors = _notifyErrors.asSharedFlow()

    init {
        NotifyClient.setDelegate(this)
    }

    override fun onNotifyNotification(notifyNotification: Notify.Event.Notification) {
        scope.launch {
            Timber.d("NotifyDelegate.onNotifyNotification - $notifyNotification")
            _notifyEvents.emit(notifyNotification)
        }
    }

    override fun onError(error: Notify.Model.Error) {
        scope.launch {
            Timber.d("NotifyDelegate.onError - $error")
            _notifyErrors.emit(error)
        }
    }

    override fun onSubscriptionsChanged(subscriptionsChanged: Notify.Event.SubscriptionsChanged) {
        scope.launch {
            Timber.d("NotifyDelegate.onSubscriptionsChanged - $subscriptionsChanged")
            _notifyEvents.emit(subscriptionsChanged)
        }
    }
}