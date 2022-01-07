package com.walletconnect.walletconnectv2.storage.sequence.vo

import com.walletconnect.walletconnectv2.common.model.vo.ExpiryVO
import com.walletconnect.walletconnectv2.common.model.vo.TopicVO
import com.walletconnect.walletconnectv2.common.model.vo.TtlVO
import com.walletconnect.walletconnectv2.storage.sequence.SequenceStatus

data class SessionVO(
    val topic: TopicVO,
    val chains: List<String>,
    val methods: List<String>,
    val types: List<String>,
    val ttl: TtlVO,
    val accounts: List<String>,
    val expiry: ExpiryVO,
    val status: SequenceStatus,
    val appMetaData: AppMetaDataVO?
)