@file:JvmSynthetic

package com.reown.android.internal

import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.RelayProtocolOptions
import com.reown.android.internal.common.model.SymmetricKey
import com.reown.android.internal.common.model.WalletConnectUri
import com.reown.foundation.common.model.Topic
import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder


object Validator {

    private const val WC_URI_QUERY_KEY = "wc?uri="

    @JvmSynthetic
    fun validateWCUri(uri: String): WalletConnectUri? {
        val wcUri = getWcUri(uri)
        if (!wcUri.startsWith("wc:")) return null

        val properUriString = when {
            wcUri.contains("wc://") -> wcUri
            wcUri.contains("wc:/") -> wcUri.replace("wc:/", "wc://")
            else -> wcUri.replace("wc:", "wc://")
        }

        val pairUri: URI = try {
            URI(properUriString)
        } catch (e: URISyntaxException) {
            return null
        }

        if (pairUri.userInfo.isEmpty()) return null
        val mapOfQueryParameters: Map<String, String> =
            pairUri.query.split("&").associate { query -> query.substringBefore("=") to query.substringAfter("=") }

        var relayProtocol = ""
        mapOfQueryParameters["relay-protocol"]?.let { relayProtocol = it } ?: return null
        if (relayProtocol.isEmpty()) return null

        val relayData: String? = mapOfQueryParameters["relay-data"]
        val expiry: String? = mapOfQueryParameters["expiryTimestamp"]
        val methods: String? = mapOfQueryParameters["methods"]

        var symKey = ""
        mapOfQueryParameters["symKey"]?.let { symKey = it } ?: return null
        if (symKey.isEmpty()) return null

        return WalletConnectUri(
            topic = Topic(pairUri.userInfo),
            relay = RelayProtocolOptions(protocol = relayProtocol, data = relayData),
            symKey = SymmetricKey(symKey),
            expiry = if (expiry != null) Expiry(expiry.toLong()) else null,
            methods = methods
        )
    }

    private fun getWcUri(uriScheme: String): String {
        return try {
            val uri = if (uriScheme.contains(WC_URI_QUERY_KEY)) {
                uriScheme.split(WC_URI_QUERY_KEY)[1]
            } else {
                uriScheme
            }
            URLDecoder.decode(uri, "UTF-8")
        } catch (e: Throwable) {
            uriScheme
        }
    }
}