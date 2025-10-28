@file:JvmSynthetic

package com.reown.sign.engine.model.mapper

import com.reown.android.internal.common.JsonRpcResponse
import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.Namespace
import com.reown.android.internal.common.model.RelayProtocolOptions
import com.reown.android.internal.common.model.SessionProposer
import com.reown.android.internal.common.model.TransportType
import com.reown.android.internal.common.model.WCRequest
import com.reown.android.internal.common.model.params.CoreSignParams
import com.reown.android.internal.common.signing.cacao.Cacao
import com.reown.android.internal.common.signing.cacao.CacaoType
import com.reown.android.internal.common.signing.cacao.Issuer
import com.reown.android.internal.common.signing.cacao.toCAIP222Message
import com.reown.android.verify.model.VerifyContext
import com.reown.foundation.common.model.PublicKey
import com.reown.foundation.common.model.Topic
import com.reown.sign.common.exceptions.PeerError
import com.reown.sign.common.model.Request
import com.reown.sign.common.model.vo.clientsync.common.PayloadParams
import com.reown.sign.common.model.vo.clientsync.common.ProposalRequests
import com.reown.sign.common.model.vo.clientsync.common.ProposalRequestsResponses
import com.reown.sign.common.model.vo.clientsync.common.Requester
import com.reown.sign.common.model.vo.clientsync.session.params.SignParams
import com.reown.sign.common.model.vo.proposal.ProposalVO
import com.reown.sign.common.model.vo.sequence.SessionVO
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.engine.model.ValidationError
import com.reown.sign.json_rpc.model.JsonRpcMethod
import com.reown.util.Empty
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Calendar

@JvmSynthetic
internal fun EngineDO.WalletConnectUri.toAbsoluteString(): String =
    "wc:${topic.value}@$version?${getQuery()}&symKey=${symKey.keyAsHex}"

private fun EngineDO.WalletConnectUri.getQuery(): String {
    var query = "relay-protocol=${relay.protocol}"
    if (relay.data != null) {
        query = "$query&relay-data=${relay.data}"
    }
    return query
}

@JvmSynthetic
internal fun SignParams.SessionProposeParams.toEngineDO(topic: Topic): EngineDO.SessionProposal =
    EngineDO.SessionProposal(
        pairingTopic = topic.value,
        name = proposer.metadata.name,
        description = proposer.metadata.description,
        url = proposer.metadata.url,
        icons = proposer.metadata.icons.mapNotNull { convertToURI(it) },
        redirect = proposer.metadata.redirect?.native ?: String.Empty,
        requiredNamespaces = requiredNamespaces.toMapOfEngineNamespacesRequired(),
        optionalNamespaces = optionalNamespaces?.toMapOfEngineNamespacesOptional() ?: emptyMap(),
        properties = properties,
        proposerPublicKey = proposer.publicKey,
        relayProtocol = relays.first().protocol,
        relayData = relays.first().data,
        scopedProperties = scopedProperties,
        requests = if (requests != null) {
            EngineDO.ProposalRequests(authentication = requests.authentication?.map { it.toEngineDO() })
        } else null
    )

@JvmSynthetic
internal fun SignParams.SessionProposeParams.toVO(topic: Topic, requestId: Long): ProposalVO =
    ProposalVO(
        requestId = requestId,
        pairingTopic = topic,
        name = proposer.metadata.name,
        description = proposer.metadata.description,
        url = proposer.metadata.url,
        icons = proposer.metadata.icons,
        redirect = proposer.metadata.redirect?.native ?: String.Empty,
        requiredNamespaces = requiredNamespaces,
        optionalNamespaces = optionalNamespaces ?: emptyMap(),
        properties = properties,
        proposerPublicKey = proposer.publicKey,
        relayProtocol = relays.first().protocol,
        relayData = relays.first().data,
        expiry = if (expiryTimestamp != null) Expiry(expiryTimestamp) else null,
        scopedProperties = scopedProperties,
        requests = ProposalRequests(authentication = requests?.authentication ?: emptyList())
    )

