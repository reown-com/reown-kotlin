@file:JvmSynthetic

package com.walletconnect.notify.common.model

import com.reown.android.internal.common.model.AccountId
import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.RelayProtocolOptions
import com.reown.foundation.common.model.PublicKey
import com.reown.foundation.common.model.Topic

internal sealed class Subscription {
    abstract val account: AccountId
    abstract val mapOfScope: Map<String, Scope.Cached>
    abstract val expiry: Expiry

    data class Active(
        override val account: AccountId,
        override val mapOfScope: Map<String, Scope.Cached>,
        override val expiry: Expiry,
        val authenticationPublicKey: PublicKey,
        val topic: Topic,
        val dappMetaData: AppMetaData? = null,
        val requestedSubscriptionId: Long? = null,
        val relay: RelayProtocolOptions = RelayProtocolOptions(),
        val lastNotificationId: String? = null,
        val reachedEndOfHistory: Boolean = false,
    ) : Subscription()
}