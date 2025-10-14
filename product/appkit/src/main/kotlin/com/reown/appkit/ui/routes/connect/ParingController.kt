package com.reown.appkit.ui.routes.connect

import com.reown.android.internal.common.wcKoinApp
import com.reown.appkit.client.Modal
import com.reown.appkit.client.toModel
import com.reown.appkit.engine.AppKitEngine

internal interface ParingController {

    fun connect(
        name: String, method: String,
        sessionParams: Modal.Params.SessionParams,
        onSuccess: (uri: String) -> Unit,
        onError: (Throwable) -> Unit
    )

    fun authenticate(
        name: String, method: String,
        authParams: Modal.Model.AuthPayloadParams,
        walletAppLink: String? = null,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit
    )

    val pairingUri: String
}

internal class PairingControllerImpl : ParingController {

    private val appKitEngine: AppKitEngine = wcKoinApp.koin.get()

    private var _uri: String? = null

    private val uri: String
        get() = _uri ?: ""

    override fun connect(
        name: String, method: String,
        sessionParams: Modal.Params.SessionParams,
        onSuccess: (uri: String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
//            generatePairing()
            val connectParams = Modal.Params.ConnectParams(
                sessionParams.optionalNamespaces,
                sessionParams.properties,
                sessionParams.scopedProperties,
            )

            appKitEngine.connectWC(
                name = name, method = method,
                connect = connectParams,
                onSuccess = { uri ->
                    _uri = uri
                    onSuccess(uri)
                },
                onError = onError
            )
        } catch (e: Exception) {
            onError(e)
        }
    }

    override fun authenticate(
        name: String,
        method: String,
        authParams: Modal.Model.AuthPayloadParams,
        walletAppLink: String?,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
//            generateAuthenticatedPairing()
            appKitEngine.authenticate(
                name = name,
                method = method,
                authenticate = authParams.toModel(""), //TODO: revisit 1CA
                walletAppLink = walletAppLink,
                onSuccess = { uri ->
                    _uri = uri
                    onSuccess(uri)
                },
                onError = onError
            )
        } catch (e: Exception) {
            onError(e)
        }
    }

    override val pairingUri: String
        get() = uri

//    private fun generatePairing(): Core.Model.Pairing =
//        CoreClient.Pairing.create { error -> throw IllegalStateException("Creating Pairing failed: ${error.throwable.stackTraceToString()}") }!!.also { _pairing = it }
//
//
//    private fun generateAuthenticatedPairing(): Core.Model.Pairing =
//        CoreClient.Pairing
//            .create(methods = "wc_sessionAuthenticate", onError = { error -> throw IllegalStateException("Creating Pairing failed: ${error.throwable.stackTraceToString()}") })!!
//            .also { _pairing = it }

}
