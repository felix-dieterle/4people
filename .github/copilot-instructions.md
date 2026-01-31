# GitHub Copilot Agent Instructions - 4people Emergency Communication App

## Core Principles

### Code Quality & Maintainability
- **Keep structure and code clean, understandable, and maintainable**
- **Make code junior-developer and AI-agent friendly** - use clear naming, simple logic, and comprehensive comments
- **Write self-documenting code** - prefer clarity over cleverness
- **Follow established patterns** - consistency is key for maintainability

### Minimal Changes Philosophy
- **Make minimal changes** - only modify what is absolutely necessary
- **Don't destroy working functionality** - if it works, don't break it
- **Preserve existing behavior** - changes should be additive or surgical
- **Test before and after** - ensure changes don't introduce regressions

## Project Architecture

### Package Organization
The project follows a **domain-driven package structure**:

```
com.fourpeople.adhoc/
├── service/          # Android Services (AdHocCommunicationService, PanicModeService)
├── receiver/         # BroadcastReceivers (EmergencyBroadcastReceiver, BootReceiver)
├── mesh/             # Mesh networking (MeshRoutingManager, RouteTable, MeshMessage)
├── trust/            # Trust system (TrustManager, MessageVerificationManager)
├── protocol/         # Protocols (SepsProtocolHandler for interoperability)
├── location/         # Location services (LocationSharingManager, SafeZoneManager)
├── util/             # Utilities (ErrorLogger, LogManager, helpers)
├── simulation/       # Simulation engine for emergency propagation modeling
└── widget/           # App widgets (EmergencyWidget, PanicWidget)
```

**When adding new code:**
- Place it in the appropriate domain package
- Create new packages only when introducing a distinct functional domain
- Keep related functionality together

### Architectural Patterns

#### Singleton Pattern (Kotlin `object`)
Use for cross-cutting concerns and utilities:
```kotlin
object ErrorLogger {
    private const val TAG = "ErrorLogger"
    
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        // Implementation
    }
}
```

#### Manager Pattern
Use for domain-specific logic with state:
```kotlin
class TrustManager(private val context: Context) {
    companion object {
        const val TAG = "TrustManager"
        private const val PREF_NAME = "trust_data"
    }
    
    private val trustLevels = mutableMapOf<String, ContactTrustLevel>()
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    fun setTrustLevel(contactId: String, level: Int): Boolean {
        // Update in-memory cache + persist to SharedPreferences
    }
}
```

#### Service Pattern
Android Services with lifecycle management:
```kotlin
class AdHocCommunicationService : Service() {
    companion object {
        const val TAG = "AdHocCommService"
        const val NOTIFICATION_ID = 1001
        
        fun isActive(context: Context): Boolean {
            // Static helper for checking service state
        }
    }
    
    private lateinit var wifiManager: WifiManager
    
    override fun onCreate() {
        super.onCreate()
        // Initialize resources
    }
}
```

## Naming Conventions

### Classes
- **Activities**: `<Feature>Activity` (MainActivity, SettingsActivity)
- **Fragments**: `<Feature>Fragment` (EmergencyFragment, PanicFragment)
- **Services**: `<Feature>Service` (AdHocCommunicationService, StandbyMonitoringService)
- **Receivers**: `<Feature>Receiver` (BootReceiver, EmergencyBroadcastReceiver)
- **Managers**: `<Feature>Manager` (TrustManager, MeshRoutingManager)
- **Helpers**: `<Feature>Helper` (FlashlightHelper, NFCHelper)
- **Data Classes**: Descriptive nouns (LocationData, MeshMessage, SepsMessage)

### Variables & Properties
- **Private binding fields**: `_binding` (nullable) with `binding` getter (non-null)
- **Boolean flags**: `is<Feature>Active`, `is<Feature>Enabled`
- **Constants**: UPPERCASE_WITH_UNDERSCORES in companion objects
- **Local variables**: camelCase
- **Collections**: Use plural names (people, messages, trustLevels)

### Functions
- **Actions**: `<verb><Noun>()` (sendMessage, broadcastLocation, getTrustLevel)
- **Callback setters**: `set<Listener>()` (setMessageListener, setStateChangeListener)
- **Boolean queries**: `is<State>()`, `has<Feature>()`, `can<Action>()`

