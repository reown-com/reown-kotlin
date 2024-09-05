package com.reown.appkit.client.models

import com.reown.android.internal.common.exception.WalletConnectException

class AppKitClientAlreadyInitializedException : WalletConnectException("AppKit already initialized")
class CoinbaseClientAlreadyInitializedException : WalletConnectException("Coinbase already initialized")