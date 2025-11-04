@file:JvmSynthetic

package com.reown.sign.common.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.internal.Util
import com.reown.sign.common.model.vo.clientsync.session.payload.SessionRequestVO
import org.json.JSONArray
import org.json.JSONObject
import kotlin.String

internal class SessionRequestVOJsonAdapter(moshi: Moshi) : JsonAdapter<SessionRequestVO>() {
    private val options: JsonReader.Options = JsonReader.Options.of("method", "params", "expiryTimestamp")
    private val stringAdapter: JsonAdapter<String> = moshi.adapter(String::class.java, emptySet(), "method")
    private val anyAdapter: JsonAdapter<Any> = moshi.adapter(Any::class.java, emptySet(), "params")
    private val longAdapter: JsonAdapter<Long> = moshi.adapter(Long::class.java, emptySet(), "expiryTimestamp")

    override fun toString(): String = buildString(38) {
        append("GeneratedJsonAdapter(").append("SessionRequestVO").append(')')
    }

    override fun fromJson(reader: JsonReader): SessionRequestVO {
        var method: String? = null
        var params: String? = null
        var expiryTimestamp: Long? = null

        reader.beginObject()

        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 -> method = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull("method", "method", reader)
                1 -> {
                    // Moshi does not handle malformed JSON where there is a missing key for an array or object
                    val paramsAny = anyAdapter.fromJson(reader) ?: throw Util.unexpectedNull("params", "params", reader)
                    params = if (paramsAny is List<*>) {
                        upsertArray(JSONArray(), paramsAny).toString()
                    } else {
                        val paramsMap = paramsAny as Map<*, *>
                        processObject(JSONObject(), paramsMap).toString()
                    }
                }

                2 -> expiryTimestamp = longAdapter.fromJson(reader) ?: throw Util.unexpectedNull("expiryTimestamp", "expiryTimestamp", reader)

                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()

        return SessionRequestVO(
            method = method ?: throw Util.missingProperty("method", "method", reader),
            params = params ?: throw Util.missingProperty("params", "params", reader),
            expiryTimestamp = expiryTimestamp
        )
    }

    private fun processObject(rootObject: JSONObject, paramsMap: Map<*, *>): JSONObject {
        (paramsMap as Map<String, Any?>).forEach { (key, value) ->
            val processedValue = when (value) {
                is List<*> -> upsertArray(JSONArray(), value)
                is Map<*, *> -> processObject(JSONObject(), value)
                is Number -> when {
                    value.toDouble() % 1 == 0.0 -> value.toLong()
                    else -> value.toDouble()
                }
                else -> value ?: JSONObject.NULL
            }
            
            rootObject.putOpt(key, processedValue)
        }

        return rootObject
    }

    private fun upsertArray(rootArray: JSONArray, paramsList: List<*>): JSONArray {
        paramsList.forEach { value ->
            when (value) {
                is List<*> -> rootArray.put(upsertArray(JSONArray(), value))
                is Map<*, *> -> rootArray.put(processObject(JSONObject(), value))
                is String -> try {
                    when (val deserializedJson = anyAdapter.fromJson(value)) {
                        is List<*> -> rootArray.put(upsertArray(JSONArray(), deserializedJson))
                        is Map<*, *> -> rootArray.put(processObject(JSONObject(), deserializedJson))
                        is Number -> rootArray.put(value.toString())
                        else -> throw IllegalArgumentException("Failed Deserializing Unknown Type $value")
                    }
                } catch (e: Exception) {
                    rootArray.put(value)
                }

                is Number -> {
                    val castedNumber = if (value.toDouble() % 1 == 0.0) {
                        value.toLong()
                    } else {
                        value.toDouble()
                    }

                    rootArray.put(castedNumber)
                }

                else -> rootArray.put(value ?: JSONObject.NULL)
            }
        }

        return rootArray
    }

    override fun toJson(writer: JsonWriter, value_: SessionRequestVO?) {
        if (value_ == null) {
            throw NullPointerException("value_ was null! Wrap in .nullSafe() to write nullable values.")
        }

        with(writer) {
            beginObject()
            name("method")
            stringAdapter.toJson(this, value_.method)
            name("params")
            valueSink().use {
                val encodedParams: String = anyAdapter.toJson(value_.params)
                    .removeSurrounding("\"")
                    .replace("\\\"", "\"")
                    .replace("\\\\\"", "\\\"")

                it.writeUtf8(encodedParams)
            }
            value_.expiryTimestamp?.let {
                name("expiryTimestamp")
                longAdapter.toJson(this, value_.expiryTimestamp)
            }
            endObject()
        }
    }
}