@JvmSynthetic
internal fun ProposalVO.toSessionProposeRequest(): WCRequest =
    WCRequest(
        topic = pairingTopic,
        id = requestId,
        method = JsonRpcMethod.WC_SESSION_PROPOSE,
        params = SignParams.SessionProposeParams(
            relays = listOf(RelayProtocolOptions(protocol = relayProtocol, data = relayData)),
            proposer = SessionProposer(proposerPublicKey, AppMetaData(name = name, description = description, url = url, icons = icons)),
            requiredNamespaces = requiredNamespaces,
            optionalNamespaces = optionalNamespaces,
            properties = properties,
            expiryTimestamp = expiry?.seconds,
            scopedProperties = scopedProperties,
            requests = requests

        ),
        transportType = TransportType.RELAY
    )

@JvmSynthetic
internal fun SignParams.SessionRequestParams.toEngineDO(
    request: WCRequest,
    peerAppMetaData: AppMetaData?,
): EngineDO.SessionRequest =
    EngineDO.SessionRequest(
        topic = request.topic.value,
        chainId = chainId,
        peerAppMetaData = peerAppMetaData,
        request = EngineDO.SessionRequest.JSONRPCRequest(
            id = request.id,
            method = this.request.method,
            params = this.request.params
        ),
        if (this.request.expiryTimestamp != null) Expiry(this.request.expiryTimestamp) else null
    )

@JvmSynthetic
internal fun SignParams.DeleteParams.toEngineDO(topic: Topic): EngineDO.SessionDelete =
    EngineDO.SessionDelete(topic.value, message)

@JvmSynthetic
internal fun SignParams.EventParams.toEngineDO(topic: Topic): EngineDO.SessionEvent =
    EngineDO.SessionEvent(topic.value, event.name, event.data.toString(), chainId)

@JvmSynthetic
internal fun SessionVO.toEngineDO(): EngineDO.Session =
    EngineDO.Session(
        topic,
        expiry,
        pairingTopic,
        requiredNamespaces.toMapOfEngineNamespacesRequired(),
        optionalNamespaces?.toMapOfEngineNamespacesOptional(),
        sessionNamespaces.toMapOfEngineNamespacesSession(),
        peerAppMetaData
    )

@JvmSynthetic
internal fun SessionVO.toEngineDOSessionExtend(expiryVO: Expiry): EngineDO.SessionExtend =
    EngineDO.SessionExtend(
        topic,
        expiryVO,
        pairingTopic,
        requiredNamespaces.toMapOfEngineNamespacesRequired(),
        optionalNamespaces?.toMapOfEngineNamespacesOptional(),
        sessionNamespaces.toMapOfEngineNamespacesSession(),
        selfAppMetaData
    )

@JvmSynthetic
internal fun SessionVO.toSessionApproved(proposalRequestsResponses: ProposalRequestsResponses?): EngineDO.SessionApproved =
    EngineDO.SessionApproved(
        topic = topic.value,
        peerAppMetaData = peerAppMetaData,
        accounts = sessionNamespaces.flatMap { (_, namespace) -> namespace.accounts },
        namespaces = sessionNamespaces.toMapOfEngineNamespacesSession(),
        proposalRequestsResponses = EngineDO.ProposalRequestsResponses(authentication = proposalRequestsResponses?.authentication)
    )

@JvmSynthetic
internal fun ProposalVO.toEngineDO(): EngineDO.SessionProposal =
    EngineDO.SessionProposal(
        pairingTopic = pairingTopic.value,
        name = name,
        description = description,
        url = url,
        icons = icons.mapNotNull { convertToURI(it) },
        redirect = redirect,
        relayData = relayData,
        relayProtocol = relayProtocol,
        requiredNamespaces = requiredNamespaces.toMapOfEngineNamespacesRequired(),
        optionalNamespaces = optionalNamespaces.toMapOfEngineNamespacesOptional(),
        proposerPublicKey = proposerPublicKey,
        properties = properties,
        scopedProperties = scopedProperties,
        requests = EngineDO.ProposalRequests(authentication = requests.authentication?.map { it.toEngineDO() })
    )

