@file:JvmSynthetic

package com.reown.notify.engine.calls

import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.AppMetaDataType
import com.reown.android.internal.common.model.IrnParams
import com.reown.android.internal.common.model.Tags
import com.reown.android.internal.common.model.params.CoreNotifyParams
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.android.internal.utils.thirtySeconds
import com.reown.foundation.common.model.Topic
import com.reown.foundation.common.model.Ttl
import com.reown.notify.common.model.NotifyRpc
import com.reown.notify.common.model.TimeoutInfo
import com.reown.notify.common.model.UpdateSubscription
import com.reown.notify.data.storage.SubscriptionRepository
import com.reown.notify.engine.blockingCallsDefaultTimeout
import com.reown.notify.engine.blockingCallsDelayInterval
import com.reown.notify.engine.domain.FetchDidJwtInteractor
import com.reown.notify.engine.responses.OnUpdateResponseUseCase
import com.reown.notify.engine.validateTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

internal class UpdateSubscriptionUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val subscriptionRepository: SubscriptionRepository,
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
    private val fetchDidJwtInteractor: FetchDidJwtInteractor,
    private val onUpdateResponseUseCase: OnUpdateResponseUseCase,
) : UpdateSubscriptionUseCaseInterface {

    override suspend fun update(topic: String, scopes: List<String>, timeout: Duration?): UpdateSubscription = supervisorScope {
        val result = MutableStateFlow<UpdateSubscription>(UpdateSubscription.Processing)
        var timeoutInfo: TimeoutInfo = TimeoutInfo.Nothing
        try {
            val validTimeout = timeout.validateTimeout()

            val subscription = subscriptionRepository.getActiveSubscriptionByNotifyTopic(topic)
                ?: throw IllegalStateException("No subscription found for topic $topic")
            val metadata: AppMetaData = metadataStorageRepository.getByTopicAndType(subscription.topic, AppMetaDataType.PEER)
                ?: throw IllegalStateException("No metadata found for topic $topic")
            val didJwt = fetchDidJwtInteractor.updateRequest(subscription.account, metadata.url, subscription.authenticationPublicKey, scopes).getOrThrow()

            val params = CoreNotifyParams.UpdateParams(didJwt.value)
            val request = NotifyRpc.NotifyUpdate(params = params)
            val irnParams = IrnParams(Tags.NOTIFY_UPDATE, Ttl(thirtySeconds))
            timeoutInfo = TimeoutInfo.Data(request.id, Topic(topic))

            jsonRpcInteractor.publishJsonRpcRequest(Topic(topic), irnParams, request, onFailure = { error -> result.value = UpdateSubscription.Error(error) })

            onUpdateResponseUseCase.events
                .filter { it.first == params }
                .map { it.second }
                .filter { it is UpdateSubscription.Success || it is UpdateSubscription.Error }
                .onEach { result.emit(it as UpdateSubscription) }
                .launchIn(scope)

            withTimeout(validTimeout) {
                while (result.value == UpdateSubscription.Processing) {
                    delay(blockingCallsDelayInterval)
                }
            }

            return@supervisorScope result.value
        } catch (e: TimeoutCancellationException) {
            with(timeoutInfo as TimeoutInfo.Data) {
                return@supervisorScope UpdateSubscription.Error(Throwable("Request: $requestId timed out after ${timeout ?: blockingCallsDefaultTimeout}"))
            }
        } catch (e: Exception) {
            return@supervisorScope UpdateSubscription.Error(e)
        }
    }
}

internal interface UpdateSubscriptionUseCaseInterface {
    suspend fun update(topic: String, scopes: List<String>, timeout: Duration? = null): UpdateSubscription
}