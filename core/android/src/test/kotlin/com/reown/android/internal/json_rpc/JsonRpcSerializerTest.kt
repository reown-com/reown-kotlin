package com.reown.android.internal.json_rpc

import com.reown.android.internal.common.json_rpc.data.JsonRpcSerializer
import com.reown.android.internal.common.model.type.SerializableJsonRpc
import com.reown.utils.JsonAdapterEntry
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.reflect.KClass

class JsonRpcSerializerTest {

    @Test
    fun `test JsonRpcSerializer creation with KotlinJsonAdapterFactory`() {
        // Create a minimal JsonRpcSerializer with just the necessary components
        val serializerEntries = setOf<KClass<out SerializableJsonRpc>>()
        val deserializerEntries = mapOf<String, KClass<*>>()
        val jsonAdapterEntries = setOf<JsonAdapterEntry<*>>()
        
        val moshiBuilder = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
        
        val serializer = JsonRpcSerializer(
            serializerEntries = serializerEntries,
            deserializerEntries = deserializerEntries,
            jsonAdapterEntries = jsonAdapterEntries,
            moshiBuilder = moshiBuilder
        )
        
        // Test that the moshi instance is created successfully
        assertNotNull("Moshi instance should not be null", serializer.moshi)
        
        println("JsonRpcSerializer created successfully with KotlinJsonAdapterFactory")
    }
} 