# Testing Guide for 4people App

This document provides detailed information about testing the 4people emergency communication app.

## Test Structure

The app uses JUnit 4 for unit testing. All tests are located in:
```
app/src/test/java/com/fourpeople/adhoc/
```

## Test Coverage Summary

The project has comprehensive test coverage across all critical domains:

- **Total Test Files**: 48+
- **Coverage Areas**: 
  - Core Services (AdHocCommunication, PanicMode, StandbyMonitoring)
  - Location Domain (LocationSharing, SafeZone, LocationDataStore)
  - Mesh Networking (MeshRouting, RouteTable, MeshMessage)
  - Trust System (TrustManager, MessageVerification, ContactTrustLevel)
  - Protocol Handlers (SEPS Protocol, Codec)
  - Utility Helpers (LogManager, BatteryMonitor, EmergencySms)
  - Simulation Engine (Scenarios, Person, WiFi)
  - Widgets (EmergencyWidget, PanicWidget)
  - Receivers (Boot, Broadcast, PhoneCall)

## Test Files Overview

### 1. AdHocCommunicationTest.kt
Tests the core emergency communication service functionality:
- Emergency SSID pattern validation
- WiFi scan interval configuration
- Notification channel setup

### 2. StandbyMonitoringTest.kt
Tests the background monitoring service:
- Emergency pattern matching in standby mode
- Scan interval optimization for battery life
- Notification ID and channel ID uniqueness
- SharedPreferences configuration

### 3. PhoneCallIndicatorTest.kt
Tests the phone call emergency indicator:
- Brief call threshold validation
- Call duration logic
- Edge cases (negative/zero durations)
- Multiple indicator detection

### 4. BootReceiverTest.kt
Tests the boot receiver component:
- Boot completion intent handling
- Service initialization
- Action constant validation

### 5. EmergencyBroadcastReceiverTest.kt
Tests emergency broadcast handling:
- Intent action format validation
- Intent extra keys
- Broadcast uniqueness

### 6. AppConstantsTest.kt
Cross-component integration tests:
- Emergency pattern consistency across services
- Notification ID uniqueness
- Channel ID uniqueness
- Scan interval relationships
- Service action naming conventions
- Battery optimization validation

### 7. BroadcastIntentTest.kt
Intent and broadcast pattern tests:
- Intent action uniqueness
- Security patterns for broadcasts
- System action constants
- Component accessibility

### 8. SecurityAndEdgeCaseTest.kt
Security and robustness tests:
- Input validation
- Injection prevention
- Resource exhaustion prevention
- Null safety
- Edge case handling
- Pattern matching security

### Location Domain Tests

#### 9. LocationSharingManagerTest.kt
Tests location sharing functionality:
- Location sharing activation/deactivation
- Participant location tracking
- Help request creation and forwarding
- Event radius filtering
- Stale location cleanup

#### 10. SafeZoneTest.kt
Tests safe zone data model:
- Safe zone creation with various parameters
- Validity checking (24-hour expiration)
- Coordinate handling (including negative values)
- Data class immutability and copying

#### 11. SafeZoneManagerTest.kt
Tests safe zone singleton manager:
- Adding and removing safe zones
- Finding nearest safe zone
- Listener notifications
- Automatic cleanup of invalid zones
- Thread safety

#### 12. LocationDataStoreTest.kt
Tests location data singleton store:
- Location updates and retrieval
- Help request filtering
- Listener notifications
- Stale location cleanup
- Thread safety

### Utility Helper Tests

#### 13. LogManagerTest.kt
Tests centralized logging system:
- Multiple log levels (INFO, WARNING, ERROR, EVENT, etc.)
- Log entry formatting and timestamps
- Listener notifications
- Log persistence (JSON serialization)
- Maximum log entry limits

#### 14. BatteryMonitorTest.kt
Tests battery-aware adaptive scanning:
- Battery level reading
- Adaptive scan intervals based on battery
- Emergency vs standby mode intervals
- Battery mode descriptions

#### 15. EmergencySmsHelperTest.kt
Tests SMS emergency notifications:
- Contact list management
- SMS enable/disable settings
- International phone number support
- Whitespace and empty value handling

### Simulation Domain Tests

#### 16. SimulationScenarioTest.kt
Tests simulation scenarios:
- Predefined scenario configurations
- Location types (city, village)
- Infrastructure failure modes
- Verbal transmission settings
- Approaching behavior settings
- Parameter validation

#### 17. SimulationWiFiTest.kt
Tests WiFi access point simulation:
- WiFi AP creation with position and range
- Coordinate handling
- Range customization

#### 18. SimulationPersonTest.kt
Tests person simulation:
- Person state (moving, indoor, has app)
- Movement speeds (walking, approaching, stationary)
- Event reception tracking
- Verbal transmission
- Approaching behavior

### Trust System Tests

#### 19. ContactTrustLevelTest.kt
Tests contact trust levels:
- Trust level constants (Unknown, Known, Friend, Family)
- Trust factor calculation (0.0 to 1.0)
- Manual vs automatic trust setting
- Trust level validation
- Descriptive labels

#### 20. MessageVerificationTest.kt
Tests message verification system:
- Confirmation and rejection tracking
- Verifier identification
- Timestamp tracking
- Optional comments
- Multiple verifications per message

## Running Tests

### Using Gradle (Recommended)

```bash
# Run all unit tests
./gradlew test

# Run with detailed output
./gradlew test --info

# Run only debug unit tests
./gradlew app:testDebugUnitTest

# Run only release unit tests
./gradlew app:testReleaseUnitTest

# Run tests and continue on failure
./gradlew test --continue
```

