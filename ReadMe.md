# **Reown - Kotlin**

The communications protocol for web3, Reown brings the ecosystem together by enabling hundreds of wallets and apps to securely connect and interact. This repository contains Kotlin implementation of
Reown protocols for Android applications.

## Overview

This SDK enables blockchain wallet functionality and wallet-to-dApp communication for Android applications.


## BOM Instructions:

To help manage compatible dependencies stay in sync, we've introduced a [BOM](https://docs.gradle.org/current/userguide/platforms.html#sub:bom_import) to the Kotlin SDK. With this, you only need to
update the BOM version to get the latest SDKs. Just add the BOM as a dependency and then list the SDKs you want to include into your project.

### example build.gradle.kts

```kotlin
dependencies {
    implementation(platform("com.reown:android-bom:{BOM version}"))
    implementation("com.reown:android-core")
    implementation("com.reown:walletkit")
}
```

## SDK Chart

| BOM   | [Core SDK](core/android) | [Sign SDK](protocol/sign) | [WalletKit](product/walletkit) | [AppKit](product/appkit) |
|-------|--------------------------|---------------------------|--------------------------------|--------------------------|
| 1.0.0 | 1.0.0                    | 1.0.0                     | 1.0.0                          | 1.0.0                    |

## License

Reown is released under the Apache 2.0 license. [See LICENSE](/LICENSE) for details.
