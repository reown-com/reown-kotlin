---
name: kotlin-coding
description: Writes high-quality Kotlin code following official conventions and reown-kotlin project patterns. Use when writing, reviewing, or refactoring Kotlin code in this codebase.
---

# Kotlin Coding Skill

## Goal
Write production-quality Kotlin code that follows official Kotlin conventions and reown-kotlin project-specific patterns.

## When to use
- Writing new Kotlin classes, functions, or modules
- Implementing features in Android/Kotlin codebase
- Refactoring existing Kotlin code
- Code reviews for Kotlin files

## When not to use
- Non-Kotlin codebases (Java-only, JavaScript, etc.)
- Configuration files (gradle, yaml, xml)
- Documentation-only tasks

## Project Context: reown-kotlin

**Stack:**
- Kotlin 2.2.0, JVM 11, Min SDK 23
- Koin (DI), Moshi (JSON), SQLDelight (DB), Retrofit/OkHttp (HTTP)
- Coroutines 1.10.2, Compose UI

**Architecture:** Layered Clean Architecture
```
Foundation (pure Kotlin) → Core (Android) → Protocol → Product
```

## Default Workflow

1. **Understand context** - Read existing code in the module
2. **Follow existing patterns** - Match the module's conventions
3. **Write minimal code** - Only what's needed for the task
4. **Add tests** - Match existing test patterns
5. **Validate** - Run through checklist below

## Core Patterns

### Visibility & Interop
```kotlin
// Hide internal APIs from Java consumers
@file:JvmSynthetic

package com.reown.module.internal

@JvmSynthetic
internal fun internalHelper(): String = "..."
```

### Use Case Pattern
```kotlin
internal class ApproveSessionUseCase(
    private val repository: SessionRepository,
    private val validator: SessionValidator
) : ApproveSessionUseCaseInterface {

    override suspend fun approve(params: ApproveParams): Result<Session> =
        supervisorScope {
            runCatching {
                validator.validate(params)
                repository.save(params.toSession())
            }
        }
}
```

### Dependency Injection (Koin)
```kotlin
// Module function pattern
fun featureModule() = module {
    includes(coreModule())

    single<FeatureRepository> { FeatureRepositoryImpl(get()) }
    single<FeatureUseCaseInterface> { FeatureUseCase(get(), get()) }
}

// Enum-based qualifiers
enum class FeatureDITags { HTTP_CLIENT, JSON_ADAPTER }

// Usage
single(named(FeatureDITags.HTTP_CLIENT)) { OkHttpClient() }
```

### Coroutines
```kotlin
// Scope definition
private val job = SupervisorJob()
val scope = CoroutineScope(job + Dispatchers.IO)

// State management
private val _state = MutableStateFlow<UiState>(UiState.Loading)
val state: StateFlow<UiState> = _state.asStateFlow()

// Context switching for IO
suspend fun loadData(): Data = withContext(Dispatchers.IO) {
    repository.fetch()
}
```

### Error Handling
```kotlin
// Custom exception hierarchy
abstract class WalletConnectException(override val message: String?) : Exception(message)

class InvalidSessionException(message: String) : WalletConnectException(message)

// Result pattern
suspend fun fetchData(): Result<Data> = runCatching {
    api.getData()
}

// Usage
fetchData()
    .onSuccess { data -> process(data) }
    .onFailure { error -> log(error) }
```

### Data Modeling
```kotlin
// Sealed class hierarchy for type-safe ADTs
sealed class ConnectionState {
    data object Connected : ConnectionState()
    data object Disconnected : ConnectionState()
    data class Error(val reason: Reason) : ConnectionState()

    sealed class Reason {
        data class Network(val code: Int) : Reason()
        data class Auth(val message: String) : Reason()
    }
}

// Value classes for type safety
@JvmInline
value class Topic(val value: String)

@JvmInline
value class SessionId(val value: String)

// Data class with Moshi
@JsonClass(generateAdapter = true)
data class SessionRequest(
    @Json(name = "id") val id: String,
    @Json(name = "topic") val topic: String,
    @Json(name = "params") val params: RequestParams? = null
)
```

