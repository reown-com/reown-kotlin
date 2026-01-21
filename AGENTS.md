# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

## AI Skills

When writing or modifying Kotlin code in this repository, use the **kotlin-coding** skill located at:
- `.claude/skills/kotlin-coding/SKILL.md` - Core patterns and validation checklist
- `.claude/skills/kotlin-coding/REFERENCE.md` - Extended patterns and examples

These skills encode project-specific conventions including use case patterns, Koin DI setup, coroutines usage, and data modeling.

## Project Overview

**reown-kotlin** is the official Kotlin/Android SDK for WalletConnect and Reown protocols. It enables:

- **Wallets**: Session management, transaction signing, push notifications
- **dApps**: Wallet connections, session requests, blockchain interactions
- **Payments**: Crypto payment links via WalletConnect Pay

**Tech Stack:**
- Kotlin 2.2.0, JVM 11, Min SDK 23, Target SDK 35
- Koin (DI), Moshi + KSP (JSON), SQLDelight (DB)
- Retrofit + OkHttp (HTTP), Scarlet (WebSocket)
- Coroutines 1.10.2, Jetpack Compose (UI)

**License:** WalletConnect Community License

## Repository Structure

```
reown-kotlin/
├── foundation/              # Pure Kotlin/JVM - crypto, JWT, HTTP utilities
├── core/
│   ├── android/             # Android core - relay, pairing, verification
│   ├── modal/               # Shared modal UI components
│   └── bom/                 # Bill of Materials for version management
├── protocol/
│   ├── sign/                # WalletConnect Sign protocol implementation
│   └── notify/              # Notification protocol implementation
├── product/
│   ├── walletkit/           # High-level wallet SDK
│   ├── appkit/              # High-level dApp SDK with Compose UI
│   ├── pay/                 # Payment SDK (standalone, uses Rust/UniFFI)
│   └── pos/                 # Point of Sale application
├── sample/
│   ├── wallet/              # Wallet reference implementation
│   ├── dapp/                # dApp reference implementation
│   ├── modal/               # Modal integration example
│   └── pos/                 # POS example
├── buildSrc/                # Custom Gradle plugins and build logic
├── gradle/
│   ├── libs.versions.toml   # Version catalog (dependencies)
│   ├── proguard-rules/      # ProGuard configurations
│   └── consumer-rules/      # Consumer ProGuard rules
├── .claude/skills/          # AI agent skills for this project
└── docs/                    # Documentation files
```

## Key Commands

### Build Commands

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :core:android:build
./gradlew :protocol:sign:build
./gradlew :product:walletkit:build

# Clean build
./gradlew clean
```

### Testing

```bash
# Run all unit tests
./gradlew test

# Run unit tests for specific module
./gradlew :protocol:sign:testDebugUnitTest

# Run single test class
./gradlew :protocol:sign:testDebugUnitTest --tests "com.reown.sign.SomeTest"

# Run single test method
./gradlew :protocol:sign:testDebugUnitTest --tests "com.reown.sign.SomeTest.testMethod"

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest
```

### Code Quality

```bash
# Run lint
./gradlew lint