### Using Android Studio

1. Right-click on `app/src/test/java/com/fourpeople/adhoc`
2. Select "Run 'Tests in com.fourpeople.adhoc'"
3. View results in the Run panel

### Running Individual Test Files

```bash
# Run a specific test class
./gradlew test --tests com.fourpeople.adhoc.AdHocCommunicationTest

# Run a specific test method
./gradlew test --tests com.fourpeople.adhoc.AdHocCommunicationTest.emergencyPatternIsCorrect

# Run all tests in a package
./gradlew test --tests "com.fourpeople.adhoc.location.*"
```

## Automated CI/CD Testing

The project includes automated testing in GitHub Actions workflows:

### Pull Request Testing (`.github/workflows/pr-build.yml`)
- Runs on every pull request to `main`
- Executes full unit test suite
- Publishes test results as PR check
- Builds release APK
- Must pass before merge

### Release Testing (`.github/workflows/release.yml`)
- Runs on every push to `main`
- Executes full unit test suite
- Publishes test results
- Builds and publishes release APK
- Increments version number

### Dedicated Test Workflow (`.github/workflows/test.yml`)
Runs comprehensive testing on pull requests and pushes:

**Unit Tests Job:**
- Runs all unit tests with `./gradlew test --continue`
- Publishes detailed test reports
- Uploads test results as artifacts (retained 30 days)
- Fails workflow if tests fail

**Lint Job:**
- Runs Android lint checks
- Uploads lint reports
- Helps maintain code quality

### Viewing Test Results in CI

1. Navigate to the "Actions" tab in GitHub
2. Select the workflow run
3. View "Unit Test Results" check
4. Download test artifacts for detailed HTML reports

### Test Report Artifacts

After each CI run, test reports are available:
- **test-results**: JUnit XML test results
- **test-reports**: HTML test reports with detailed output
- Retained for 30 days

## Test Categories

### Functional Tests
Verify that core functionality works as expected:
- Emergency pattern matching
- Service configuration
- Broadcast handling

### Integration Tests
Verify that components work together correctly:
- Cross-service consistency
- Action uniqueness
- Configuration compatibility

### Security Tests
Verify security properties:
- Input validation
- Broadcast security patterns
- Resource limits
- Injection prevention

### Edge Case Tests
Verify handling of unusual inputs:
- Null values
- Empty strings
- Boundary conditions
- Invalid states

## Test Coverage Goals

- **Constants & Configuration**: 100% coverage
- **Public APIs**: 80%+ coverage
- **Critical paths**: 100% coverage
- **Edge cases**: Comprehensive coverage

## Writing New Tests

When adding new features, follow these guidelines:

### 1. Test File Naming
- Use descriptive names: `[Component]Test.kt`
- Place in same package as code under test

### 2. Test Method Naming
- Use descriptive names that explain what is tested
- Format: `featureBeingTested_expectedBehavior`
- Example: `emergencyPatternMatching_validSSID_returnsTrue`

### 3. Test Structure
```kotlin
@Test
fun descriptiveTestName() {
    // Arrange: Set up test data
    val input = "4people-abc123"
    
    // Act: Execute the code being tested
    val result = input.startsWith(EMERGENCY_SSID_PATTERN)
    
    // Assert: Verify the result
    assertTrue(result)
}
```

### 4. Test Both Positive and Negative Cases
```kotlin
@Test
fun validInputTest() {
    // Test with valid input
}

@Test
fun invalidInputTest() {
    // Test with invalid input
}
```

### 5. Use Meaningful Assertions
```kotlin
// Good: Provides context
assertEquals("Emergency pattern should be '4people-'", "4people-", EMERGENCY_SSID_PATTERN)

// Better: Descriptive test name with simple assertion
@Test
fun emergencyPatternFollowsExpectedFormat() {
    assertEquals("4people-", EMERGENCY_SSID_PATTERN)
}
```

## Continuous Integration

Tests should be run:
- Before every commit
- In CI/CD pipeline
- Before creating pull requests
- Before releases

## Known Limitations

### Android-Specific Tests
Some Android components (like Services, BroadcastReceivers) require instrumentation tests for full testing. The current test suite focuses on:
- Business logic validation
- Configuration verification
- Pattern matching
- Constant validation

### Future Test Additions
Consider adding:
- Instrumentation tests (`androidTest`)
- Integration tests with Android framework
- UI tests with Espresso
- Performance tests

## Troubleshooting

### Tests Won't Run
1. Ensure Android SDK is installed
2. Check that gradle wrapper is present
3. Sync Gradle files in Android Studio
4. Clean and rebuild: `./gradlew clean build`

### Tests Fail
1. Check for recent code changes
2. Verify constants haven't changed
3. Review test output for details
4. Run tests individually to isolate failures

### Missing Dependencies
If you see compilation errors:
1. Sync Gradle: `./gradlew build`
2. Check `app/build.gradle.kts` for test dependencies
3. Ensure JUnit 4.13.2 is included

## Resources

- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Android Testing Guide](https://developer.android.com/training/testing)
- [Gradle Testing](https://docs.gradle.org/current/userguide/java_testing.html)

## Contributing Tests

When contributing new tests:
1. Follow existing patterns and conventions
2. Ensure tests are deterministic
3. Keep tests independent (no shared state)
4. Add documentation for complex test scenarios
5. Update this guide if adding new test categories
