# Testing Guide for 4people App

This document provides detailed information about testing the 4people emergency communication app.

## Test Structure

The app uses JUnit 4 for unit testing. All tests are located in:
```
app/src/test/java/com/fourpeople/adhoc/
```

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
```

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
