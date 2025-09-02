package com.reown.pos.client.utils

object CaipUtil {
    fun isValidChainId(chainId: String): Boolean {
        if (chainId.isBlank()) return false
        
        val parts = chainId.split(":")
        if (parts.size != 2) return false
        
        val namespace = parts[0]
        val reference = parts[1]
        
        if (namespace.isBlank() || reference.isBlank()) return false
        
        if (!namespace.matches(Regex("^[a-z0-9]+$"))) return false
        
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
}