### Mapper Extensions
```kotlin
@JvmSynthetic
internal fun SessionDTO.toDomain(): Session = Session(
    id = SessionId(this.id),
    topic = Topic(this.topic),
    expiry = this.expiry
)

@JvmSynthetic
internal fun Session.toDTO(): SessionDTO = SessionDTO(
    id = this.id.value,
    topic = this.topic.value,
    expiry = this.expiry
)
```

### Interface + Delegation
```kotlin
// Public API delegates to internal implementation
object SignClient : SignInterface by SignProtocol.instance

// Interface definition
interface SignInterface {
    fun initialize(params: InitParams)
    suspend fun connect(params: ConnectParams): Result<Uri>
    fun setDelegate(delegate: SignDelegate)
}

// Delegate callbacks
interface SignDelegate {
    fun onSessionProposal(proposal: SessionProposal)
    fun onSessionRequest(request: SessionRequest)
    val onSessionDelete: ((DeletedSession) -> Unit)? get() = null
}
```

## Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Use Case | `*UseCase` | `ApproveSessionUseCase` |
| Interface | `*Interface` | `SignInterface` |
| Repository | `*Repository` | `SessionStorageRepository` |
| DTO | `*DTO` | `SessionDTO` |
| Domain Object | `*DO` | `EngineDO` |
| DI Tags | `*DITags` enum | `SignDITags` |
| Mapper | `to*()` extension | `toSession()`, `toDomain()` |
| State | `_private` → `public` | `_state` → `state` |

## Testing Pattern
```kotlin
class FeatureUseCaseTest {
    private val repository: FeatureRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var useCase: FeatureUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        useCase = FeatureUseCase(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invoke returns success when repository succeeds`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.fetch() } returns expectedData

        // When
        val result = useCase.invoke()
        advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.fetch() }
    }
}
```

## Validation Checklist

- [ ] Uses `internal` visibility for non-public APIs
- [ ] `@file:JvmSynthetic` on internal utility files
- [ ] Use cases wrap logic in `supervisorScope` + `runCatching`
- [ ] StateFlow follows `_private` → `public asStateFlow()` pattern
- [ ] Koin modules use functional DSL: `fun module() = module { }`
- [ ] Data classes annotated with `@JsonClass(generateAdapter = true)` for Moshi
- [ ] Value classes used for type-safe identifiers (Topic, SessionId)
- [ ] Mapper functions use `to*()` naming with `@JvmSynthetic`
- [ ] Tests use `mockk`, `runTest`, and `StandardTestDispatcher`
- [ ] No hardcoded strings for DI - use enum tags

## Examples

### Example 1: New Use Case

**Task:** Create a use case to fetch user sessions

```kotlin
@file:JvmSynthetic

package com.reown.sign.engine.use_case.calls

internal class GetUserSessionsUseCase(
    private val sessionRepository: SessionStorageRepository,
    private val logger: Logger
) : GetUserSessionsUseCaseInterface {

    override suspend fun getSessions(userId: String): Result<List<EngineDO.Session>> =
        supervisorScope {
            runCatching {
                sessionRepository.getSessionsByUser(userId)
                    .map { it.toEngineDO() }
            }.onFailure { error ->
                logger.error("Failed to fetch sessions: ${error.message}")
            }
        }
}

internal interface GetUserSessionsUseCaseInterface {
    suspend fun getSessions(userId: String): Result<List<EngineDO.Session>>
}
```

### Example 2: Koin Module Registration

**Task:** Register the new use case in DI

```kotlin
// In CallsModule.kt
fun callsModule() = module {
    includes(storageModule())

    single<GetUserSessionsUseCaseInterface> {
        GetUserSessionsUseCase(
            sessionRepository = get(),
            logger = get()
        )
    }
}
```

### Example 3: Sealed Class for Events

**Task:** Model wallet events

```kotlin
@file:JvmSynthetic

package com.reown.walletkit.client.model

sealed class WalletEvent {
    data class SessionProposal(
        val id: ProposalId,
        val proposer: AppMetaData,
        val namespaces: Map<String, Namespace>
    ) : WalletEvent()

    data class SessionRequest(
        val topic: Topic,
        val chainId: String,
        val request: Request
    ) : WalletEvent()

    data class SessionDelete(
        val topic: Topic,
        val reason: String
    ) : WalletEvent()

    @JvmInline
    value class ProposalId(val value: Long)
}
```
