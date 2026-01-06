# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

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
- `WalletConnectPay.initialize()` - Initialize payment SDK (standalone)

## WalletConnectPay Module

### Overview

`WalletConnectPay` is a standalone payment SDK in `product/pay/` that enables crypto payments via payment links. It uses a Rust backend via UniFFI bindings (`yttrium-wcpay`). Unlike other SDKs, it has no dependencies on Core, Sign, or WalletKit.

**Key files:**
- `product/pay/src/main/java/com/walletconnect/pay/WalletConnectPay.kt` - Main SDK singleton
- `product/pay/src/main/java/com/walletconnect/pay/Pay.kt` - Data models
- `product/pay/src/main/java/com/walletconnect/pay/Mappers.kt` - UniFFI type mappings

### Initialization

```kotlin
WalletConnectPay.initialize(
    Pay.SdkConfig(
        apiKey = "your-api-key",
        sdkName = "kotlin-walletconnect-pay",
        sdkVersion = PayBuildConfig.SDK_VERSION,
        sdkPlatform = "android"
    )
)
```

### Payment Flow

```
1. getPaymentOptions(paymentLink, accounts)
   → Returns: merchant info, payment options, optional data collection fields

2. [If collectDataAction exists] Collect user data (TEXT/DATE fields)

3. getRequiredPaymentActions(paymentId, optionId)
   → Returns: list of WalletRpcAction to sign (eth_signTypedData_v4, personal_sign)

4. Sign each action with wallet's private key

5. confirmPayment(paymentId, optionId, signatures, collectedData)
   → Returns: PaymentStatus (SUCCEEDED, PROCESSING, FAILED, EXPIRED)
```

### Public API

```kotlin
object WalletConnectPay {
    fun initialize(config: Pay.SdkConfig)
    val isInitialized: Boolean

    suspend fun getPaymentOptions(
        paymentLink: String,
        accounts: List<String>  // CAIP-10 format: "eip155:1:0x..."
    ): Result<Pay.PaymentOptionsResponse>

    suspend fun getRequiredPaymentActions(
        paymentId: String,
        optionId: String
    ): Result<List<Pay.RequiredAction>>

    suspend fun confirmPayment(
        paymentId: String,
        optionId: String,
        signatures: List<String>,
        collectedData: List<Pay.CollectDataFieldResult>? = null
    ): Result<Pay.ConfirmPaymentResponse>

    fun shutdown()
}
```

### Key Data Models

```kotlin
// Payment status
enum class Pay.PaymentStatus {
    REQUIRES_ACTION, PROCESSING, SUCCEEDED, FAILED, EXPIRED
}

// Required signing action
sealed class Pay.RequiredAction {
    data class WalletRpc(val action: WalletRpcAction) : RequiredAction()
}

data class Pay.WalletRpcAction(
    val chainId: String,
    val method: String,    // "eth_signTypedData_v4" or "personal_sign"
    val params: String     // JSON parameters
)

// Data collection for information capture
data class Pay.CollectDataField(
    val id: String,
    val name: String,
    val fieldType: CollectDataFieldType,  // TEXT or DATE
    val required: Boolean
)
```

### Error Handling

All methods return `Result<T>`. Error types are sealed classes:
- `GetPaymentOptionsError` - InvalidPaymentLink, PaymentExpired, PaymentNotFound, etc.
- `GetPaymentRequestError` - OptionNotFound, PaymentNotFound, etc.
- `ConfirmPaymentError` - InvalidSignature, RouteExpired, etc.
- `PayError` - Http, Api, Timeout

### Sample Implementation

See `sample/wallet/src/main/kotlin/com/reown/sample/wallet/`:
- `PaymentViewModel.kt` - Complete payment flow with StateFlow
- `PaymentRoute.kt` - Jetpack Compose UI

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