@JvmSynthetic
internal fun ProposalVO.toExpiredProposal(): EngineDO.ExpiredProposal = EngineDO.ExpiredProposal(pairingTopic.value, proposerPublicKey)

@JvmSynthetic
internal fun Request<String>.toExpiredSessionRequest() = EngineDO.ExpiredRequest(topic.value, id)

private fun convertToURI(it: String) = try {
    URI(it)
} catch (e: Exception) {
    null
}

@JvmSynthetic
internal fun Map<String, EngineDO.Namespace.Proposal>.toNamespacesVORequired(): Map<String, Namespace.Proposal> =
    this.mapValues { (_, namespace) ->
        Namespace.Proposal(chains = namespace.chains, methods = namespace.methods, events = namespace.events)
    }

@JvmSynthetic
internal fun Map<String, EngineDO.Namespace.Proposal>.toNamespacesVOOptional(): Map<String, Namespace.Proposal> =
    this.mapValues { (_, namespace) ->
        Namespace.Proposal(chains = namespace.chains, methods = namespace.methods, events = namespace.events)
    }

@JvmSynthetic
internal fun Map<String, Namespace.Proposal>.toMapOfEngineNamespacesRequired(): Map<String, EngineDO.Namespace.Proposal> =
    this.mapValues { (_, namespace) ->
        EngineDO.Namespace.Proposal(namespace.chains, namespace.methods, namespace.events)
    }

@JvmSynthetic
internal fun Map<String, Namespace.Proposal>.toMapOfEngineNamespacesOptional(): Map<String, EngineDO.Namespace.Proposal> =
    this.mapValues { (_, namespace) ->
        EngineDO.Namespace.Proposal(namespace.chains, namespace.methods, namespace.events)
    }

@JvmSynthetic
internal fun Map<String, Namespace.Session>.toMapOfEngineNamespacesSession(): Map<String, EngineDO.Namespace.Session> =
    this.mapValues { (_, namespaceVO) ->
        EngineDO.Namespace.Session(namespaceVO.chains, namespaceVO.accounts, namespaceVO.methods, namespaceVO.events)
    }

@JvmSynthetic
internal fun Map<String, EngineDO.Namespace.Session>.toMapOfNamespacesVOSession(): Map<String, Namespace.Session> =
    this.mapValues { (_, namespace) ->
        Namespace.Session(namespace.chains, namespace.accounts, namespace.methods, namespace.events)
    }

@JvmSynthetic
internal fun JsonRpcResponse.JsonRpcResult.toEngineDO(): EngineDO.JsonRpcResponse.JsonRpcResult =
    EngineDO.JsonRpcResponse.JsonRpcResult(id = id, result = result.toString())

@JvmSynthetic
internal fun JsonRpcResponse.JsonRpcError.toEngineDO(): EngineDO.JsonRpcResponse.JsonRpcError =
    EngineDO.JsonRpcResponse.JsonRpcError(id = id, error = EngineDO.JsonRpcResponse.Error(error.code, error.message))

@JvmSynthetic
internal fun ProposalVO.toSessionApproveParams(selfPublicKey: PublicKey): CoreSignParams.ApprovalParams =
    CoreSignParams.ApprovalParams(
        relay = RelayProtocolOptions(relayProtocol, relayData),
        responderPublicKey = selfPublicKey.keyAsHex
    )

@JvmSynthetic
internal fun SignParams.SessionRequestParams.toEngineDO(topic: Topic): EngineDO.Request =
    EngineDO.Request(topic.value, request.method, request.params, chainId)

