package com.reown.appkit.data.network

import com.reown.appkit.data.model.IdentityDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface BlockchainService {

    @GET("identity/{address}")
    suspend fun getIdentity(
        @Path("address") address: String,
        @Query("chainId") chainId: String,
        @Query("projectId") projectId: String
    ): Response<IdentityDTO>

}
