package com.reown.android.internal.common.storage.pairing

import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.Pairing
import com.reown.foundation.common.model.Topic

interface PairingStorageRepositoryInterface {

    fun insertPairing(pairing: Pairing)

    fun deletePairing(topic: Topic)

    fun hasTopic(topic: Topic): Boolean

    suspend fun getListOfPairings(): List<Pairing>

    suspend fun getListOfPairingsWithoutRequestReceived(): List<Pairing>

    fun setRequestReceived(topic: Topic)

    fun getPairingOrNullByTopic(topic: Topic): Pairing?

    fun getPairingOrNullByTopicAndRpcId(topic: Topic, rpcId: Long): Pairing?
}