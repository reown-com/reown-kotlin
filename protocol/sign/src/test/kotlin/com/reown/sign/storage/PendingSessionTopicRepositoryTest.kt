package com.reown.sign.storage

import com.reown.sign.storage.pending_session.PendingSessionTopicRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingSessionTopicRepositoryTest {
    private val repository = PendingSessionTopicRepository()

    @Test
    fun `insert and getAndRemove returns stored topic`() {
        repository.insert("publicKey1", "sessionTopic1")
        val result = repository.getAndRemove("publicKey1")
        assertEquals("sessionTopic1", result)
    }

    @Test
    fun `getAndRemove returns null for unknown key`() {
        assertNull(repository.getAndRemove("unknownKey"))
    }

    @Test
    fun `getAndRemove removes entry after retrieval`() {
        repository.insert("publicKey1", "sessionTopic1")
        repository.getAndRemove("publicKey1")
        assertNull(repository.getAndRemove("publicKey1"))
    }

    @Test
    fun `insert overwrites existing entry for same key`() {
        repository.insert("publicKey1", "sessionTopic1")
        repository.insert("publicKey1", "sessionTopic2")
        assertEquals("sessionTopic2", repository.getAndRemove("publicKey1"))
    }

    @Test
    fun `getAllSessionTopics returns all stored topics`() {
        repository.insert("key1", "topic1")
        repository.insert("key2", "topic2")
        val topics = repository.getAllSessionTopics()
        assertEquals(2, topics.size)
        assert(topics.contains("topic1"))
        assert(topics.contains("topic2"))
    }

    @Test
    fun `getAllSessionTopics returns empty when no entries`() {
        assert(repository.getAllSessionTopics().isEmpty())
    }
}
