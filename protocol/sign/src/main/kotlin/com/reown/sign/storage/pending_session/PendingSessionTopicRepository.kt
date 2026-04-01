@file:JvmSynthetic

package com.reown.sign.storage.pending_session

import java.util.concurrent.ConcurrentHashMap

internal class PendingSessionTopicRepository {
    private val pendingSessionTopics = ConcurrentHashMap<String, String>()

    @JvmSynthetic
    fun insert(proposerPublicKey: String, sessionTopic: String) {
        pendingSessionTopics[proposerPublicKey] = sessionTopic
    }

    @JvmSynthetic
    fun getAndRemove(proposerPublicKey: String): String? {
        return pendingSessionTopics.remove(proposerPublicKey)
    }

    @JvmSynthetic
    fun getAllSessionTopics(): Collection<String> = pendingSessionTopics.values
}
