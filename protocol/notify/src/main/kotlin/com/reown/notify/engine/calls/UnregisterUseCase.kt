@file:JvmSynthetic

package com.reown.notify.engine.calls

import com.reown.android.internal.common.model.AccountId
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.keyserver.domain.IdentitiesInteractor
import com.reown.foundation.common.model.Topic
import com.reown.notify.data.storage.NotificationsRepository
import com.reown.notify.data.storage.RegisteredAccountsRepository
import com.reown.notify.data.storage.SubscriptionRepository
import com.reown.notify.engine.domain.StopWatchingSubscriptionsUseCase
import kotlinx.coroutines.supervisorScope

internal class UnregisterUseCase(
    private val identitiesInteractor: IdentitiesInteractor,
    private val keyserverUrl: String,
    private val registeredAccountsRepository: RegisteredAccountsRepository,
    private val stopWatchingSubscriptionsUseCase: StopWatchingSubscriptionsUseCase,
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val subscriptionRepository: SubscriptionRepository,
    private val notificationsRepository: NotificationsRepository,
) : UnregisterUseCaseInterface {

    override suspend fun unregister(
        account: String,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) = supervisorScope {
        val accountId = AccountId(account)
        identitiesInteractor.unregisterIdentity(accountId, keyserverUrl).fold(
            onFailure = { error -> onFailure(error) },
            onSuccess = { identityPublicKey ->
                runCatching {
                    stopWatchingSubscriptionsUseCase(accountId, onFailure = { error -> onFailure(error) })
                    registeredAccountsRepository.deleteAccountByAccountId(account)
                }.fold(
                    onFailure = { error -> onFailure(error) },
                    onSuccess = {
                        subscriptionRepository.getAccountActiveSubscriptions(accountId).map { it.topic.value }.map { topic ->
                            jsonRpcInteractor.unsubscribe(Topic(topic)) { error -> onFailure(error) }
                            subscriptionRepository.deleteSubscriptionByNotifyTopic(topic)
                            notificationsRepository.deleteNotificationsByTopic(topic)
                        }
                        onSuccess(identityPublicKey.keyAsHex)
                    }
                )
            }
        )
    }
}

internal interface UnregisterUseCaseInterface {
    suspend fun unregister(
        account: String,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit,
    )
}