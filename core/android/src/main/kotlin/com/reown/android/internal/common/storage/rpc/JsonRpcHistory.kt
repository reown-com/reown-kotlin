package com.reown.android.internal.common.storage.rpc

import com.reown.android.internal.common.json_rpc.model.JsonRpcHistoryRecord
import com.reown.android.internal.common.model.TransportType
import com.reown.android.sdk.storage.data.dao.JsonRpcHistoryQueries
import com.reown.foundation.common.model.Topic
import com.reown.foundation.util.Logger

class JsonRpcHistory(
    private val jsonRpcHistoryQueries: JsonRpcHistoryQueries,
    private val logger: Logger,
) {

    fun setRequest(requestId: Long, topic: Topic, method: String, payload: String, transportType: TransportType): Boolean {
        return try {
            if (jsonRpcHistoryQueries.doesJsonRpcNotExist(requestId).executeAsOne()) {
                jsonRpcHistoryQueries.insertOrAbortJsonRpcHistory(requestId, topic.value, method, payload, transportType)
                jsonRpcHistoryQueries.selectLastInsertedRowId().executeAsOne() > 0L
            } else {
                logger.error("Duplicated JsonRpc RequestId: $requestId")
                false
            }
        } catch (e: Exception) {
            logger.error(e)
            false
        }
    }

    fun updateRequestWithResponse(requestId: Long, response: String): JsonRpcHistoryRecord? {
        val record = jsonRpcHistoryQueries.getJsonRpcHistoryRecord(requestId, mapper = ::toRecord).executeAsOneOrNull()
        return if (record != null) {
            updateRecord(record, requestId, response)
        } else {
            logger.log("No JsonRpcRequest matching response")
            null
        }
    }

    private fun updateRecord(record: JsonRpcHistoryRecord, requestId: Long, response: String): JsonRpcHistoryRecord? =
        if (record.response != null) {
            logger.log("Duplicated JsonRpc RequestId: $requestId")
            null
        } else {
            jsonRpcHistoryQueries.updateJsonRpcHistory(response = response, request_id = requestId)
            record
        }

    fun deleteRecordsByTopic(topic: Topic) {
        jsonRpcHistoryQueries.deleteJsonRpcHistory(topic.value)
    }

    fun deleteRecordById(id: Long) {
        jsonRpcHistoryQueries.deleteJsonRpcHistoryByRequestId(id)
    }

    fun getListOfPendingRecordsByTopic(topic: Topic): List<JsonRpcHistoryRecord> =
        jsonRpcHistoryQueries.getJsonRpcRecordsByTopic(topic.value, mapper = ::toRecord)
            .executeAsList()
            .filter { record -> record.response == null }

    fun getListOfPendingRecords(): List<JsonRpcHistoryRecord> =
        jsonRpcHistoryQueries.getJsonRpcRecords(mapper = ::toRecord)
            .executeAsList()
            .filter { record -> record.response == null }

    fun getListOfPendingSessionRequests(): List<JsonRpcHistoryRecord> =
        jsonRpcHistoryQueries.getPendingSessionRequests(mapper = ::toRecord).executeAsList()

    fun getPendingRecordById(id: Long): JsonRpcHistoryRecord? {
        val record = jsonRpcHistoryQueries.getJsonRpcHistoryRecord(id, mapper = ::toRecord).executeAsOneOrNull()
        return if (record != null && record.response == null) record else null
    }

    fun getRecordById(id: Long): JsonRpcHistoryRecord? {
        return jsonRpcHistoryQueries.getJsonRpcHistoryRecord(id, mapper = ::toRecord).executeAsOneOrNull()
    }

    private fun toRecord(requestId: Long, topic: String, method: String, body: String, response: String?, transportType: TransportType?): JsonRpcHistoryRecord =
        JsonRpcHistoryRecord(requestId, topic, method, body, response, transportType)
}