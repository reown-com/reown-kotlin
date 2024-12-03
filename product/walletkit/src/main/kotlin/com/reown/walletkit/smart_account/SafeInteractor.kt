package com.reown.walletkit.smart_account

import com.reown.android.internal.common.model.ProjectId
import com.reown.android.internal.common.wcKoinApp
import uniffi.uniffi_yttrium.FfiAccountClient
import uniffi.uniffi_yttrium.FfiAccountClientConfig
import uniffi.yttrium.Config
import uniffi.yttrium.Endpoint
import uniffi.yttrium.Endpoints

class SafeInteractor(private val pimlicoApiKey: String) {
    private val projectId: String = wcKoinApp.koin.get<ProjectId>().value
    private val ownerToAccountClient = mutableMapOf<String, FfiAccountClient>()

    fun getOrCreate(account: Account): FfiAccountClient {
        return if (ownerToAccountClient.containsKey(account.owner)) {
            ownerToAccountClient[account.owner]!!
        } else {
            val safeAccount = createSafeAccount(account)
            ownerToAccountClient[account.owner] = safeAccount
            safeAccount
        }
    }

    private fun createSafeAccount(account: Account): FfiAccountClient {
        val pimlicoUrl = "https://api.pimlico.io/v2/${account.reference}/rpc?apikey=$pimlicoApiKey"
        val endpoints = Endpoints(
            rpc = Endpoint(baseUrl = "https://rpc.walletconnect.com/v1?chainId=${account.namespace}:${account.reference}&projectId=$projectId", apiKey = ""),
            bundler = Endpoint(baseUrl = pimlicoUrl, apiKey = ""), //todo: remove apiKet from bindings
            paymaster = Endpoint(baseUrl = pimlicoUrl, apiKey = ""),
        )
        val config = Config(endpoints)
        val accountConfig = FfiAccountClientConfig(
            ownerAddress = account.address,
            chainId = account.reference.toULong(),
            config = config,
            privateKey = "ff89825a799afce0d5deaa079cdde227072ec3f62973951683ac8cc033092156", //todo: remove sign service
            safe = true,
            signerType = "PrivateKey" //todo: remove sign service
        )
        return FfiAccountClient(accountConfig)
    }
}