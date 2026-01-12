# 4people - Ad-hoc Emergency Communication

Emergency communication app for Android that enables ad-hoc networking when traditional infrastructure fails.

## Konzept (Concept)

This app provides ad-hoc emergency communication capabilities that can be activated with a single click. When activated, the app:

1. **Activates Bluetooth**: Makes the device discoverable and scans for other emergency devices
2. **Scans WiFi Networks**: Continuously scans for emergency WiFi networks with pattern `4people-<id>`
3. **Creates Hotspot**: Attempts to create a WiFi hotspot with emergency naming pattern for automatic detection
4. **Standby Mode**: App is ready for activation by detecting emergency broadcasts from other devices

### Emergency Detection Pattern

All emergency devices use the naming pattern `4people-<unique-id>` for both:
- Bluetooth device names
- WiFi network SSIDs (where supported)

This allows automatic detection of emergency situations by scanning for this pattern.

## Features

### Active Mode
When emergency mode is activated:
- ✓ Bluetooth discovery enabled
- ✓ Bluetooth advertising with emergency name pattern
- ✓ WiFi scanning for emergency networks (every 10 seconds)
- ✓ Local hotspot activation (on supported devices)
- ✓ Foreground service with persistent notification
- ✓ Emergency broadcast signaling

### Standby Mode
- ✓ App ready to receive activation broadcasts
- ✓ Background WiFi scanning for emergency patterns (every 30 seconds)
- ✓ Phone call indicator detection (brief incoming calls)
- ✓ Automatic or manual activation on emergency detection
- ✓ Low battery consumption
- ✓ Starts automatically on device boot

## Technical Implementation

### Components

1. **MainActivity**: Main UI with emergency activation button and settings access
2. **SettingsActivity**: Configure standby monitoring and auto-activation preferences
3. **AdHocCommunicationService**: Foreground service managing all communication channels
4. **StandbyMonitoringService**: Background service for periodic emergency detection
5. **BootReceiver**: Starts standby monitoring on device boot
6. **EmergencyBroadcastReceiver**: Handles emergency detection broadcasts
7. **PhoneCallIndicatorReceiver**: Detects brief incoming calls as emergency signals

### Permissions Required

The app requires the following permissions for full functionality:

- `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`
- `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`
- `READ_PHONE_STATE` (for phone call indicator detection)
- `FOREGROUND_SERVICE`
- `POST_NOTIFICATIONS` (Android 13+)
- `RECEIVE_BOOT_COMPLETED`

### Build Requirements

- Android Studio Arctic Fox or later
- Gradle 8.0+
- Android SDK 26+ (minimum)
- Target SDK 34

## Building the App

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

## Usage

1. **Install the app** on your Android device
2. **Grant permissions** when prompted (Bluetooth, Location, WiFi, Phone State)
3. **Enable Standby Monitoring** in Settings to automatically detect emergencies
4. **Configure Auto-Activation** in Settings (recommended) or rely on notifications
5. **Manual Activation**: Click "Activate Emergency Communication" button
6. The app will:
   - Start a foreground service
   - Enable Bluetooth discovery
   - Scan for emergency WiFi networks
   - Attempt to create a hotspot
   - Display status of all communication channels

7. **Standby Mode**: The app continuously monitors for:
   - Emergency WiFi networks (4people-*)
   - Brief incoming phone calls (less than 5 seconds)
   - Emergency broadcasts from other devices

8. Other devices with the app will automatically detect your emergency signal

## Architecture

```
MainActivity
    ├── UI Layer (Button, Status Display, Settings)
    ├── SettingsActivity (Configure standby and auto-activation)
    ├── AdHocCommunicationService
    │       ├── Bluetooth Manager (Discovery + Advertising)
    │       ├── WiFi Scanner (Pattern Detection)
    │       └── Hotspot Manager (Emergency Network)
    └── StandbyMonitoringService
            ├── Periodic WiFi Scanner (30s intervals)
            ├── Phone Call Indicator Listener
            └── Auto-Activation Logic
```

## Testing

The app includes comprehensive unit tests to verify core functionality:

### Running Tests

```bash
# Run all unit tests
./gradlew test

# Run tests with detailed output
./gradlew test --info

# Run tests for a specific module
./gradlew app:testDebugUnitTest

# Generate test coverage report
./gradlew testDebugUnitTest jacocoTestReport
```

### Test Coverage

The test suite covers:

- **AdHocCommunicationTest**: Emergency pattern matching, WiFi scan intervals, notification channels
- **StandbyMonitoringTest**: Standby monitoring logic, scan intervals, preferences, notification IDs
- **PhoneCallIndicatorTest**: Phone call duration logic, emergency indicator detection
- **BootReceiverTest**: Boot completion handling, service initialization
- **EmergencyBroadcastReceiverTest**: Emergency broadcast handling, intent actions
- **AppConstantsTest**: Cross-component consistency, configuration validation

### Test Location

All unit tests are located in:
```
app/src/test/java/com/fourpeople/adhoc/
```

### Writing New Tests

When adding new features, follow these testing guidelines:

1. Place unit tests in `app/src/test/java/com/fourpeople/adhoc/`
2. Use descriptive test names that explain what is being tested
3. Follow the existing test structure and naming conventions
4. Test both positive and negative cases
5. Verify constants, patterns, and cross-component consistency

## Limitations

- WiFi hotspot creation is restricted on Android 8+ and requires special system permissions
- Some manufacturers restrict background Bluetooth scanning
- Location permission is required for WiFi scanning on Android 6+
- Battery consumption increases significantly in active mode

## Future Enhancements

- [ ] Peer-to-peer messaging over ad-hoc connections
- [ ] Automatic mesh network formation
- [ ] Emergency location sharing
- [ ] Offline map integration
- [ ] Advanced battery optimization for standby mode
- [ ] Support for more communication protocols (WiFi Direct, NFC)
- [ ] Contact-based emergency signaling (calling frequent contacts)

## License

This is a concept implementation for emergency communication.
