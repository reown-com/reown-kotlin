@file:JvmSynthetic

package com.reown.notify.engine.calls

import com.reown.android.internal.common.model.AppMetaDataType
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.notify.common.model.Subscription
import com.reown.notify.data.storage.SubscriptionRepository

internal class GetAllActiveSubscriptionsUseCase(
    private val subscriptionRepository: SubscriptionRepository,
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
) {

    suspend operator fun invoke(): Map<String, Subscription.Active> =
        subscriptionRepository.getAllActiveSubscriptions()
            .map { subscription ->
                val metadata = metadataStorageRepository.getByTopicAndType(subscription.topic, AppMetaDataType.PEER)
                subscription.copy(dappMetaData = metadata)
            }
            .associateBy { subscription -> subscription.topic.value }
}
