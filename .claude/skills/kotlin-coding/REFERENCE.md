# Kotlin Coding Reference

Extended patterns and examples for reown-kotlin development.

## File Organization

### Standard File Structure
```kotlin
@file:JvmSynthetic
@file:Suppress("DEPRECATION")  // Only when needed

package com.reown.module.subpackage

import kotlinx.coroutines.*
import com.reown.foundation.*
// ... other imports

// Constants
private const val TIMEOUT_MS = 30_000L

// Type aliases (if needed)
internal typealias SessionMap = Map<Topic, Session>

// Main class/interface
internal class FeatureImpl(...) : FeatureInterface {
    // Companion object first
    companion object {
        private const val TAG = "FeatureImpl"
    }

    // Properties: injected → derived → mutable state
    private val repository: Repository
    private val scope: CoroutineScope

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    // Init block
    init { ... }

    // Public/override methods
    override fun publicMethod() { ... }

    // Internal methods
    internal fun internalHelper() { ... }

    // Private methods
    private fun privateHelper() { ... }
}

// Extension functions at file level
@JvmSynthetic
internal fun Type.toOther(): Other = ...

// Sealed classes / data classes
sealed class State { ... }
```

## Coroutines Advanced Patterns

### Structured Concurrency
```kotlin
// Engine/manager level scope
internal class FeatureEngine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    fun processItems(items: List<Item>) {
        scope.launch {
            supervisorScope {
                items.map { item ->
                    async { processItem(item) }
                }.awaitAll()
            }
        }
    }
}
```

### Flow Operators
```kotlin
// Collecting with lifecycle
fun observeState(): Flow<State> = _state
    .distinctUntilChanged()
    .onEach { state -> log("State: $state") }

// Combining flows
val combinedState: Flow<CombinedState> = combine(
    connectionState,
    sessionState,
    networkState
) { conn, session, network ->
    CombinedState(conn, session, network)
}

// Debounce for rapid events
fun onSearchQuery(query: String) {
    searchQueryFlow
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { q -> searchRepository.search(q) }
        .collect { results -> updateResults(results) }
}
```

### Channel Patterns
```kotlin
// Event channel for one-time events
private val _events = Channel<Event>(Channel.BUFFERED)
val events: Flow<Event> = _events.receiveAsFlow()

// Sending events
suspend fun notifyEvent(event: Event) {
    _events.send(event)
}
```

## Moshi Configuration

### Custom Adapters
```kotlin
// Value class adapter
class TopicAdapter {
    @ToJson
    fun toJson(topic: Topic): String = topic.value

    @FromJson
    fun fromJson(value: String): Topic = Topic(value)
}

// Polymorphic adapter factory
val jsonRpcAdapterFactory: PolymorphicJsonAdapterFactory<JsonRpcResponse> =
    PolymorphicJsonAdapterFactory.of(JsonRpcResponse::class.java, "type")
        .withSubtype(JsonRpcResult::class.java, "result")
        .withSubtype(JsonRpcError::class.java, "error")

// Moshi instance
val moshi: Moshi = Moshi.Builder()
    .add(TopicAdapter())
    .add(jsonRpcAdapterFactory)
    .addLast(KotlinJsonAdapterFactory())
    .build()
```

### Sealed Class Serialization
```kotlin
@JsonClass(generateAdapter = false)  // Manual adapter needed
sealed class Response {
    @JsonClass(generateAdapter = true)
    data class Success(val data: String) : Response()

    @JsonClass(generateAdapter = true)
    data class Error(val code: Int, val message: String) : Response()
}
```

## SQLDelight Patterns

### Query Definition (.sq files)
```sql
-- Session.sq
CREATE TABLE SessionTable (
    topic TEXT NOT NULL PRIMARY KEY,
    expiry INTEGER NOT NULL,
    metadata TEXT,
    is_acknowledged INTEGER AS Boolean DEFAULT 0
);

selectByTopic:
SELECT * FROM SessionTable WHERE topic = ?;

selectAllActive:
SELECT * FROM SessionTable WHERE expiry > ?;

insert:
INSERT OR REPLACE INTO SessionTable(topic, expiry, metadata, is_acknowledged)
VALUES (?, ?, ?, ?);

deleteByTopic:
DELETE FROM SessionTable WHERE topic = ?;
```

### Repository Implementation
```kotlin
internal class SessionStorageRepository(
    private val database: SignDatabase
) : SessionStorageRepositoryInterface {

    private val queries = database.sessionTableQueries

    override suspend fun getByTopic(topic: Topic): Session? =
        withContext(Dispatchers.IO) {
            queries.selectByTopic(topic.value)
                .executeAsOneOrNull()
                ?.toSession()
        }

    override suspend fun insert(session: Session) =
        withContext(Dispatchers.IO) {
            queries.insert(
                topic = session.topic.value,
                expiry = session.expiry,
                metadata = session.metadata?.toJson(),
                is_acknowledged = session.isAcknowledged
            )
        }
}
```

