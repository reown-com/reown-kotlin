package com.reown.foundation.network

import com.reown.foundation.common.model.Topic
import com.reown.foundation.network.model.Relay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface RelayInterface {
    var isLoggingEnabled: Boolean
    val eventsFlow: SharedFlow<Relay.Model.Event>
    val subscriptionRequest: Flow<Relay.Model.Call.Subscription.Request>

    fun proposeSession(
        pairingTopic: Topic,
        sessionProposal: String,
        correlationId: Long,
        id: Long? = null,
        onResult: (Result<Relay.Model.Call.ProposeSession.Acknowledgement>) -> Unit
    )

    fun approveSession(
        pairingTopic: Topic,
        sessionTopic: Topic,
        sessionProposalResponse: String,
        sessionSettlementRequest: String,
        approvedChains: List<String>? = null,
        approvedMethods: List<String>? = null,
        approvedEvents: List<String>? = null,
        sessionProperties: Map<String, String>? = null,
        scopedProperties: Map<String, String>? = null,
        correlationId: Long,
        id: Long? = null,
        onResult: (Result<Relay.Model.Call.ApproveSession.Acknowledgement>) -> Unit,
    )

    fun publish(
        topic: String,
        message: String,
        params: Relay.Model.IrnParams,
        id: Long? = null,
        onResult: (Result<Relay.Model.Call.Publish.Acknowledgement>) -> Unit = {},
    )

    fun subscribe(topic: String, id: Long? = null, onResult: (Result<Relay.Model.Call.Subscribe.Acknowledgement>) -> Unit)

    fun batchSubscribe(topics: List<String>, id: Long? = null, onResult: (Result<Relay.Model.Call.BatchSubscribe.Acknowledgement>) -> Unit)

    fun unsubscribe(
        topic: String,
        subscriptionId: String,
        id: Long? = null,
        onResult: (Result<Relay.Model.Call.Unsubscribe.Acknowledgement>) -> Unit,
    )
}
