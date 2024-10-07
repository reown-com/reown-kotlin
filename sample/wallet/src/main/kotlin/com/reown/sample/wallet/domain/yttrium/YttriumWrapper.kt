package com.reown.sample.wallet.domain.yttrium

import com.reown.android.internal.common.scope
import com.reown.kotlin.ffi.yttrium.AccountClient
import com.reown.kotlin.ffi.yttrium.AccountClientConfig
import com.reown.kotlin.ffi.yttrium.Config
import com.reown.kotlin.ffi.yttrium.Endpoint
import com.reown.kotlin.ffi.yttrium.Endpoints
import com.reown.sample.wallet.domain.EthAccountDelegate
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

lateinit var accountClient: AccountClient

private const val host: String = "192.168.18.137"

private val endpoints = Endpoints(
    rpc = Endpoint(baseUrl = "http://$host:8545", apiKey = ""),
    bundler = Endpoint(baseUrl = "http://$host:4337", apiKey = ""),
    paymaster = Endpoint(baseUrl = "http://$host:3000", apiKey = ""),
)

private val config = Config(endpoints)

val accountConfig = AccountClientConfig(
    ownerAddress = EthAccountDelegate.account,
    chainId = 1u,
    config = config,
    privateKey = EthAccountDelegate.privateKey,
    safe = true,
    signerType = "PrivateKey"
)

var smartAccountAddress: String = ""