## Testing Patterns

### Mockk Advanced Usage
```kotlin
// Relaxed mocks for simple cases
private val logger: Logger = mockk(relaxed = true)

// Slot for argument capture
private val capturedEvent = slot<Event>()

@Test
fun `emits correct event`() = runTest {
    coEvery { repository.save(capture(capturedEvent)) } returns Unit

    useCase.execute(params)

    assertEquals(expectedEvent, capturedEvent.captured)
}

// Verification with argument matchers
coVerify {
    repository.save(match { it.type == EventType.SESSION_APPROVED })
}

// Ordered verification
coVerifyOrder {
    validator.validate(any())
    repository.save(any())
    notifier.notify(any())
}
```

### Flow Testing
```kotlin
@Test
fun `state flow emits correct sequence`() = runTest {
    val states = mutableListOf<State>()

    val job = launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.state.toList(states)
    }

    viewModel.loadData()
    advanceUntilIdle()

    assertEquals(
        listOf(State.Loading, State.Success(data)),
        states
    )

    job.cancel()
}
```

## Exception Hierarchy

```kotlin
// Base exception
abstract class WalletConnectException(
    override val message: String?
) : Exception(message)

// Category exceptions
abstract class PairingException(message: String) : WalletConnectException(message)
abstract class SessionException(message: String) : WalletConnectException(message)
abstract class NetworkException(message: String) : WalletConnectException(message)

// Specific exceptions
class PairingExpiredException : PairingException("Pairing has expired")
class InvalidNamespaceException(details: String) : SessionException("Invalid namespace: $details")
class RelayConnectionException(cause: Throwable) : NetworkException("Relay connection failed: ${cause.message}")
```

## Koin Module Organization

### Layered Modules
```kotlin
// Foundation layer
fun foundationModule() = module {
    single<Logger> { DefaultLogger() }
    single<Moshi>(named(FoundationDITags.MOSHI)) { createMoshi() }
}

// Core layer
fun coreModule() = module {
    includes(foundationModule())

    single<RelayClient> { RelayClientImpl(get(), get()) }
    single<PairingRepository> { PairingRepositoryImpl(get()) }
}

// Feature layer
fun signModule() = module {
    includes(coreModule())

    // Storage
    single<SessionStorageRepository> { SessionStorageRepositoryImpl(get()) }

    // Use cases
    single<ConnectUseCaseInterface> { ConnectUseCase(get(), get(), get()) }
    single<ApproveSessionUseCaseInterface> { ApproveSessionUseCase(get(), get()) }

    // Engine
    single<SignEngine> { SignEngine(get(), get(), get(), get()) }
}

// SDK initialization
fun initializeKoin() {
    startKoin {
        modules(signModule())
    }
}
```

## Delegate Pattern Details

### Complete Delegate Implementation
```kotlin
interface WalletDelegate {
    // Required callbacks
    fun onSessionProposal(proposal: Model.SessionProposal, context: Model.VerifyContext)
    fun onSessionRequest(request: Model.SessionRequest, context: Model.VerifyContext)

    // Optional callbacks with defaults
    val onSessionDelete: ((Model.SessionDelete) -> Unit)? get() = null
    val onSessionExtend: ((Model.Session) -> Unit)? get() = null
    val onConnectionStateChange: ((Model.ConnectionState) -> Unit)? get() = null
    val onError: ((Model.Error) -> Unit)? get() = null
}

// Usage in SDK
object WalletKit {
    private var delegate: WalletDelegate? = null

    fun setDelegate(walletDelegate: WalletDelegate) {
        delegate = walletDelegate
    }

    internal fun notifySessionProposal(proposal: SessionProposal, context: VerifyContext) {
        delegate?.onSessionProposal(proposal.toModel(), context.toModel())
    }
}
```

## Common Anti-Patterns to Avoid

```kotlin
// BAD: Blocking calls in coroutines
suspend fun fetchData() {
    val result = api.fetchSync()  // Blocks thread
}

// GOOD: Use suspend functions
suspend fun fetchData() {
    val result = api.fetchAsync()
}

// BAD: Exposing MutableStateFlow
val state = MutableStateFlow<State>(State.Idle)

// GOOD: Encapsulate mutability
private val _state = MutableStateFlow<State>(State.Idle)
val state: StateFlow<State> = _state.asStateFlow()

// BAD: Catching generic Exception
try { ... } catch (e: Exception) { ... }

// GOOD: Catch specific or use Result
runCatching { ... }.onFailure { e -> when(e) { ... } }

// BAD: Hardcoded DI keys
single(named("httpClient")) { ... }

// GOOD: Enum-based tags
single(named(DITags.HTTP_CLIENT)) { ... }

// BAD: Public by default
class FeatureUseCase { ... }

// GOOD: Internal by default for SDK internals
internal class FeatureUseCase { ... }
```