## Coding Standards

### Kotlin Best Practices

#### Null Safety
Always use safe navigation and null checks:
```kotlin
// Good
val result = data?.field?.let { processData(it) } ?: defaultValue

// Avoid
val result = data!!.field // Avoid !! operator
```

#### Data Classes with Defaults
Use data classes with sensible defaults for domain models:
```kotlin
data class MeshMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val sourceId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 5,
    val hopCount: Int = 0
) : Serializable {
    fun forward(isSecureHop: Boolean = true): MeshMessage {
        // Return immutable copy with updated fields
        return copy(hopCount = hopCount + 1, ttl = ttl - 1)
    }
}
```

#### Fragment ViewBinding Pattern
Always use ViewBinding with proper lifecycle management:
```kotlin
class EmergencyFragment : Fragment() {
    private var _binding: FragmentEmergencyBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmergencyBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }
}
```

### Error Handling

#### Try-Catch with Fallbacks
Wrap risky operations with graceful fallbacks:
```kotlin
fun parseLocationData(json: String): LocationData? {
    return try {
        val jsonObj = JSONObject(json)
        LocationData(
            deviceId = jsonObj.getString("deviceId"),
            latitude = jsonObj.getDouble("latitude"),
            longitude = jsonObj.getDouble("longitude")
        )
    } catch (e: JSONException) {
        ErrorLogger.logError(TAG, "Failed to parse location data", e)
        null // Return null instead of crashing
    }
}
```

#### Centralized Logging
Use ErrorLogger for persistent error tracking:
```kotlin
ErrorLogger.logError(TAG, "WiFi scan failed", exception)
ErrorLogger.logMessage(TAG, "Emergency mode activated")
```

Use LogManager for in-app log display:
```kotlin
LogManager.log(LogLevel.ERROR, "Service", "Connection failed")
LogManager.log(LogLevel.INFO, "Network", "Device discovered: $deviceId")
```

### Thread Safety

Use concurrent collections when accessing from multiple threads:
```kotlin
private val activeConnections = CopyOnWriteArrayList<String>()
private val routeTable = ConcurrentHashMap<String, RouteEntry>()
```

### Documentation

#### KDoc for Classes
Every major class should have KDoc documentation:
```kotlin
/**
 * Manages mesh network routing using a simplified AODV-like protocol.
 * 
 * Features:
 * - Route discovery and maintenance
 * - Duplicate message detection
 * - TTL-based message forwarding
 * - Sequence number tracking for loop prevention
 * 
 * @param context Application context for accessing system services
 * @param deviceId Unique identifier for this device
 */
class MeshRoutingManager(
    private val context: Context,
    private val deviceId: String
) {
    // Implementation
}
```

#### Inline Comments
Use sparingly but meaningfully:
```kotlin
// Check if route is stale (older than 5 minutes)
if (System.currentTimeMillis() - route.timestamp > 300_000) {
    routeTable.remove(deviceId)
}
```

#### Logging Tags
Each class should define a TAG constant:
```kotlin
companion object {
    private const val TAG = "MeshRoutingManager"
}
```

## Testing Patterns

### Test Structure
- **Location**: `app/src/test/java/com/fourpeople/adhoc/`
- **Naming**: `<Feature>Test.kt` (MeshRoutingManagerTest.kt)
- **Package**: Mirror source package structure

### Test Templates

#### Unit Test with Mocks
```kotlin
class MeshRoutingManagerTest {
    private lateinit var context: Context
    private lateinit var meshManager: MeshRoutingManager
    
    @Before
    fun setUp() {
        context = mock(Context::class.java)
        meshManager = MeshRoutingManager(context, "test-device-id")
    }
    
    @Test
    fun testMessageForwarding() {
        val message = MeshMessage(
            sourceId = "source-1",
            content = "Test message"
        )
        
        val forwarded = message.forward()
        
        assertEquals(1, forwarded.hopCount)
        assertEquals(4, forwarded.ttl)
        assertTrue(forwarded.canForward())
    }
}
```

