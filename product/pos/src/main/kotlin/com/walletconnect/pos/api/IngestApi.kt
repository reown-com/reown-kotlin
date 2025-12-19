package com.walletconnect.pos.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface IngestApi {

    @POST("event")
    suspend fun sendEvent(@Body request: IngestEventRequest): Response<Unit>
}
