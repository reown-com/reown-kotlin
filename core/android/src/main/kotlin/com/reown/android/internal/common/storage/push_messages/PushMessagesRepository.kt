@file:JvmSynthetic

package com.reown.android.internal.common.storage.push_messages

import android.database.sqlite.SQLiteException
import com.reown.android.internal.common.model.Tags
import com.reown.android.sdk.storage.data.dao.PushMessageQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class PushMessagesRepository(private val pushMessageQueries: PushMessageQueries) {

    val notificationTags = listOf(Tags.SESSION_PROPOSE.id, Tags.SESSION_REQUEST.id, Tags.NOTIFY_MESSAGE.id, Tags.SESSION_AUTHENTICATE.id)

    private val _arePushNotificationsEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val arePushNotificationsEnabled: StateFlow<Boolean> = _arePushNotificationsEnabled.asStateFlow()

    fun enablePushNotifications() {
        _arePushNotificationsEnabled.value = true
    }

    @Throws(SQLiteException::class)
    suspend fun insertPushMessage(id: String, topic: String, blob: String, tag: Int) = withContext(Dispatchers.IO) {
        pushMessageQueries.upsertMessage(id, topic, blob, tag.toLong())
    }

    @Throws(SQLiteException::class)
    suspend fun doesPushMessageExist(id: String): Boolean = withContext(Dispatchers.IO) {
        pushMessageQueries.doesMessagesExistsByRequestId(id).executeAsOneOrNull() ?: false
    }

    @Throws(SQLiteException::class)
    suspend fun deletePushMessagesByTopic(topic: String) = withContext(Dispatchers.IO) {
        pushMessageQueries.deleteMessageByTopic(topic)
    }
}