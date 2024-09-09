# **WalletKit - Kotlin**

Kotlin implementation of WalletKit for Android applications.

![Maven Central](https://img.shields.io/maven-central/v/com.walletconnect/web3wallet)

## Requirements

* Android min SDK 23
* Java 11

## Documentation and usage
* [WalletKit installation](https://docs.walletconnect.com/2.0/kotlin/web3wallet/installation)
* [WalletKit usage](https://docs.walletconnect.com/2.0/kotlin/web3wallet/wallet-usage)

## Installation

root/build.gradle.kts:

```gradle
allprojects {
 repositories {
    mavenCentral()
    maven { url "https://jitpack.io" }
 }
}
```

app/build.gradle.kts

```gradle
implementation(platform("com.reown:android-bom:{BOM version}"))
implementation("com.reown:android-core")
implementation("com.reown:walletkit")
```