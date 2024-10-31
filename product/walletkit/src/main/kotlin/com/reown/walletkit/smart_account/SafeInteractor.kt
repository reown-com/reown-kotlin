package com.reown.walletkit.smart_account

import com.reown.android.internal.common.model.ProjectId
import com.reown.android.internal.common.wcKoinApp
import uniffi.uniffi_yttrium.AccountClient
import uniffi.uniffi_yttrium.AccountClientConfig
import uniffi.yttrium.Config
import uniffi.yttrium.Endpoint
import uniffi.yttrium.Endpoints

//todo: use Object instead of class?
class SafeInteractor(private val pimlicoApiKey: String) {
    private val projectId: String = wcKoinApp.koin.get<ProjectId>().value
    private val ownerToAccountClient = mutableMapOf<String, AccountClient>()

    fun getOrCreate(owner: String): AccountClient =
        if (ownerToAccountClient.containsKey(owner)) {
            ownerToAccountClient[owner]!!
        } else {
            val safeAccount = createSafeAccount(owner)
            ownerToAccountClient[owner] = safeAccount
            safeAccount
        }


    private fun createSafeAccount(owner: String): AccountClient {
        val (namespace: String, reference: String, _) = owner.split(":")
        val pimlicoUrl = "https://api.pimlico.io/v2/$reference/rpc?apikey=$pimlicoApiKey"
        val endpoints = Endpoints(
            rpc = Endpoint(baseUrl = "https://rpc.walletconnect.com/v1?chainId=$namespace:$reference&projectId=$projectId", apiKey = ""),
            bundler = Endpoint(baseUrl = pimlicoUrl, apiKey = ""), //todo: remove apiKet from bindings
            paymaster = Endpoint(baseUrl = pimlicoUrl, apiKey = ""),
        )
        val config = Config(endpoints)

        val accountConfig = AccountClientConfig(
            ownerAddress = owner,
            chainId = reference.toULong(),
            config = config,
            privateKey = "", //todo: remove
            safe = true,
            signerType = "PrivateKey" //todo: remove
        )
        return AccountClient(accountConfig)
    }
}