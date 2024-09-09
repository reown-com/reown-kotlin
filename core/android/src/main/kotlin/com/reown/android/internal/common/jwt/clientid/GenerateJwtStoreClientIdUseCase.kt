@file:JvmSynthetic

package com.reown.android.internal.common.jwt.clientid

import android.content.SharedPreferences
import androidx.core.content.edit
import com.reown.android.internal.common.di.KEY_CLIENT_ID
import com.reown.android.utils.strippedUrl
import com.reown.foundation.crypto.data.repository.ClientIdJwtRepository

internal class GenerateJwtStoreClientIdUseCase(private val clientIdJwtRepository: ClientIdJwtRepository, private val sharedPreferences: SharedPreferences) {

    operator fun invoke(relayUrl: String): String =
        clientIdJwtRepository.generateJWT(relayUrl.strippedUrl()) { clientId ->
            sharedPreferences.edit {
                putString(KEY_CLIENT_ID, clientId)
            }
        }
}