package com.walletconnect.notify.common.model

import com.reown.android.internal.common.signing.cacao.Cacao
import com.reown.foundation.common.model.PrivateKey

internal data class CacaoPayloadWithIdentityPrivateKey(val payload: Cacao.Payload, val key: PrivateKey)
