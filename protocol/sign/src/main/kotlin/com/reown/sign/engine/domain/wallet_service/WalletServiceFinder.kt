package com.reown.sign.engine.domain.wallet_service

import com.reown.foundation.util.Logger
import com.reown.sign.common.model.vo.sequence.SessionVO
import com.reown.sign.engine.model.EngineDO
import java.net.URL

internal class WalletServiceFinder(private val logger: Logger) {

    /**
     * Finds a matching wallet service URL for the given request based on scopedProperties
     */
    fun findMatchingWalletService(request: EngineDO.Request, session: SessionVO): URL? {
        val scopedProperties = session.scopedProperties ?: return null

        // Check for exact chain match first (e.g., "eip155:1")
        findWalletService(request.method, scopedProperties, request.chainId)?.let { return it }

        // Check for namespace match (e.g., "eip155" for any eip155 chain)
        val namespace = request.chainId.split(":").first()
        findWalletService(request.method, scopedProperties, namespace)?.let { return it }

        return null
    }

    /**
     * Finds a wallet service that supports the given method in the specified scope
     */
    private fun findWalletService(method: String, scopedProperties: Map<String, String>, key: String): URL? {
        println("kobe: method: $method; scopedProperties: $scopedProperties; key: $key")

        val scopeJSON = scopedProperties[key] ?: return null
        val scopeData = scopeJSON.toByteArray(Charsets.UTF_8)

        try {
            // Parse the JSON data
            val jsonObject = org.json.JSONObject(String(scopeData))
            val walletServices = jsonObject.optJSONArray("walletService") ?: return null

            // Find a service that supports the requested method
            for (i in 0 until walletServices.length()) {
                val service = walletServices.optJSONObject(i) ?: continue
                val url = service.optString("url")
                val methodsArray = service.optJSONArray("methods") ?: continue

                val methods = mutableListOf<String>()
                for (j in 0 until methodsArray.length()) {
                    methods.add(methodsArray.optString(j))
                }

                if (methods.contains(method) && url.isNotEmpty()) {
                    val serviceURL = try {
                        URL(url)
                    } catch (e: Exception) {
                        continue
                    }
                    return serviceURL
                }
            }
        } catch (error: Exception) {
            logger.error("Failed to parse scopedProperties JSON: $error")
        }

        return null
    }
}