package com.walletconnect.pay

import uniffi.yttrium_wcpay.SdkConfig
import uniffi.yttrium_wcpay.WalletConnectPay

object WalletConnectPay {

    fun test() {
        val client = WalletConnectPay("", SdkConfig("", "", "", ""))

    }
}