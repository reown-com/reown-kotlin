package com.reown.sample.wallet.blockchain

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

fun createBlockChainApiService(projectId: String, chainId: String, rpcUrl: String = "https://rpc.walletconnect.com"): BlockChainApiService {
    val httpClient = OkHttpClient.Builder()

    // Logging interceptor (optional)
    val logging = HttpLoggingInterceptor()
    logging.setLevel(HttpLoggingInterceptor.Level.BODY)
    httpClient.addInterceptor(logging)

    // Interceptor to add query parameters
    val queryParameterInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val originalHttpUrl = originalRequest.url

        val newUrl = originalHttpUrl.newBuilder()
            .addQueryParameter("chainId", chainId)
            .addQueryParameter("projectId", projectId)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        chain.proceed(newRequest)
    }

    httpClient.addInterceptor(queryParameterInterceptor)

    val retrofit = Retrofit.Builder()
        .baseUrl(rpcUrl)
        .client(httpClient.build())
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    return retrofit.create(BlockChainApiService::class.java)
}