@file:JvmName("ApprovedNamespacesUtils")

package com.reown.sign.client.utils

import android.util.Base64
import com.reown.android.internal.common.model.Namespace
import com.reown.android.internal.common.signing.cacao.Cacao.Payload.Companion.RECAPS_PREFIX
import com.reown.android.internal.common.signing.cacao.CacaoType
import com.reown.android.internal.common.signing.cacao.RECAPS_STATEMENT
import com.reown.android.internal.common.signing.cacao.decodeReCaps
import com.reown.android.internal.common.signing.cacao.getStatement
import com.reown.android.internal.common.signing.cacao.parseReCaps
import com.reown.android.internal.utils.CoreValidator
import com.reown.sign.client.Sign
import com.reown.sign.client.mapper.toCacaoPayload
import com.reown.sign.client.mapper.toCore
import com.reown.sign.client.mapper.toProposalNamespacesVO
import com.reown.sign.client.mapper.toSessionNamespacesVO
import com.reown.sign.common.validator.SignValidator
import org.json.JSONArray
import org.json.JSONObject

fun generateApprovedNamespaces(
    proposal: Sign.Model.SessionProposal,
    supportedNamespaces: Map<String, Sign.Model.Namespace.Session>,
): Map<String, Sign.Model.Namespace.Session> {
    val supportedNamespacesVO = supportedNamespaces.toSessionNamespacesVO()
    val normalizedRequiredNamespaces = normalizeNamespaces(proposal.requiredNamespaces.toProposalNamespacesVO())
    val normalizedOptionalNamespaces = normalizeNamespaces(proposal.optionalNamespaces.toProposalNamespacesVO())

    SignValidator.validateProposalNamespaces(normalizedRequiredNamespaces) { error -> throw Exception(error.message) }
    SignValidator.validateProposalNamespaces(normalizedOptionalNamespaces) { error -> throw Exception(error.message) }
    SignValidator.validateSupportedNamespace(supportedNamespacesVO, normalizedRequiredNamespaces) { error -> throw Exception(error.message) }

    if (proposal.requiredNamespaces.isEmpty() && proposal.optionalNamespaces.isEmpty()) {
        return supportedNamespacesVO.toCore()
    }

    val approvedNamespaces = mutableMapOf<String, Namespace.Session>()
    normalizedRequiredNamespaces.forEach { (key, requiredNamespace) ->
        val chains = supportedNamespacesVO[key]?.chains?.filter { chain -> requiredNamespace.chains!!.contains(chain) } ?: emptyList()
        val methods = supportedNamespaces[key]?.methods?.filter { method -> requiredNamespace.methods.contains(method) } ?: emptyList()
        val events = supportedNamespaces[key]?.events?.filter { event -> requiredNamespace.events.contains(event) } ?: emptyList()
        val accounts = chains.flatMap { chain ->
            supportedNamespaces[key]?.accounts?.filter { account -> SignValidator.getChainFromAccount(account) == chain } ?: emptyList()
        }

        approvedNamespaces[key] = Namespace.Session(chains = chains, methods = methods, events = events, accounts = accounts)
    }

    normalizedOptionalNamespaces.forEach { (key, optionalNamespace) ->
        if (!supportedNamespaces.containsKey(key)) return@forEach
        val chains = supportedNamespacesVO[key]?.chains?.filter { chain -> optionalNamespace.chains!!.contains(chain) } ?: emptyList()
        val methods = supportedNamespaces[key]?.methods?.filter { method -> optionalNamespace.methods.contains(method) } ?: emptyList()
        val events = supportedNamespaces[key]?.events?.filter { event -> optionalNamespace.events.contains(event) } ?: emptyList()
        val accounts = chains.flatMap { chain ->
            supportedNamespaces[key]?.accounts?.filter { account -> SignValidator.getChainFromAccount(account) == chain } ?: emptyList()
        }

        if (chains.isNotEmpty()) {
            approvedNamespaces[key] = Namespace.Session(
                chains = approvedNamespaces[key]?.chains?.plus(chains)?.distinct() ?: chains,
                methods = approvedNamespaces[key]?.methods?.plus(methods)?.distinct() ?: methods,
                events = approvedNamespaces[key]?.events?.plus(events)?.distinct() ?: events,
                accounts = approvedNamespaces[key]?.accounts?.plus(accounts)?.distinct() ?: accounts
            )
        }
    }

    return approvedNamespaces.toCore()
}