# Generate coverage report (JaCoCo)
./gradlew jacocoTestReport
```

### Sample Apps

```bash
# Build sample apps
./gradlew :sample:wallet:assembleDebug
./gradlew :sample:dapp:assembleDebug
./gradlew :sample:modal:assembleDebug
./gradlew :sample:pos:assembleDebug
```

Sample apps require:
- `google-services.json` in their `src/` directories
- `secrets.properties` file in root (see ReadMe.md)

## Architecture Overview

### Layer Structure

The SDK follows a four-layer architecture where each layer depends only on layers below it:

```
┌─────────────────────────────────────────────────────────┐
│  PRODUCT LAYER                                          │
│  product/walletkit/  - High-level wallet SDK            │
│  product/appkit/     - High-level dApp SDK (Compose UI) │
│  product/pay/        - Payment integration (standalone) │
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
object WalletKit : WalletInterface by WalletKitProtocol.instance
```

**Delegate Pattern**: Event-driven callbacks through delegate interfaces:
- `CoreDelegate` for core events
- `SignClient.WalletDelegate` for wallet-side sign events
- `SignClient.DappDelegate` for dApp-side sign events

**Dependency Injection (Koin)**: Modular DI with functional DSL:
```kotlin
fun featureModule() = module {
    includes(coreModule())
    single<UseCaseInterface> { UseCase(get(), get()) }
}
```

**Use Case Pattern**: Single-responsibility business logic:
```kotlin
internal class FeatureUseCase(...) : FeatureUseCaseInterface {
    override suspend fun execute(): Result<T> = supervisorScope {
        runCatching { ... }
    }
}
```

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

## Development Notes

### Coding Conventions

- **Visibility**: Use `internal` by default for SDK internals
- **JVM Interop**: Add `@file:JvmSynthetic` to hide Kotlin-only APIs from Java
- **Async**: Use `supervisorScope` + `runCatching` in use cases
- **State**: Private `_mutableState` → public `state.asStateFlow()`
- **Naming**: `*UseCase`, `*Repository`, `*DTO`, `*DO` suffixes
- **DI Tags**: Use enum-based qualifiers (e.g., `SignDITags.HTTP_CLIENT`)

### Testing Conventions

- **Framework**: JUnit 4, Mockk, Robolectric
- **Coroutines**: Use `StandardTestDispatcher` + `runTest` + `advanceUntilIdle()`
- **Naming**: Backtick test names: `` `should return success when valid` ``

### Lint Configuration

- Lint errors abort the build (`abortOnError = true`)
- Warnings are ignored (`ignoreWarnings = true`)
- No ktlint/detekt - uses Android Lint only

### Important Constraints

- Min SDK: 23 (POS module requires 29)
- Target SDK: 35
- Kotlin: 2.2.0
- JVM Target: 11
- Compose Compiler: 1.6.0

## Versioning and Publishing

### Version Management

Versions are centralized in `buildSrc/src/main/kotlin/Versions.kt`:

```kotlin
const val BOM_VERSION = "1.5.2"
const val FOUNDATION_VERSION = "1.5.2"
const val CORE_VERSION = "1.5.2"
const val SIGN_VERSION = "1.5.2"
// ... all modules share the same version
```

Version catalog for dependencies: `gradle/libs.versions.toml`

### Publishing to Maven Central

**Repository**: Maven Central via Sonatype OSSRH

**Artifacts published**:
| Module | Group | Artifact ID |
|--------|-------|-------------|
| Foundation | com.reown | foundation |
| Core | com.reown | android-core |
| Sign | com.reown | sign |
| Notify | com.reown | notify |
| WalletKit | com.reown | walletkit |
| AppKit | com.reown | appkit |
| Modal Core | com.reown | modal-core |
| BOM | com.reown | android-bom |

**Publishing commands**:
```bash
# Publish to staging
./gradlew publishToSonatype

# Close and release staging repository
./gradlew closeAndReleaseSonatypeStagingRepository

# Bump versions (custom task)
./gradlew bumpVersion -PnewVersion=1.6.0
```

**Required environment variables**:
- `CENTRAL_PORTAL_USERNAME` - Sonatype username
- `CENTRAL_PORTAL_PASSWORD` - Sonatype password
- `SIGNING_KEY_ID` - GPG key ID
- `SIGNING_KEY` - GPG private key (armored)
- `SIGNING_PASSWORD` - GPG key passphrase

**Publication includes**:
- Release AAR/JAR
- Sources JAR (`sourcesJar`)
- Javadoc JAR (`javadocJar` via Dokka)
- POM with license, developer, and SCM information

### Version Bumping Workflow

1. Update versions in `buildSrc/src/main/kotlin/Versions.kt`
2. Run `./gradlew build` to verify
3. Commit with message: `chore: bump version to X.Y.Z`
4. Tag release: `git tag vX.Y.Z`
5. Push tag to trigger CI publish

## WalletConnectPay Module

### Overview

`WalletConnectPay` is a standalone payment SDK in `product/pay/` that enables crypto payments via payment links. It uses a Rust backend via UniFFI bindings (`yttrium-wcpay`). Unlike other SDKs, it has no dependencies on Core, Sign, or WalletKit.

**Key files:**
- `product/pay/src/main/java/com/walletconnect/pay/WalletConnectPay.kt` - Main SDK singleton
- `product/pay/src/main/java/com/walletconnect/pay/Pay.kt` - Data models
- `product/pay/src/main/java/com/walletconnect/pay/Mappers.kt` - UniFFI type mappings

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

### Sample Implementation

See `sample/wallet/src/main/kotlin/com/reown/sample/wallet/`:
- `PaymentViewModel.kt` - Complete payment flow with StateFlow
- `PaymentRoute.kt` - Jetpack Compose UI
