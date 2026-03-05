# **Reown - Kotlin**

The communications protocol for web3, Reown brings the ecosystem together by enabling hundreds of wallets and apps to securely connect and interact. This repository contains Kotlin implementation of
Reown protocols for Android applications.

## Overview

The Reown Kotlin SDK is a comprehensive mobile development kit that enables blockchain wallet functionality and wallet-to-dApp communication for Android applications. It implements the WalletConnect protocol suite, allowing mobile wallets to securely connect with decentralized applications (dApps).

### Key Features

- **Wallet Implementation**: Manage crypto accounts and transactions
- **Sign Protocol**: Secure connection between wallets and dApps
- **Notify Protocol**: Blockchain-related notifications
- **Chain Abstraction**: Simplified cross-chain transactions
- **Authentication**: Wallet verification mechanisms

### Core Components

- **WalletKit**: SDK for building wallet applications with full blockchain capabilities
- **AppKit**: SDK for building dApps that connect to wallets
- **Protocol Layer**: Implementations of Sign, Notify, and Auth protocols

## Project Structure

The Reown Kotlin SDK is organized as a modular system with several layers:

1. **Foundation Layer**: Base libraries providing core functionality
   - `foundation/`: Fundamental libraries and utilities

2. **Core Layer**: Essential platform-specific implementations
   - `core/android/`: Core Android implementation
   - `core/modal/`: Core modal UI components
   - `core/bom/`: Bill of Materials for dependency version management

3. **Protocol Layer**: Protocol-specific implementations
   - `protocol/sign/`: Implementation of the WalletConnect Sign protocol
   - `protocol/notify/`: Implementation of the WalletConnect Notify protocol

4. **Product Layer**: High-level SDK features
   - `product/walletkit/`: SDK for building wallet applications
   - `product/appkit/`: SDK for building dApp applications

5. **Sample Applications**: Example implementations
   - `sample/wallet/`: Wallet sample application
   - `sample/dapp/`: dApp sample application
   - `sample/pos/`: POS sample application
   - `sample/modal/`: Modal sample application

## Installation

To integrate the Reown Kotlin SDK into your project, use the Bill of Materials (BOM) to manage compatible dependencies:

### Gradle Setup (build.gradle.kts)

```kotlin
dependencies {
    implementation(platform("com.reown:android-bom:{BOM version}"))

    // Core SDK
    implementation("com.reown:android-core")

    // For wallet applications
    implementation("com.reown:walletkit")

    // For dApp applications
    implementation("com.reown:appkit")
}
```

## Building Sample Applications

The repository includes several sample applications that demonstrate different use cases of the Reown Kotlin SDK. Follow these instructions to build and run the sample apps:

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK API level 21 or higher
- Gradle 7.0 or later
- JDK 11 or later

### Setup Steps

No manual setup is needed for debug builds. Mock `secrets.properties` and `google-services.json` files are auto-generated on first build if they are missing.

For production or internal builds, provide real configuration:
- `secrets.properties` in the root directory with valid keystore paths and passwords
- `google-services.json` in each sample app directory with real Firebase credentials

### Available Sample Apps

#### Wallet Sample (`sample/wallet/`)
- **Purpose**: Demonstrates a complete wallet implementation
- **Package**: `com.reown.sample.wallet`
- **Build Command**: `./gradlew :sample:wallet:assembleDebug`

#### dApp Sample (`sample/dapp/`)
- **Purpose**: Shows how to build a dApp that connects to wallets
- **Package**: `com.reown.sample.dapp`
- **Build Command**: `./gradlew :sample:dapp:assembleDebug`

#### POS Sample (`sample/pos/`)
- **Purpose**: Point of Sale application example
- **Package**: `com.reown.sample.pos`
- **Build Command**: `./gradlew :sample:pos:assembleDebug`

#### Modal Sample (`sample/modal/`)
- **Purpose**: Modal UI integration example
- **Package**: `com.reown.sample.modal`
- **Build Command**: `./gradlew :sample:modal:assembleDebug`

### Build Commands

- **Build all samples**: `./gradlew :sample:assembleDebug`
- **Build specific sample**: `./gradlew :sample:{sample_name}:assembleDebug`
- **Install on device**: `./gradlew :sample:{sample_name}:installDebug`
- **Run tests**: `./gradlew :sample:{sample_name}:testDebugUnitTest`

### Troubleshooting

- **Build errors**: Ensure all dependencies are synced with `./gradlew build`
- **Keystore issues**: Delete `secrets.properties` and rebuild to regenerate mock values, or provide real keystore config
- **Google Services**: Delete `google-services.json` from the sample directory and rebuild to regenerate, or provide real Firebase config
- **Gradle sync**: Try `./gradlew clean` followed by `./gradlew build`
