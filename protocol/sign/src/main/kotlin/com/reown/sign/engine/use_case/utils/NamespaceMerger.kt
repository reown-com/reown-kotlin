package com.reown.sign.engine.use_case.utils

import com.reown.sign.engine.model.EngineDO

internal object NamespaceMerger {
    /**
     * Merges required namespaces into optional namespaces, avoiding duplications.
     * This method moves all required namespaces to optional namespaces to prevent
     * connection failures when wallets don't support all required methods/chains.
     *
     * @param requiredNamespaces The required namespaces to be merged
     * @param optionalNamespaces The existing optional namespaces (can be null)
     * @return A merged map of optional namespaces containing both original optional and moved required namespaces
     */
    fun merge(
        requiredNamespaces: Map<String, EngineDO.Namespace.Proposal>?,
        optionalNamespaces: Map<String, EngineDO.Namespace.Proposal>?
    ): Map<String, EngineDO.Namespace.Proposal>? {
        val mergedOptionalNamespaces = optionalNamespaces?.toMutableMap() ?: mutableMapOf()
        
        requiredNamespaces?.forEach { (key, requiredNamespace) ->
            val existingOptional = mergedOptionalNamespaces[key]
            if (existingOptional != null) {
                // Merge chains
                val mergedChains = mergeChains(required = requiredNamespace.chains, optional = existingOptional.chains)
                
                // Merge methods (union - no duplicates)
                val mergedMethods = (requiredNamespace.methods + existingOptional.methods).distinct()
                
                // Merge events (union - no duplicates)
                val mergedEvents = (requiredNamespace.events + existingOptional.events).distinct()
                
                mergedOptionalNamespaces[key] = EngineDO.Namespace.Proposal(
                    chains = mergedChains,
                    methods = mergedMethods,
                    events = mergedEvents
                )
            } else {
                // If no existing optional namespace for this key, add the required one
                mergedOptionalNamespaces[key] = requiredNamespace
            }
        }
        
        return if (mergedOptionalNamespaces.isEmpty()) null else mergedOptionalNamespaces
    }
    
    /**
     * Helper method to merge chains from required and optional namespaces
     * @param required The chains from the required namespace
     * @param optional The chains from the optional namespace
     * @return A merged list of chains without duplicates
     */
    private fun mergeChains(
        required: List<String>?,
        optional: List<String>?
    ): List<String> {
        val mergedChains = mutableListOf<String>()
        
        // Add required chains
        required?.let { mergedChains.addAll(it) }
        
        // Add optional chains that are not already in the merged list
        optional?.forEach { chain ->
            if (!mergedChains.contains(chain)) {
                mergedChains.add(chain)
            }
        }
        
        return mergedChains
    }
}