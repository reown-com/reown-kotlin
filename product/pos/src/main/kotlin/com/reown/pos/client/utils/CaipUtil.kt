package com.reown.pos.client.utils

object CaipUtil {
    fun isValidChainId(chainId: String): Boolean {
        if (chainId.isBlank()) return false
        
        val parts = chainId.split(":")
        if (parts.size != 2) return false
        
        val namespace = parts[0]
        val reference = parts[1]
        
        if (namespace.isBlank() || reference.isBlank()) return false
        
        // Validate namespace (should be lowercase alphanumeric)
        if (!namespace.matches(Regex("^[a-z0-9]+$"))) return false
        
        // Validate reference (should be alphanumeric, can contain hyphens and underscores)
        if (!reference.matches(Regex("^[a-zA-Z0-9_-]+$"))) return false
        
        return true
    }
    fun isValidAccountId(accountId: String): Boolean {
        if (accountId.isBlank()) return false
        
        val parts = accountId.split(":")
        if (parts.size != 3) return false
        
        val chainId = "${parts[0]}:${parts[1]}"
        val accountAddress = parts[2]
        
        if (!isValidChainId(chainId)) return false
        if (accountAddress.isBlank()) return false
        
        return true
    }

    fun isValidAssetId(assetId: String): Boolean {
        if (assetId.isBlank()) return false
        
        val parts = assetId.split(":")
        if (parts.size < 3) return false
        
        val chainId = "${parts[0]}:${parts[1]}"
        if (!isValidChainId(chainId)) return false
        
        val assetNamespace = parts[2]
        if (assetNamespace.isBlank()) return false
        
        // Check if there are more parts for asset reference
        if (parts.size > 3) {
            val assetReference = parts[3]
            if (assetReference.isBlank()) return false
        }
        
        return true
    }

    fun extractChainIdFromAccountId(accountId: String): String? {
        if (!isValidAccountId(accountId)) return null
        
        val parts = accountId.split(":")
        return "${parts[0]}:${parts[1]}"
    }

    fun extractAccountAddressFromAccountId(accountId: String): String? {
        if (!isValidAccountId(accountId)) return null
        
        val parts = accountId.split(":")
        return parts[2]
    }

    fun extractChainIdFromAssetId(assetId: String): String? {
        if (!isValidAssetId(assetId)) return null
        
        val parts = assetId.split(":")
        return "${parts[0]}:${parts[1]}"
    }
    
    /**
     * Builds a CAIP-19 asset ID from components
     * @param chainId The chain ID (e.g., "eip155:1")
     * @param assetNamespace The asset namespace (e.g., "erc20")
     * @param assetReference The asset reference (e.g., contract address)
     * @return The complete CAIP-19 asset ID
     */
    fun buildAssetId(chainId: String, assetNamespace: String, assetReference: String): String {
        require(isValidChainId(chainId)) { "Invalid chain ID: $chainId" }
        require(assetNamespace.isNotBlank()) { "Asset namespace cannot be blank" }
        require(assetReference.isNotBlank()) { "Asset reference cannot be blank" }
        
        return "$chainId:$assetNamespace:$assetReference"
    }
    
    /**
     * Builds a CAIP-10 account ID from components
     * @param chainId The chain ID (e.g., "eip155:1")
     * @param accountAddress The account address
     * @return The complete CAIP-10 account ID
     */
    fun buildAccountId(chainId: String, accountAddress: String): String {
        require(isValidChainId(chainId)) { "Invalid chain ID: $chainId" }
        require(accountAddress.isNotBlank()) { "Account address cannot be blank" }
        
        return "$chainId:$accountAddress"
    }
}
