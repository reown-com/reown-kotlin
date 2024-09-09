package com.reown.android.push.network

import com.reown.android.push.network.model.PushBody
import com.reown.android.push.network.model.PushResponse
import retrofit2.Response
import retrofit2.http.*

interface PushService {

    @POST("{projectId}/clients")
    suspend fun register(@Path("projectId") projectId: String, @Query("auth") clientID: String, @Body echoClientsBody: PushBody): Response<PushResponse>

    @DELETE("{projectId}/clients/{clientId}")
    suspend fun unregister(@Path("projectId") projectId: String, @Path("clientId") clientID: String): Response<PushResponse>
}