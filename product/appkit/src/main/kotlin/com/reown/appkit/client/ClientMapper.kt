package com.reown.appkit.client

import com.reown.android.internal.common.signing.cacao.Cacao
import com.reown.sign.client.Sign
import com.reown.appkit.client.models.Account
import com.reown.appkit.client.models.Session
import com.reown.appkit.client.models.request.Request
import java.text.SimpleDateFormat
import java.util.Calendar

internal fun Sign.Model.ApprovedSession.toModal() = Modal.Model.ApprovedSession.WalletConnectSession(topic, metaData, namespaces.toModal(), accounts)

internal fun Map<String, Sign.Model.Namespace.Session>.toModal() =
    mapValues { (_, namespace) -> Modal.Model.Namespace.Session(namespace.chains, namespace.accounts, namespace.methods, namespace.events) }

internal fun Sign.Model.RejectedSession.toModal() = Modal.Model.RejectedSession(topic, reason)

internal fun Sign.Model.UpdatedSession.toModal() = Modal.Model.UpdatedSession(topic, namespaces.toModal())

internal fun Sign.Model.SessionEvent.toModal() = Modal.Model.SessionEvent(name, data)

internal fun Sign.Model.Event.toModal() = Modal.Model.Event(topic, name, data, chainId)

internal fun Sign.Model.Session.toModal() = Modal.Model.Session(pairingTopic, topic, expiry, namespaces.toModal(), metaData)

internal fun Sign.Model.DeletedSession.toModal() = when (this) {
    is Sign.Model.DeletedSession.Error -> Modal.Model.DeletedSession.Error(error)
    is Sign.Model.DeletedSession.Success -> Modal.Model.DeletedSession.Success(topic, reason)
}

internal fun Sign.Model.SessionRequestResponse.toModal() = Modal.Model.SessionRequestResponse(topic, chainId, method, result.toModal())

internal fun Sign.Model.JsonRpcResponse.toModal() = when (this) {
    is Sign.Model.JsonRpcResponse.JsonRpcError -> Modal.Model.JsonRpcResponse.JsonRpcError(id, code, message)
    is Sign.Model.JsonRpcResponse.JsonRpcResult -> Modal.Model.JsonRpcResponse.JsonRpcResult(id, result)
}

@JvmSynthetic
internal fun Sign.Model.SessionAuthenticateResponse.toModal(): Modal.Model.SessionAuthenticateResponse =
    when (this) {
        is Sign.Model.SessionAuthenticateResponse.Result -> Modal.Model.SessionAuthenticateResponse.Result(id, cacaos.toClient(), session?.toModal())
        is Sign.Model.SessionAuthenticateResponse.Error -> Modal.Model.SessionAuthenticateResponse.Error(id, code, message)
    }

@JvmSynthetic
internal fun Sign.Model.ExpiredProposal.toModal(): Modal.Model.ExpiredProposal = Modal.Model.ExpiredProposal(pairingTopic, proposerPublicKey)

@JvmSynthetic
internal fun Sign.Model.ExpiredRequest.toModal(): Modal.Model.ExpiredRequest = Modal.Model.ExpiredRequest(topic, id)


@JvmSynthetic
internal fun List<Sign.Model.Cacao>.toClient(): List<Modal.Model.Cacao> = this.map {
    with(it) {
        Modal.Model.Cacao(
            Modal.Model.Cacao.Header(header.t),
            Modal.Model.Cacao.Payload(
                payload.iss,
                payload.domain,
                payload.aud,
                payload.version,
                payload.nonce,
                payload.iat,
                payload.nbf,
                payload.exp,
                payload.statement,
                payload.requestId,
                payload.resources
            ),
            Modal.Model.Cacao.Signature(signature.t, signature.s, signature.m)
        )
    }
}

internal fun Sign.Model.ConnectionState.toModal() = Modal.Model.ConnectionState(isAvailable)

internal fun Sign.Model.Error.toModal() = Modal.Model.Error(throwable)

internal fun Sign.Params.Disconnect.toModal() = Modal.Params.Disconnect(sessionTopic)

internal fun Sign.Model.SentRequest.toModal() = Modal.Model.SentRequest(requestId, sessionTopic, method, params, chainId)

internal fun Sign.Model.Ping.Success.toModal() = Modal.Model.Ping.Success(topic)

internal fun Sign.Model.Ping.Error.toModal() = Modal.Model.Ping.Error(error)

// toSign()
internal fun Modal.Params.Connect.toSign() =
    Sign.Params.Connect(namespaces?.toSign(), optionalNamespaces?.toSign(), properties, scopedProperties, pairing)

internal fun Modal.Params.ConnectParams.toSign() = Sign.Params.ConnectParams(sessionNamespaces?.toSign(), properties, scopedProperties, pairing)

internal fun Modal.Params.Authenticate.toSign(): Sign.Params.Authenticate = with(this) {
    Sign.Params.Authenticate(
        pairingTopic,
        chains = chains,
        domain = domain,
        uri = uri,
        nonce = nonce,
        nbf = nbf,
        exp = exp,
        statement = statement,
        requestId = requestId,
        resources = resources,
        methods = methods,
        expiry = expiry
    )
}

internal fun Modal.Model.AuthPayloadParams.toModel(pairingTopic: String): Modal.Params.Authenticate = with(this) {
    Modal.Params.Authenticate(
        chains = chains,
        domain = domain,
        uri = uri,
        nonce = nonce,
        nbf = nbf,
        exp = exp,
        statement = statement,
        requestId = requestId,
        resources = resources,
        expiry = expiry,
        methods = methods,
        pairingTopic = pairingTopic,
    )
}

internal fun Modal.Model.AuthPayloadParams.toSign(issuer: String): Sign.Params.FormatMessage = with(this) {
    Sign.Params.FormatMessage(
        payloadParams = Sign.Model.PayloadParams(
            chains = chains,
            domain = domain,
            aud = uri,
            nonce = nonce,
            nbf = nbf,
            exp = exp,
            iat = SimpleDateFormat(Cacao.Payload.ISO_8601_PATTERN).format(Calendar.getInstance().time),
            type = "",
            statement = statement,
            requestId = requestId,
            resources = resources,
        ),
        iss = issuer
    )
}


internal fun Map<String, Modal.Model.Namespace.Proposal>.toSign() =
    mapValues { (_, namespace) -> Sign.Model.Namespace.Proposal(namespace.chains, namespace.methods, namespace.events) }

internal fun Modal.Params.Disconnect.toSign() = Sign.Params.Disconnect(sessionTopic)

internal fun Modal.Params.Ping.toSign() = Sign.Params.Ping(topic)

internal fun Request.toSign(sessionTopic: String, chainId: String) = Sign.Params.Request(sessionTopic, method, params, chainId, expiry)

internal fun Modal.Listeners.SessionPing.toSign() = object : Sign.Listeners.SessionPing {
    override fun onSuccess(pingSuccess: Sign.Model.Ping.Success) {
        this@toSign.onSuccess(pingSuccess.toModal())
    }

    override fun onError(pingError: Sign.Model.Ping.Error) {
        this@toSign.onError(pingError.toModal())
    }
}

internal fun Sign.Model.Session.toSession() = Session.WalletConnectSession(pairingTopic, topic, expiry, namespaces.toModal(), metaData)

internal fun Account.toCoinbaseSession() = Session.CoinbaseSession(chain.id, address)