### Testing Guidelines
- **Test positive and negative cases**
- **Mock Android components** (Context, Service, Manager)
- **Verify behavior**, not implementation details
- **Use descriptive test names** that explain what is being tested
- **Follow AAA pattern**: Arrange, Act, Assert

## Dependency Management

### Current Stack
- **AndroidX**: core-ktx, appcompat, lifecycle, recyclerview
- **Material Design**: com.google.android.material
- **Maps**: osmdroid (offline maps)
- **Testing**: JUnit 4, Mockito, AndroidX test

### Adding Dependencies
- **Check for alternatives** - prefer existing dependencies over adding new ones
- **Verify compatibility** - ensure version compatibility with existing libs
- **Update sparingly** - only update versions when necessary for bug fixes or required features
- **No dependency injection framework** - use constructor injection manually

## Common Patterns in This Project

### Persistence Strategy
- **SharedPreferences** for small key-value data (trust levels, settings)
- **File storage** for logs (ErrorLogger writes to Downloads directory)
- **In-memory caches** backed by persistence (TrustManager, LogManager)

### Communication Patterns
- **BroadcastReceiver** for inter-component communication
- **Callback interfaces** for asynchronous operations (MessageListener, LogListener)
- **Foreground Services** for long-running operations (AdHocCommunicationService)

### Mesh Networking
- **AODV-like routing** with route discovery and maintenance
- **Duplicate detection** using message IDs
- **TTL-based forwarding** to prevent infinite loops
- **Secure route tracking** for trust evaluation

### SEPS Protocol
- **JSON-based messages** for interoperability
- **Version negotiation** for protocol evolution
- **Standard message types** (HELLO, LOCATION, HELP_REQUEST, etc.)
- **Backward compatibility** with legacy 4people devices

## Project-Specific Guidelines

### Emergency Communication Context
This app is designed for **ad-hoc emergency networking** when infrastructure fails:
- **Offline-first design** - assume no internet connectivity
- **Multiple transports** - Bluetooth, WiFi, WiFi Direct, SMS
- **Mesh routing** - messages relay through intermediate devices
- **Trust-based evaluation** - assess message reliability in emergencies

### Feature Implementation Checklist
When adding new features:
1. ✅ **Consider offline use** - does it work without internet?
2. ✅ **Battery impact** - minimize background processing
3. ✅ **Permission requirements** - document and request only what's needed
4. ✅ **Cross-device compatibility** - test on different Android versions
5. ✅ **Error recovery** - graceful degradation when hardware unavailable
6. ✅ **Logging** - add appropriate log statements for debugging
7. ✅ **Testing** - write unit tests for core logic
8. ✅ **Documentation** - update relevant .md files

### Performance Considerations
- **Lazy initialization** for expensive resources (hardware managers)
- **Background threads** for I/O operations (file access, network)
- **Periodic cleanup** for caches and old data (route expiration, log rotation)
- **Foreground services** for user-visible long-running tasks

## Build & Testing Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (signed with debug key)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run tests with coverage
./gradlew testDebugUnitTest jacocoTestReport

# Install on device
./gradlew installDebug
```

## Security Considerations

### Don't Commit Secrets
- Never commit API keys, passwords, or keystores
- Use environment variables for sensitive data in CI

### Permission Handling
- Request permissions at runtime (Android 6+)
- Explain permission usage to users
- Handle permission denial gracefully

### Data Privacy
- Don't log sensitive user data (phone numbers, exact locations in logs)
- Sanitize data before logging
- Respect user privacy settings

## Summary Checklist for Contributors

When contributing code, ensure:
- [ ] Code follows established naming conventions
- [ ] Placed in appropriate package structure
- [ ] Uses proper error handling with logging
- [ ] Includes KDoc for public APIs
- [ ] Has unit tests for core logic
- [ ] Minimal changes - don't refactor unrelated code
- [ ] No breaking changes to existing functionality
- [ ] Documentation updated if needed
- [ ] Build and tests pass locally

---

**Remember**: This is an emergency communication app that could save lives. Code quality and reliability are paramount. Keep it simple, keep it working, and always think about the users who may depend on this in critical situations.
