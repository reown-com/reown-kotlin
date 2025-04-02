@file:JvmSynthetic

package com.reown.sign.common.model.vo.clientsync.session.params

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.reown.android.internal.common.model.Namespace
import com.reown.android.internal.common.model.RelayProtocolOptions
import com.reown.android.internal.common.model.SessionProposer
import com.reown.android.internal.common.model.params.CoreSignParams
import com.reown.sign.common.model.vo.clientsync.common.PayloadParams
import com.reown.sign.common.model.vo.clientsync.common.Requester
import com.reown.sign.common.model.vo.clientsync.common.SessionParticipant
import com.reown.sign.common.model.vo.clientsync.session.payload.SessionEventVO
import com.reown.sign.common.model.vo.clientsync.session.payload.SessionRequestVO
import com.reown.utils.DefaultId

internal sealed class SignParams : CoreSignParams() {

    @JsonClass(generateAdapter = true)
    internal data class SessionProposeParams(
        @Json(name = "requiredNamespaces")
        val requiredNamespaces: Map<String, Namespace.Proposal>,
        @Json(name = "optionalNamespaces")
        val optionalNamespaces: Map<String, Namespace.Proposal>?,
        @Json(name = "relays")
        val relays: List<RelayProtocolOptions>,
        @Json(name = "proposer")
        val proposer: SessionProposer,
        @Json(name = "sessionProperties")
        val properties: Map<String, String>?,
        @Json(name = "scopedProperties")
        val scopedProperties: Map<String, String>?,
        @Json(name = "expiryTimestamp")
        val expiryTimestamp: Long?,
    ) : SignParams()

    @JsonClass(generateAdapter = true)
    internal data class SessionAuthenticateParams(
        @Json(name = "requester")
        val requester: Requester,
        @Json(name = "authPayload")
        val authPayload: PayloadParams,
        @Json(name = "expiryTimestamp")
        val expiryTimestamp: Long
    ) : SignParams() {
        val metadataUrl = requester.metadata.url
        val appLink = requester.metadata.redirect?.universal
        val linkMode = requester.metadata.redirect?.linkMode
    }

    @JsonClass(generateAdapter = true)
    internal data class SessionSettleParams(
        @Json(name = "relay")
        val relay: RelayProtocolOptions,
        @Json(name = "controller")
        val controller: SessionParticipant,
        @Json(name = "namespaces")
        val namespaces: Map<String, Namespace.Session>,
        @Json(name = "expiry")
        val expiry: Long,
        @Json(name = "sessionProperties")
        val properties: Map<String, String>?,
        @Json(name = "scopedProperties")
        val scopedProperties: Map<String, String>?,
    ) : SignParams()

    @JsonClass(generateAdapter = true)
    internal data class SessionRequestParams(
        @Json(name = "request")
        val request: SessionRequestVO,
        @Json(name = "chainId")
        val chainId: String,
    ) : SignParams() {
        val expiry = request.expiryTimestamp
        val rpcMethod = request.method
        val rpcParams = request.params
    }

    @JsonClass(generateAdapter = true)
    internal data class EventParams(
        @Json(name = "event")
        val event: SessionEventVO,
        @Json(name = "chainId")
        val chainId: String,
    ) : SignParams()

    @JsonClass(generateAdapter = true)
    internal class UpdateNamespacesParams(
        @Json(name = "namespaces")
        val namespaces: Map<String, Namespace.Session>,
    ) : SignParams()

    @JsonClass(generateAdapter = true)
    internal data class ExtendParams(
        @Json(name = "expiry")
        val expiry: Long,
    ) : SignParams()

    @JsonClass(generateAdapter = true)
    internal class DeleteParams(
        @Json(name = "code")
        val code: Int = Int.DefaultId,
        @Json(name = "message")
        val message: String,
    ) : SignParams()

    @Suppress("CanSealedSubClassBeObject")
    internal class PingParams : SignParams()
}