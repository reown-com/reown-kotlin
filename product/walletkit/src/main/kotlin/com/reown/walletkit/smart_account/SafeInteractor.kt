package com.reown.walletkit.smart_account

import com.reown.android.internal.common.model.ProjectId
import com.reown.android.internal.common.wcKoinApp
import uniffi.uniffi_yttrium.FfiAccountClient
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
        return FfiAccountClient(owner = account.address, chainId = account.reference.toULong(), config = config)
    }
}