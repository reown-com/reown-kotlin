package com.reown.notify.common.model

import com.reown.android.internal.common.model.AccountId
import com.reown.foundation.common.model.PublicKey
import com.reown.foundation.common.model.Topic

data class RegisteredAccount(
    val accountId: AccountId,
    val publicIdentityKey: PublicKey,
    val allApps: Boolean,
    val appDomain: String?,
    val notifyServerWatchTopic: Topic?,
    val notifyServerAuthenticationKey: PublicKey?,
)