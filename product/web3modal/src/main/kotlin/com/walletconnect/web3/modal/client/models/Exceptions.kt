package com.walletconnect.web3.modal.client.models

import com.walletconnect.android.internal.common.exception.WalletConnectException

class AppKitClientAlreadyInitializedException : WalletConnectException("AppKit already initialized")
class CoinbaseClientAlreadyInitializedException : WalletConnectException("Coinbase already initialized")