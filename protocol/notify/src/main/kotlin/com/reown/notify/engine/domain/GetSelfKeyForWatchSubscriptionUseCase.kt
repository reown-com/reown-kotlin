@file:JvmSynthetic

package com.reown.notify.engine.domain

import com.reown.android.internal.common.crypto.kmr.KeyManagementRepository
import com.reown.android.internal.common.model.AccountId
import com.reown.android.internal.utils.getPeerTag
import com.reown.foundation.common.model.Topic

internal class GetSelfKeyForWatchSubscriptionUseCase(
    private val keyManagementRepository: KeyManagementRepository,
) {
    suspend operator fun invoke(requestTopic: Topic, accountId: AccountId) = runCatching {
        keyManagementRepository.getPublicKey(Pair(accountId, requestTopic).getPeerTag())
    }.getOrElse {
        keyManagementRepository.generateAndStoreX25519KeyPair().also { pubKey ->
            keyManagementRepository.setKey(pubKey, Pair(accountId, requestTopic).getPeerTag())
        }
    }
}
