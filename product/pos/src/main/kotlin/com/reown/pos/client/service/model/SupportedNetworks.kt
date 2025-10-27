package com.reown.pos.client.service.model

import com.reown.pos.client.service.generateId
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsonRpcSupportedNetworksRequest(
    @param:Json(name = "id")
    val id: Int = generateId(),
    @param:Json(name = "jsonrpc")
    val jsonrpc: String = "2.0",
    @param:Json(name = "method")
    val method: String = "wc_pos_supportedNetworks",
    @param:Json(name = "params")
    val params: SupportedNetworksParams
)

@JsonClass(generateAdapter = true)
class SupportedNetworksParams

@JsonClass(generateAdapter = true)
data class JsonRpcSupportedNetworksResponse(
    @Json(name = "jsonrpc")
    val jsonrpc: String,
    @Json(name = "id")
    val id: Int,
    @Json(name = "result")
    val result: SupportedNetworksResult?,
    @Json(name = "error")
    val error: JsonRpcError?
)

@JsonClass(generateAdapter = true)
data class SupportedNetworksResult(
    @Json(name = "namespaces")
    val namespaces: List<SupportedNamespace>
)

@JsonClass(generateAdapter = true)
data class SupportedNamespace(
    @Json(name = "name")
    val name: String,
    @Json(name = "methods")
    val methods: List<String>,
    @Json(name = "events")
    val events: List<String>,
    @Json(name = "assetNamespaces")
    val assetNamespaces: List<String>,
    @Json(name = "capabilities")
    val capabilities: Any?
)