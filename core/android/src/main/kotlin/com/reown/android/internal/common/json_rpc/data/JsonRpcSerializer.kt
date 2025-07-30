package com.reown.android.internal.common.json_rpc.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.reown.android.internal.common.JsonRpcResponse
import com.reown.android.internal.common.model.type.ClientParams
import com.reown.android.internal.common.model.type.JsonRpcClientSync
import com.reown.android.internal.common.model.type.SerializableJsonRpc
import com.reown.utils.JsonAdapterEntry
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

class JsonRpcSerializer(
    val serializerEntries: Set<KClass<*>>,
    val deserializerEntries: Map<String, KClass<*>>,
    val jsonAdapterEntries: Set<JsonAdapterEntry<*>>,
    val moshiBuilder: Moshi.Builder,
) {
    val moshi: Moshi
        get() = moshiBuilder.add { type, _, moshi ->
            try {
                val entry = jsonAdapterEntries.firstOrNull { it.type == type }
                entry?.adapter?.invoke(moshi)
            } catch (e: Exception) {
                // If custom adapter creation fails, return null to let Moshi use default behavior
                null
            }
        }
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun deserialize(method: String, json: String): ClientParams? {
        val type = deserializerEntries[method] ?: return null
        val deserializedObject = tryDeserialize(json, type) ?: return null

        return if (deserializedObject::class == type && deserializedObject is JsonRpcClientSync<*>) {
            deserializedObject.params
        } else {
            null
        }
    }

    fun serialize(payload: SerializableJsonRpc): String? {
        lateinit var payloadType: KClass<*>
        return when {
            payload is JsonRpcResponse.JsonRpcResult -> trySerialize(payload)
            payload is JsonRpcResponse.JsonRpcError -> trySerialize(payload)
            serializerEntries.any { type: KClass<*> ->
                payloadType = type
                type.safeCast(payload) != null
            } -> trySerialize(payload, payloadType)
            else -> null
        }
    }

    inline fun <reified T> tryDeserialize(json: String): T? = runCatching { moshi.adapter(T::class.java).fromJson(json) }.getOrNull()
    fun tryDeserialize(json: String, type: KClass<*>): Any? = runCatching { moshi.adapter(type.java).fromJson(json) }.getOrNull()
    
    private inline fun <reified T> trySerialize(type: T): String = runCatching { 
        moshi.adapter(T::class.java).toJson(type) 
    }.getOrElse { throw RuntimeException("Failed to serialize ${T::class.java.simpleName}: ${it.message}", it) }
    
    fun trySerialize(payload: Any, type: KClass<*>): String = runCatching { 
        moshi.adapter<Any>(type.java).toJson(payload) 
    }.getOrElse { throw RuntimeException("Failed to serialize ${type.simpleName}: ${it.message}", it) }
}