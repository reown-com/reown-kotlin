package com.reown.foundation.crypto.data.repository

interface ClientIdJwtRepository {

    fun generateJWT(serverUrl: String, getIssuerClientId: (String) -> Unit = {}): String
}