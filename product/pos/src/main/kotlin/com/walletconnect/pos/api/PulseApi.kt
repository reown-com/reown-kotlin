package com.walletconnect.pos.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

internal interface PulseApi {

    @Headers("Content-Type: application/json")
    @POST("/e")
    suspend fun sendEvent(
        @Header("x-sdk-type") sdkType: String,
        @Header("x-sdk-version") sdkVersion: String,
        @Header("x-project-id") projectId: String,
        @Body body: PulseEvent
    ): Response<Unit>
}
