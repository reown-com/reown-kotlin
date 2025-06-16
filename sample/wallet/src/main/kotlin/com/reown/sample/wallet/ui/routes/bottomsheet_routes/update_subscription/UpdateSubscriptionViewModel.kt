package com.reown.sample.wallet.ui.routes.bottomsheet_routes.update_subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reown.notify.client.Notify
import com.reown.notify.client.NotifyClient
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.NotifyDelegate
import com.reown.sample.wallet.ui.common.subscriptions.ActiveSubscriptionsUI
import com.reown.sample.wallet.ui.common.subscriptions.toUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@Suppress("UNCHECKED_CAST")
class UpdateSubscriptionViewModelFactory(private val topic: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UpdateSubscriptionViewModel(topic) as T
    }
}

@OptIn(FlowPreview::class)
class UpdateSubscriptionViewModel(val topic: String) : ViewModel() {
    private val _activeSubscriptions = NotifyDelegate.notifyEvents
        .filterIsInstance<Notify.Event.SubscriptionsChanged>()
        .debounce(500L)
        .map { event -> event.subscriptions }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), NotifyClient.getActiveSubscriptions(Notify.Params.GetActiveSubscriptions(
            EthAccountDelegate.ethAccount)).values.toList())

    private val currentSubscription: Notify.Model.Subscription =
        _activeSubscriptions.value.firstOrNull { it.topic == topic } ?: throw IllegalStateException("No subscription found for topic $topic")

    val activeSubscriptionUI: MutableStateFlow<ActiveSubscriptionsUI> = MutableStateFlow(currentSubscription.toUI())


    private val _initialNotificationTypes = currentSubscription.scope.map { (id, setting) -> id.value to Triple(setting.name, setting.description, setting.enabled) }.toMap()
    private val _notificationTypes = MutableStateFlow(_initialNotificationTypes)
    val notificationTypes = _notificationTypes.asStateFlow()

    private val _state = MutableStateFlow<UpdateSubscriptionState>(UpdateSubscriptionState.Displaying)
    val state = _state.asStateFlow()

    val isUpdateEnabled = _notificationTypes.map { currentNotificationTypes ->
        (currentNotificationTypes != _initialNotificationTypes)
    }.combine(_state) { isUpdateEnabled, state ->
        isUpdateEnabled && state is UpdateSubscriptionState.Displaying
    }

    fun updateNotificationType(id: String, value: Triple<String, String, Boolean>) {
        val types = _notificationTypes.value.toMutableMap()
        types[id] = value
        _notificationTypes.update { types }
    }

    fun updateSubscription(onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = UpdateSubscriptionState.Updating
            NotifyClient.updateSubscription(Notify.Params.UpdateSubscription(
                topic, _notificationTypes.value.filter { (_, value) -> value.third }.map { (name, _) -> name }, 15.seconds
            )
            ).let { result ->
                when (result) {
                    is Notify.Result.UpdateSubscription.Success -> onSuccess()
                    is Notify.Result.UpdateSubscription.Error -> {
                        Timber.e(result.error.throwable)
                        onFailure(result.error.throwable)
                    }
                }
                _state.value = UpdateSubscriptionState.Displaying
            }
        }
    }
}


sealed interface UpdateSubscriptionState {
    object Updating : UpdateSubscriptionState
    object Displaying : UpdateSubscriptionState
}