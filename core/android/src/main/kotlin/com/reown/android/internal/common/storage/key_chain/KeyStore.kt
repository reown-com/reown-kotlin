@file:JvmSynthetic

package com.reown.android.internal.common.storage.key_chain

import com.reown.foundation.common.model.Key

interface KeyStore {
    fun getKey(tag: String): String?
    fun setKey(tag: String, key: Key)

    fun getKeys(tag: String): Pair<String, String>?
    fun setKeys(tag: String, key1: Key, key2: Key)

    fun deleteKeys(tag: String)
    fun checkKeys(tag: String): Boolean
}