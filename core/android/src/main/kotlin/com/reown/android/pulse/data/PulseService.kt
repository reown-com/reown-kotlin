@file:JvmSynthetic

package com.reown.android.pulse.data

import com.reown.android.pulse.model.Event
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface PulseService {
    @Headers("Content-Type: application/json")
    @POST("/e")
    suspend fun sendEvent(@Header("x-sdk-type") sdkType: String,  @Body body: Event): Response<Unit>

    @Headers("Content-Type: application/json")
    @POST("/batch")
    suspend fun sendEventBatch(@Header("x-sdk-type") sdkType: String,  @Body body: List<Event>): Response<Unit>
}