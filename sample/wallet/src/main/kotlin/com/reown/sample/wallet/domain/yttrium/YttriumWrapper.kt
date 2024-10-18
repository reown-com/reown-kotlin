package com.reown.sample.wallet.domain.yttrium


import com.reown.sample.wallet.domain.EthAccountDelegate
import uniffi.uniffi_yttrium.AccountClient
import uniffi.uniffi_yttrium.AccountClientConfig
import uniffi.yttrium.Config
import uniffi.yttrium.Endpoint
import uniffi.yttrium.Endpoints


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