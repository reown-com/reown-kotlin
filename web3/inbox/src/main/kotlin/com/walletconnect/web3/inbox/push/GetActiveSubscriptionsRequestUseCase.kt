package com.walletconnect.web3.inbox.push

import com.walletconnect.android.Core
import com.walletconnect.push.common.Push
import com.walletconnect.push.wallet.client.PushWalletInterface
import com.walletconnect.web3.inbox.json_rpc.Web3InboxParams
import com.walletconnect.web3.inbox.json_rpc.Web3InboxRPC
import com.walletconnect.web3.inbox.proxy.ProxyInteractor
import com.walletconnect.web3.inbox.proxy.PushProxyInteractor
import com.walletconnect.web3.inbox.proxy.request.RequestUseCase

internal class GetActiveSubscriptionsRequestUseCase(
    private val pushWalletClient: PushWalletInterface,
    proxyInteractor: PushProxyInteractor,
) : RequestUseCase<Web3InboxParams.Request.Empty>(proxyInteractor) {

    override fun invoke(rpc: Web3InboxRPC, params: Web3InboxParams.Request.Empty) =
        respondWithResult(rpc, pushWalletClient.getActiveSubscriptions().toResult())

    private fun Map<String, Push.Model.Subscription>.toResult(): Map<String, Web3InboxParams.Response.Push.GetActiveSubscriptionsResult> =
        map { it.key to it.value.toResult() }.toMap()

    private fun Push.Model.Subscription.toResult(): Web3InboxParams.Response.Push.GetActiveSubscriptionsResult =
        Web3InboxParams.Response.Push.GetActiveSubscriptionsResult(requestId, topic, account, relay.toResult(), metadata.toResult())

    private fun Push.Model.Subscription.Relay.toResult(): Web3InboxParams.Response.Push.GetActiveSubscriptionsResult.Relay =
        Web3InboxParams.Response.Push.GetActiveSubscriptionsResult.Relay(protocol, data)

    private fun Core.Model.AppMetaData.toResult(): Web3InboxParams.Response.Push.GetActiveSubscriptionsResult.AppMetaData =
        Web3InboxParams.Response.Push.GetActiveSubscriptionsResult.AppMetaData(name, description, url, icons, redirect, verifyUrl)
}