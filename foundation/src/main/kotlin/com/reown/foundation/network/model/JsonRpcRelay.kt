@file:JvmSynthetic

package com.reown.foundation.network.model

private const val SUFFIX = "irn"
internal const val IRN_PUBLISH: String = "${SUFFIX}_publish"
internal const val IRN_SUBSCRIPTION: String = "${SUFFIX}_subscription"
internal const val IRN_SUBSCRIBE: String = "${SUFFIX}_subscribe"
internal const val IRN_BATCH_SUBSCRIBE: String = "${SUFFIX}_batchSubscribe"
internal const val IRN_UNSUBSCRIBE: String = "${SUFFIX}_unsubscribe"

internal const val WC_PROPOSE_SESSION: String = "wc_proposeSession"
internal const val WC_APPROVE_SESSION: String = "wc_approveSession"