package com.reown.android.internal.common.di

import android.net.Uri
import android.os.Build
import com.squareup.moshi.Moshi
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.reown.android.BuildConfig
import com.reown.android.internal.common.ConditionalExponentialBackoffStrategy
import com.reown.android.internal.common.connection.ConnectivityState
import com.reown.android.internal.common.connection.DefaultConnectionLifecycle
import com.reown.android.internal.common.connection.ManualConnectionLifecycle
import com.reown.android.internal.common.jwt.clientid.GenerateJwtStoreClientIdUseCase
import com.reown.android.relay.ConnectionType
import com.reown.android.relay.NetworkClientTimeout
import com.reown.foundation.network.data.adapter.FlowStreamAdapter
import com.reown.foundation.network.data.service.RelayService
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidApplication
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

private const val INIT_BACKOFF_MILLIS = 1L
private const val MAX_BACKOFF_SEC = 20L
internal const val KEY_CLIENT_ID = "clientId"

@Suppress("LocalVariableName")
@JvmSynthetic
fun coreAndroidNetworkModule(
    serverUrl: String,
    connectionType: ConnectionType,
    sdkVersion: String,
    timeout: NetworkClientTimeout? = null,
    packageName: String
) = module {
    val networkClientTimeout = timeout ?: NetworkClientTimeout.getDefaultNetworkTimeout()
    factory(named(AndroidCommonDITags.RELAY_URL)) {
        val jwt = get<GenerateJwtStoreClientIdUseCase>().invoke(serverUrl)
        Uri.parse(serverUrl)
            .buildUpon()
            .appendQueryParameter("auth", jwt)
            .appendQueryParameter("ua", get(named(AndroidCommonDITags.USER_AGENT)))
            .appendQueryParameter("packageName", packageName)
            .build()
            .toString()
    }

    factory(named(AndroidCommonDITags.USER_AGENT)) {
        """wc-2/reown-kotlin-${sdkVersion}/android-${Build.VERSION.RELEASE}"""
    }

    single {
        GenerateJwtStoreClientIdUseCase(clientIdJwtRepository = get(), sharedPreferences = get())
    }

    single(named(AndroidCommonDITags.SHARED_INTERCEPTOR)) {
        Interceptor { chain ->
            val updatedRequest = chain.request().newBuilder()
                .addHeader("User-Agent", get(named(AndroidCommonDITags.USER_AGENT)))
                .build()

            chain.proceed(updatedRequest)
        }
    }

    single<Interceptor>(named(AndroidCommonDITags.LOGGING_INTERCEPTOR)) {
        HttpLoggingInterceptor().apply { setLevel(HttpLoggingInterceptor.Level.BODY) }
    }

    single(named(AndroidCommonDITags.AUTHENTICATOR)) {
        Authenticator { _, response ->
            response.request.run {
                if (Uri.parse(serverUrl).host == this.url.host) {
                    this.newBuilder().url(get<String>(named(AndroidCommonDITags.RELAY_URL))).build()
                } else {
                    null
                }
            }
        }
    }

    single(named(AndroidCommonDITags.OK_HTTP)) {
        OkHttpClient.Builder()
            .addInterceptor(get<Interceptor>(named(AndroidCommonDITags.SHARED_INTERCEPTOR)))
            .authenticator((get(named(AndroidCommonDITags.AUTHENTICATOR))))
            .writeTimeout(networkClientTimeout.timeout, networkClientTimeout.timeUnit)
            .readTimeout(networkClientTimeout.timeout, networkClientTimeout.timeUnit)
            .callTimeout(networkClientTimeout.timeout, networkClientTimeout.timeUnit)
            .connectTimeout(networkClientTimeout.timeout, networkClientTimeout.timeUnit)
            .apply {
                if (BuildConfig.DEBUG) {
                    val loggingInterceptor = get<Interceptor>(named(AndroidCommonDITags.LOGGING_INTERCEPTOR))
                    addInterceptor(loggingInterceptor)
                }
            }
            .retryOnConnectionFailure(true)
            .build()
    }

    single(named(AndroidCommonDITags.MSG_ADAPTER)) { MoshiMessageAdapter.Factory(get<Moshi.Builder>(named(AndroidCommonDITags.MOSHI)).build()) }

    single<ManualConnectionLifecycle>(named(AndroidCommonDITags.MANUAL_CONNECTION_LIFECYCLE)) {
        ManualConnectionLifecycle()
    }

    single<DefaultConnectionLifecycle>(named(AndroidCommonDITags.DEFAULT_CONNECTION_LIFECYCLE)) {
        DefaultConnectionLifecycle(androidApplication())
    }

    single { ConditionalExponentialBackoffStrategy(INIT_BACKOFF_MILLIS, TimeUnit.SECONDS.toMillis(MAX_BACKOFF_SEC), connectionType) }

    single { FlowStreamAdapter.Factory() }

    single(named(AndroidCommonDITags.SCARLET)) {
        Scarlet.Builder()
            .backoffStrategy((get<ConditionalExponentialBackoffStrategy>()))
            .webSocketFactory(get<OkHttpClient>(named(AndroidCommonDITags.OK_HTTP)).newWebSocketFactory(get<String>(named(AndroidCommonDITags.RELAY_URL))))
            .lifecycle(getLifecycle(connectionType))
            .addMessageAdapterFactory(get<MoshiMessageAdapter.Factory>(named(AndroidCommonDITags.MSG_ADAPTER)))
            .addStreamAdapterFactory(get<FlowStreamAdapter.Factory>())
            .build()
    }

    single(named(AndroidCommonDITags.RELAY_SERVICE)) {
        get<Scarlet>(named(AndroidCommonDITags.SCARLET)).create(RelayService::class.java)
    }

    single(named(AndroidCommonDITags.CONNECTIVITY_STATE)) {
        ConnectivityState(androidApplication())
    }
}

private fun Scope.getLifecycle(connectionType: ConnectionType): Lifecycle =
    if (connectionType == ConnectionType.MANUAL) {
        get<ManualConnectionLifecycle>(named(AndroidCommonDITags.MANUAL_CONNECTION_LIFECYCLE))
    } else {
        get<DefaultConnectionLifecycle>(named(AndroidCommonDITags.DEFAULT_CONNECTION_LIFECYCLE))
    }