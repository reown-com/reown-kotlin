# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :core:android:build
./gradlew :protocol:sign:build
./gradlew :product:walletkit:build

# Run unit tests (all modules)
./gradlew test

# Run unit tests for specific module
./gradlew :protocol:sign:testDebugUnitTest

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Run lint
./gradlew lint

# Build sample apps
./gradlew :sample:wallet:assembleDebug
./gradlew :sample:dapp:assembleDebug
./gradlew :sample:modal:assembleDebug
./gradlew :sample:pos:assembleDebug

# Clean build
./gradlew clean
```

## Architecture Overview

### Layer Structure

The SDK follows a four-layer architecture where each layer depends only on layers below it:

```
┌─────────────────────────────────────────────────────────┐
│  PRODUCT LAYER                                          │
│  product/walletkit/  - High-level wallet SDK            │
│  product/appkit/     - High-level dApp SDK (Compose UI) │
│  product/pay/        - Payment integration              │
│  product/pos/        - Point of Sale                    │
├─────────────────────────────────────────────────────────┤
│  PROTOCOL LAYER                                         │
│  protocol/sign/      - WalletConnect Sign protocol      │
│  protocol/notify/    - Notification protocol            │
├─────────────────────────────────────────────────────────┤
│  CORE LAYER                                             │
│  core/android/       - Android core (relay, pairing)    │
│  core/modal/         - Shared modal UI components       │
│  core/bom/           - Bill of Materials                │
├─────────────────────────────────────────────────────────┤
│  FOUNDATION LAYER                                       │
│  foundation/         - Pure Kotlin/JVM (crypto, JWT)    │
└─────────────────────────────────────────────────────────┘
```

### Key Architectural Patterns

**Singleton with Delegation**: Public SDKs expose singleton objects that delegate to internal protocol implementations:
```kotlin
object SignClient : SignInterface by SignProtocol.instance
object CoreClient : CoreInterface by CoreProtocol.instance
```

**Delegate Pattern**: Event-driven callbacks through delegate interfaces:
- `CoreDelegate` for core events
- `SignClient.WalletDelegate` for wallet-side sign events
- `SignClient.DappDelegate` for dApp-side sign events

**Dependency Injection**: Koin modules organized per layer/feature. Debug builds use local project dependencies; release builds use published Maven artifacts.

**Repository Pattern**: SQLDelight-backed repositories for persistence (sessions, proposals, notifications).

### Database Schema

SQLDelight databases with compile-time schema verification:
- `AndroidCoreDatabase` in core/android
- `SignDatabase` in protocol/sign
- `NotifyDatabase` in protocol/notify

Migrations are strictly validated. Schema files are in `src/main/sqldelight/`.

### Key Entry Points

- `CoreClient.initialize()` - Initialize core relay connection
- `SignClient.initialize()` - Initialize Sign protocol
- `WalletKit.initialize()` - Initialize wallet SDK (wraps Sign)
- `AppKit.initialize()` - Initialize dApp SDK (wraps Sign)

## Testing

Unit tests use JUnit 5 and MockK. Instrumented tests run with AndroidX Test Orchestrator.

```bash
# Single test class
./gradlew :protocol:sign:testDebugUnitTest --tests "com.reown.sign.SomeTest"

# Single test method
./gradlew :protocol:sign:testDebugUnitTest --tests "com.reown.sign.SomeTest.testMethod"
```

## Important Conventions

- Min SDK: 23 (POS module requires 29)
- Target SDK: 35
- Kotlin: 2.2.0
- JSON serialization: Moshi with KSP
- Async: Kotlin Coroutines
- UI (AppKit): Jetpack Compose

Sample apps require `google-services.json` in their `src/` directories and a `secrets.properties` file in the root (see ReadMe.md for setup).