@JvmSynthetic
internal fun SignParams.EventParams.toEngineDOEvent(): EngineDO.Event =
    EngineDO.Event(event.name, event.data.toString(), chainId)

@JvmSynthetic
internal fun Request<String>.toSessionRequest(peerAppMetaData: AppMetaData?): EngineDO.SessionRequest =
    EngineDO.SessionRequest(topic.value, chainId, peerAppMetaData, EngineDO.SessionRequest.JSONRPCRequest(id, method, params), expiry)


@JvmSynthetic
internal fun ValidationError.toPeerError() = when (this) {
    is ValidationError.UnsupportedNamespaceKey -> PeerError.CAIP25.UnsupportedNamespaceKey(message)
    is ValidationError.UnsupportedChains -> PeerError.CAIP25.UnsupportedChains(message)
    is ValidationError.InvalidEvent -> PeerError.Invalid.Event(message)
    is ValidationError.InvalidExtendRequest -> PeerError.Invalid.ExtendRequest(message)
    is ValidationError.InvalidSessionRequest -> PeerError.Invalid.Method(message)
    is ValidationError.UnauthorizedEvent -> PeerError.Unauthorized.Event(message)
    is ValidationError.UnauthorizedMethod -> PeerError.Unauthorized.Method(message)
    is ValidationError.UserRejected -> PeerError.CAIP25.UserRejected(message)
    is ValidationError.UserRejectedEvents -> PeerError.CAIP25.UserRejectedEvents(message)
    is ValidationError.UserRejectedMethods -> PeerError.CAIP25.UserRejectedMethods(message)
    is ValidationError.UserRejectedChains -> PeerError.CAIP25.UserRejectedChains(message)
    is ValidationError.InvalidSessionProperties -> PeerError.CAIP25.InvalidSessionPropertiesObject(message)
    is ValidationError.EmptyNamespaces -> PeerError.CAIP25.EmptySessionNamespaces(message)
}

@JvmSynthetic
internal fun VerifyContext.toEngineDO(): EngineDO.VerifyContext =
    EngineDO.VerifyContext(id, origin, validation, verifyUrl, isScam)

@JvmSynthetic
internal fun Requester.toEngineDO(): EngineDO.Participant =
    EngineDO.Participant(publicKey, metadata)

@JvmSynthetic
internal fun EngineDO.Authenticate.toCommon(): PayloadParams =
    PayloadParams(
        domain = domain,
        aud = aud,
        nonce = nonce,
        nbf = nbf,
        exp = exp,
        statement = statement,
        requestId = requestId,
        resources = resources,
        chains = chains,
        type = type ?: CacaoType.CAIP222.header,
        version = "1",
        iat = SimpleDateFormat(Cacao.Payload.ISO_8601_PATTERN).format(Calendar.getInstance().time)
    )

@JvmSynthetic
internal fun PayloadParams.toEngineDO(): EngineDO.PayloadParams =
    EngineDO.PayloadParams(
        domain = domain,
        aud = aud,
        version = "1",
        nonce = nonce,
        nbf = nbf,
        exp = exp,
        statement = statement,
        requestId = requestId,
        resources = resources,
        chains = chains,
        type = type,
        iat = iat,
        signatureTypes = signatureTypes
    )

@JvmSynthetic
internal fun EngineDO.PayloadParams.toCacaoPayload(iss: Issuer): Cacao.Payload =
    Cacao.Payload(
        iss.value,
        domain = domain,
        aud = aud,
        version = version,
        nonce = nonce,
        nbf = nbf,
        exp = exp,
        iat = iat,
        statement = statement,
        requestId = requestId,
        resources = resources
    )

@JvmSynthetic
internal fun EngineDO.PayloadParams.toCAIP222Message(iss: Issuer, chainName: String): String =
    this.toCacaoPayload(iss).toCAIP222Message(chainName)