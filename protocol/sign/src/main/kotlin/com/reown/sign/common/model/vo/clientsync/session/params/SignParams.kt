@file:JvmSynthetic

package com.reown.sign.common.model.vo.clientsync.session.params

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.reown.android.internal.common.model.Namespace
import com.reown.android.internal.common.model.RelayProtocolOptions
import com.reown.android.internal.common.model.SessionProposer
import com.reown.android.internal.common.model.params.CoreSignParams
import com.reown.sign.common.model.vo.clientsync.common.PayloadParams
import com.reown.sign.common.model.vo.clientsync.common.ProposalRequests
import com.reown.sign.common.model.vo.clientsync.common.Requester
import com.reown.sign.common.model.vo.clientsync.common.SessionParticipant
import com.reown.sign.common.model.vo.clientsync.session.payload.SessionEventVO
import com.reown.sign.common.model.vo.clientsync.session.payload.SessionRequestVO
import com.reown.utils.DefaultId

internal sealed class SignParams : CoreSignParams() {

    @JsonClass(generateAdapter = true)
    internal data class SessionProposeParams(
        @param:Json(name = "requiredNamespaces")
        val requiredNamespaces: Map<String, Namespace.Proposal>,
        @param:Json(name = "optionalNamespaces")
        val optionalNamespaces: Map<String, Namespace.Proposal>?,
        @param:Json(name = "relays")
        val relays: List<RelayProtocolOptions>,
        @param:Json(name = "proposer")
        val proposer: SessionProposer,
        @param:Json(name = "sessionProperties")
        val properties: Map<String, String>?,
        @param:Json(name = "scopedProperties")
        val scopedProperties: Map<String, String>?,
        @param:Json(name = "expiryTimestamp")
        val expiryTimestamp: Long?,
        @param:Json(name = "requests")
        val requests: ProposalRequests?,
    ) : SignParams()

    @JsonClass(generateAdapter = true)
    internal data class SessionAuthenticateParams(
        @param:Json(name = "requester")
        val requester: Requester,
        @param:Json(name = "authPayload")
        val authPayload: PayloadParams,
        @param:Json(name = "expiryTimestamp")
        val expiryTimestamp: Long
    ) : SignParams() {
        val metadataUrl = requester.metadata.url
        val appLink = requester.metadata.redirect?.universal
        val linkMode = requester.metadata.redirect?.linkMode
    }

    @JsonClass(generateAdapter = true)
    internal data class SessionSettleParams(
        @param:Json(name = "relay")
        val relay: RelayProtocolOptions,
        @param:Json(name = "controller")
        val controller: SessionParticipant,
        @param:Json(name = "namespaces")
        val namespaces: Map<String, Namespace.Session>,
        @param:Json(name = "expiry")
        val expiry: Long,
        @param:Json(name = "sessionProperties")
        val properties: Map<String, String>?,
        @param:Json(name = "scopedProperties")
        val scopedProperties: Map<String, String>?,
    ) : SignParams()

    @JsonClass(generateAdapter = true)
    internal data class SessionRequestParams(
        @param:Json(name = "request")
        val request: SessionRequestVO,
        @param:Json(name = "chainId")
        val chainId: String,
    ) : SignParams() {
        val expiry = request.expiryTimestamp
        val rpcMethod = request.method
        val rpcParams = request.params
    }

    @JsonClass(generateAdapter = true)
    internal data class EventParams(
        @param:Json(name = "event")
        val event: SessionEventVO,
        @param:Json(name = "chainId")
        val chainId: String,
    ) : SignParams()

    @JsonClass(generateAdapter = true)
    internal class UpdateNamespacesParams(
        @param:Json(name = "namespaces")
        val namespaces: Map<String, Namespace.Session>,
    ) : SignParams()

    @JsonClass(generateAdapter = true)
    internal data class ExtendParams(
        @param:Json(name = "expiry")
        val expiry: Long,
    ) : SignParams()

    @JsonClass(generateAdapter = true)
    internal class DeleteParams(
        @param:Json(name = "code")
        val code: Int = Int.DefaultId,
        @param:Json(name = "message")
        val message: String,
    ) : SignParams()

    @Suppress("CanSealedSubClassBeObject")
    internal class PingParams : SignParams()
}