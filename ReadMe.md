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
- **Chain Support**: Multi-chain support for Ethereum, Solana, and Layer 2 networks

### Supported Chains

The SDK supports multiple blockchain networks including Ethereum, Solana, and various Layer 2 chains.



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



## Sample Applications

The repository includes sample applications to demonstrate SDK usage:

- **Wallet Sample**: Demonstrates how to build a wallet application using WalletKit
- **dApp Sample**: Shows how to build a dApp that connects to wallets using AppKit

Check the `sample/` directory for complete implementations.

## License

Reown is released under the Apache 2.0 license. [See LICENSE](/LICENSE) for details.
