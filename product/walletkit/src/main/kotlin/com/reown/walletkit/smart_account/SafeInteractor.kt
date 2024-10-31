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
        val (namespace: String, reference: String, address: String) = owner.split(":")
        val pimlicoUrl = "https://api.pimlico.io/v2/$reference/rpc?apikey=$pimlicoApiKey"
        val endpoints = Endpoints(
            rpc = Endpoint(baseUrl = "https://rpc.walletconnect.com/v1?chainId=$namespace:$reference&projectId=$projectId", apiKey = ""),
            bundler = Endpoint(baseUrl = pimlicoUrl, apiKey = ""), //todo: remove apiKet from bindings
            paymaster = Endpoint(baseUrl = pimlicoUrl, apiKey = ""),
        )
        val config = Config(endpoints)

        val accountConfig = AccountClientConfig(
            ownerAddress = address,
            chainId = reference.toULong(),
            config = config,
            privateKey = "ff89825a799afce0d5deaa079cdde227072ec3f62973951683ac8cc033092156", //todo: remove sign service
            safe = true,
            signerType = "PrivateKey" //todo: remove sign service
        )
        println("kobe: init Safe Account")
        return AccountClient(accountConfig)
    }
}