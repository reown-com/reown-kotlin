@file:JvmSynthetic

package com.reown.foundation.di

import com.reown.foundation.common.model.PrivateKey
import com.reown.foundation.common.model.PublicKey
import com.reown.foundation.crypto.data.repository.BaseClientIdJwtRepository
import com.reown.foundation.crypto.data.repository.ClientIdJwtRepository
import org.koin.dsl.module

internal fun cryptoModule() = module {

    single<ClientIdJwtRepository> {
        object: BaseClientIdJwtRepository() {
            override fun setKeyPair(key: String, privateKey: PrivateKey, publicKey: PublicKey) {}
        }
    }
}