internal fun normalizeNamespaces(namespaces: Map<String, Namespace.Proposal>): Map<String, Namespace.Proposal> {
    if (SignValidator.isNamespaceKeyRegexCompliant(namespaces)) return namespaces
    return mutableMapOf<String, Namespace.Proposal>().apply {
        namespaces.forEach { (key, namespace) ->
            val normalizedKey = normalizeKey(key)
            this[normalizedKey] = Namespace.Proposal(
                chains = getChains(normalizedKey).plus(getNamespaceChains(key, namespace)),
                methods = getMethods(normalizedKey).plus(namespace.methods),
                events = getEvents(normalizedKey).plus(namespace.events)
            )
        }
    }.toMap()
}

private fun getNamespaceChains(key: String, namespace: Namespace) =
    if (CoreValidator.isChainIdCAIP2Compliant(key)) listOf(key) else namespace.chains!!

private fun normalizeKey(key: String): String = if (CoreValidator.isChainIdCAIP2Compliant(key)) SignValidator.getNamespaceKeyFromChainId(key) else key
private fun MutableMap<String, Namespace.Proposal>.getChains(normalizedKey: String) = (this[normalizedKey]?.chains ?: emptyList())
private fun MutableMap<String, Namespace.Proposal>.getMethods(normalizedKey: String) = (this[normalizedKey]?.methods ?: emptyList())
private fun MutableMap<String, Namespace.Proposal>.getEvents(normalizedKey: String) = (this[normalizedKey]?.events ?: emptyList())

fun generateAuthObject(payload: Sign.Model.PayloadParams, issuer: String, signature: Sign.Model.Cacao.Signature): Sign.Model.Cacao {
    return Sign.Model.Cacao(
        header = Sign.Model.Cacao.Header(t = CacaoType.CAIP222.header),
        payload = payload.toCacaoPayload(issuer),
        signature = signature
    )
}

fun generateAuthPayloadParams(
    payloadParams: Sign.Model.PayloadParams,
    supportedChains: List<String>,
    supportedMethods: List<String>
): Sign.Model.PayloadParams {
    val reCapsJson: String? = payloadParams.resources.decodeReCaps()
    if (reCapsJson.isNullOrEmpty() || !reCapsJson.contains("eip155")) return payloadParams

    val sessionReCaps = reCapsJson.parseReCaps()["eip155"]
    val requestedMethods = sessionReCaps!!.keys.map { key -> key.substringAfter('/') }
    val requestedChains = payloadParams.chains

    val sessionChains = requestedChains.intersect(supportedChains.toSet()).toList().distinct()
    val sessionMethods = requestedMethods.intersect(supportedMethods.toSet()).toList().distinct()

    if (sessionChains.isEmpty()) throw Exception("Unsupported chains")
    if (sessionMethods.isEmpty()) throw Exception("Unsupported methods")

    if (!sessionChains.all { chain -> CoreValidator.isChainIdCAIP2Compliant(chain) }) throw Exception("Chains are not CAIP-2 compliant")
    if (!sessionChains.all { chain -> SignValidator.getNamespaceKeyFromChainId(chain) == "eip155" }) throw Exception("Only eip155(EVM) is supported")

    val actionsJsonObject = JSONObject()
    val chainsJsonArray = JSONArray()
    sessionChains.forEach { chain -> chainsJsonArray.put(chain) }
    sessionMethods.forEach { method -> actionsJsonObject.put("request/$method", JSONArray().put(0, JSONObject().put("chains", chainsJsonArray))) }

    val recaps = JSONObject(reCapsJson)
    val att = recaps.getJSONObject("att")
    att.put("eip155", actionsJsonObject)
    val stringReCaps = recaps.toString().replace("\\/", "/")
    val base64Recaps = Base64.encodeToString(stringReCaps.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.NO_PADDING)
    val newReCapsUrl = "${RECAPS_PREFIX}$base64Recaps"

    if (payloadParams.resources == null) {
        payloadParams.resources = listOf(newReCapsUrl)
    } else {
        payloadParams.resources = payloadParams.resources!!.dropLast(1).plus(newReCapsUrl)
    }

    return with(payloadParams) {
        Sign.Model.PayloadParams(
            chains = sessionChains,
            domain = domain,
            nonce = nonce,
            aud = aud,
            type = type,
            nbf = nbf,
            exp = exp,
            iat = iat,
            statement = getStatement(),
            resources = resources,
            requestId = requestId
        )
    }
}

private fun Sign.Model.PayloadParams.getStatement() =
    if (statement?.contains(RECAPS_STATEMENT) == true) {
        statement
    } else {
        Pair(statement, resources).getStatement()
    }

