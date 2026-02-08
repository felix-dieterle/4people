# 4people - Ad-hoc Emergency Communication

![Tests](https://github.com/felix-dieterle/4people/actions/workflows/test.yml/badge.svg)
![Build](https://github.com/felix-dieterle/4people/actions/workflows/pr-build.yml/badge.svg)

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

### Interoperability with Other Emergency Apps

The 4people app implements the **Standard Emergency Protocol Specification (SEPS) v1.0**, enabling interoperability with other emergency communication apps:
- ✓ **Standard protocol support** - JSON-based message format for maximum compatibility
- ✓ **SEPS device naming** - Supports `SEPS-4people-<id>` pattern for cross-app discovery
- ✓ **Message forwarding** - Relays messages from other SEPS-compliant apps
- ✓ **Protocol negotiation** - Automatically detects and adapts to different app capabilities
- ✓ **Backward compatibility** - Works with both legacy 4people devices and new SEPS devices

See [EMERGENCY_PROTOCOL_SPEC.md](EMERGENCY_PROTOCOL_SPEC.md) for full protocol specification and [INTEROPERABILITY_GUIDE.md](INTEROPERABILITY_GUIDE.md) for implementation guide.

## Features

### Active Mode
When emergency mode is activated:
- ✓ Bluetooth discovery enabled
- ✓ Bluetooth advertising with emergency name pattern
- ✓ WiFi scanning for emergency networks (every 10 seconds)
- ✓ Local hotspot activation (on supported devices)
- ✓ **WiFi auto-connect** - Automatically connect to discovered emergency networks to expand mesh coverage
- ✓ Foreground service with persistent notification
- ✓ Emergency broadcast signaling
- ✓ WiFi Direct peer-to-peer discovery
- ✓ SMS emergency broadcast to contacts
- ✓ **Mesh network routing** - Multi-hop message forwarding through intermediate devices
- ✓ **Automatic route discovery** - AODV-like protocol for finding paths to distant devices
- ✓ **SEPS protocol support** - Interoperability with other emergency apps implementing standard emergency protocols
- ✓ **Trust-based message evaluation** - Assess message trustworthiness based on sender trust level, hop count, and peer verifications
- ✓ **Flashlight Morse code signaling (optional)**
- ✓ **Ultrasound emergency beacon (optional)**
- ✓ **NFC Tap-to-Join** - Quick network joining through device-to-device NFC touch
- ✓ **GPS location sharing** - Automatic GPS coordinate broadcasting to all network participants
- ✓ **Participant location map** - View all participants' locations in the emergency network
- ✓ **Offline map integration** - OSM-based offline maps with participant tracking, safe zones, and routing
- ✓ **Help requests with location** - Send emergency help requests with GPS coordinates
- ✓ **Panic Mode** - Progressive escalation system with confirmation checks, alerts, and emergency contact notification
- ✓ **Emergency Propagation Simulation** - Visual simulation tool to demonstrate message spread through the network
- ✓ **Infrastructure Health Monitoring** - Real-time monitoring of Bluetooth, WiFi, cellular, and mesh network health with failure notifications

### Offline Map Integration
The app includes an OpenStreetMap-based offline map for emergency navigation:
- ✓ **OSM-based maps** - Uses osmdroid library for offline map capability
- ✓ **Real-time participant tracking** - Shows all network participants on the map with live updates
- ✓ **Help request visualization** - Highlights participants requesting help with 🆘 markers
- ✓ **Safe zones (collection points)** - Mark and share emergency gathering locations
- ✓ **Route display** - Show routes to nearest safe zones with distance calculations
- ✓ **Interactive markers** - Tap markers for details and route planning
- ✓ **Automatic map updates** - Participant locations update in real-time as they move

### Emergency Propagation Simulation
The app includes an interactive simulation mode that visualizes how emergency messages spread:
- ✓ Visual map showing people, WiFi networks, and events
- ✓ **9 Predefined Scenarios**: City Center, City, Village × 3 infrastructure failure modes
- ✓ **Verbal Transmission**: Simulates word-of-mouth in critical scenarios
- ✓ **Approaching Behavior**: Informed people actively seek uninformed people
- ✓ **Indoor/Outdoor Modeling**: Buildings reduce signal range by 40%
- ✓ Configurable parameters (people count, app adoption rate 5-90%)
- ✓ Time controls with adjustable speed (1x, 2x, 5x, 10x)
- ✓ Real-time statistics and coverage metrics
- ✓ Movement simulation with realistic walking patterns
- ✓ See [SIMULATION.md](SIMULATION.md) for detailed documentation

### Panic Mode
Panic Mode provides automatic escalation when user cannot respond:
- ✓ Regular confirmation requests (every 30 seconds)
- ✓ Gentle warning phase (vibration/sound alerts)
- ✓ Massive alert phase (flashlight, alarm, GPS activation, backend notification)
- ✓ Progressive contact notification (intervals double: 3min, 6min, 12min, etc.)
- ✓ Home screen widget for quick activation
- ✓ Configurable warning types and auto-network activation
- ✓ See [PANIC_MODE.md](PANIC_MODE.md) for detailed documentation

### Standby Mode
- ✓ App ready to receive activation broadcasts
- ✓ Background WiFi scanning for emergency patterns (every 30 seconds)
- ✓ Phone call indicator detection (brief incoming calls)
- ✓ **Ultrasound listening for emergency beacons** (continuous passive detection)
- ✓ Automatic or manual activation on emergency detection
- ✓ Low battery consumption
- ✓ Starts automatically on device boot

## Technical Implementation

### Components

1. **MainActivity**: Main UI with emergency activation button and settings access
2. **SettingsActivity**: Configure standby monitoring and auto-activation preferences
3. **LocationMapActivity**: View GPS locations of all participants in the emergency network (list view)
4. **OfflineMapActivity**: Interactive OSM-based map with participant tracking, safe zones, and routing
5. **AdHocCommunicationService**: Foreground service managing all communication channels
6. **StandbyMonitoringService**: Background service for periodic emergency detection
7. **PanicModeService**: Foreground service managing panic mode with progressive escalation
8. **BootReceiver**: Starts standby monitoring on device boot
9. **EmergencyBroadcastReceiver**: Handles emergency detection broadcasts
10. **PhoneCallIndicatorReceiver**: Detects brief incoming calls as emergency signals
11. **MeshRoutingManager**: Manages mesh network routing and multi-hop message forwarding
12. **BluetoothMeshTransport**: Handles Bluetooth communication for mesh messages
13. **LocationSharingManager**: Manages GPS location capture and broadcasting to network participants
14. **LocationDataStore**: Singleton store for real-time location data sharing across components
15. **SafeZoneManager**: Manages safe zones (collection points) for emergency gathering
16. **EmergencyWidget**: Home screen widget for quick emergency mode activation
17. **PanicWidget**: Home screen widget for quick panic mode activation
18. **InfrastructureMonitor**: Real-time health monitoring of critical infrastructure components

### Permissions Required

The app requires the following permissions for full functionality:

- `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`
- `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` (for GPS location sharing)
- `READ_PHONE_STATE` (for phone call indicator detection)
- `SEND_SMS` (for emergency SMS broadcast)
- `CAMERA` (for flashlight Morse code signaling)
- `RECORD_AUDIO` (for ultrasound signal detection)
- `NFC` (for tap-to-join functionality)
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

### APK Signing and Updates

**Important**: For APKs to be updateable, they must be signed with the same keystore across all versions.

- **Local Development**: By default, debug keystore is used (APKs not updateable)
- **Production Releases**: Configure a release keystore in GitHub secrets for updateable APKs
- **Setup Instructions**: See [RELEASE_KEYSTORE_SETUP.md](RELEASE_KEYSTORE_SETUP.md) for detailed configuration guide

**Without a release keystore**, each build will be signed with a different key, preventing app updates (users must uninstall and reinstall).

## Releases

This project uses automated releases through GitHub Actions. When changes are merged to the `main` branch:

1. **Version is automatically incremented**: Both `versionCode` and `versionName` are bumped
2. **Release APK is built and signed**: A signed APK is generated
3. **GitHub Release is created**: A new release with the version tag is published
4. **APK is attached**: The built APK is automatically uploaded to the release

**APK Updateability**: 
- ✅ If a release keystore is configured in GitHub secrets, APKs will be updateable across versions
- ⚠️ Without a release keystore, APKs use debug signing and cannot update each other

See [RELEASE_KEYSTORE_SETUP.md](RELEASE_KEYSTORE_SETUP.md) for instructions on configuring a release keystore.

For more details about the release workflow, see [RELEASE_WORKFLOW.md](RELEASE_WORKFLOW.md).

### Download Latest Release

Visit the [Releases page](https://github.com/felix-dieterle/4people/releases) to download the latest APK.

### Version Numbering

- **versionCode**: Incremented by 1 with each release (used by Android)
- **versionName**: Follows semantic versioning (e.g., 1.0.0 → 1.0.1)

## Usage

1. **Install the app** on your Android device
2. **Grant permissions** when prompted (Bluetooth, Location, WiFi, Phone State)
3. **Enable Standby Monitoring** in Settings to automatically detect emergencies
4. **Configure Auto-Activation** in Settings (recommended) or rely on notifications
5. **Manual Activation**: Click "Activate Emergency Communication" button
6. **NFC Tap-to-Join**: To quickly join an active emergency network, tap your device against another device that has emergency mode active. Network credentials will be automatically exchanged via NFC, and you'll be prompted to join.
7. The app will:
   - Start a foreground service
   - Enable Bluetooth discovery
   - Scan for emergency WiFi networks
   - Attempt to create a hotspot
   - **Start GPS location sharing** - Broadcast your location to all network participants
   - Display status of all communication channels

8. **Standby Mode**: The app continuously monitors for:
   - Emergency WiFi networks (4people-*)
   - Brief incoming phone calls (less than 5 seconds)
   - Emergency broadcasts from other devices

9. **Location Sharing Features**:
   - **View Participant Map**: Click "View Participant Map" to see GPS locations of all emergency network participants
   - **Open Offline Map**: Click "Open Offline Map" for an interactive OSM-based map with real-time participant tracking
   - **Mark Safe Zones**: Use the map to mark and share safe collection points
   - **Navigate to Safety**: View routes to nearest safe zones with distance information
   - **Send Help Request**: Click "Send Help Request" to broadcast your location with an emergency message to all participants
   - Locations are automatically updated every 30 seconds
   - Help requests are highlighted with 🆘 indicator

10. Other devices with the app will automatically detect your emergency signal
    
11. Other devices with the app will automatically detect your emergency signal

### NFC Tap-to-Join Feature

The NFC Tap-to-Join feature allows for quick and seamless network joining:

**How it works:**
1. **Person A** has emergency mode activated
2. **Person B** taps their NFC-enabled device against Person A's device
3. Network credentials are automatically exchanged via NFC
4. **Person B** receives a prompt to join the emergency network
5. Upon confirmation, Person B immediately joins the network

**Requirements:**
- Both devices must have NFC hardware
- NFC must be enabled in device settings
- The app must be in the foreground or have been recently used

**Benefits:**
- **Instant network sharing** - No manual configuration needed
- **Secure** - Credentials expire after 1 hour
- **Easy to use** - Simply tap devices together
- **Works offline** - No internet connection required

### Infrastructure Health Monitoring

The app continuously monitors the health of critical infrastructure components and alerts you when failures occur:

**Monitored Components:**
- **Bluetooth**: Adapter availability and status
- **WiFi**: Connection strength and availability
- **Cellular Network**: Signal strength and connectivity
- **Mesh Network**: Active nodes and routing status

**Features:**
- **Real-time Status Display**: Color-coded indicators show the health of each component
  - ✓ Green: Operational (healthy)
  - ⚠ Yellow: Degraded (weak signal or partial availability)
  - ✗ Red: Failed (not working or unavailable)
- **Overall Health Status**: Combined health assessment of all infrastructure components
- **Push Notifications**: Get alerted when critical infrastructure fails (can be enabled/disabled in Settings)
- **Historical Tracking**: The system tracks infrastructure health over time to detect patterns
- **Automatic Monitoring**: Checks infrastructure health every 30 seconds when emergency mode is active

**How to Use:**
1. Activate emergency mode to begin infrastructure monitoring
2. View the "Infrastructure Health" section in the main screen
3. Enable/disable infrastructure failure notifications in Settings > Infrastructure Alerts
4. When critical infrastructure fails, you'll receive a push notification with details

This feature helps you quickly identify communication channel failures and adapt your emergency response strategy accordingly.

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

The test suite includes **8 comprehensive test files** with **80+ test cases** covering:

#### Core Component Tests
- **AdHocCommunicationTest**: Emergency pattern matching, WiFi scan intervals, notification channels
- **StandbyMonitoringTest**: Standby monitoring logic, scan intervals, preferences, notification IDs
- **PhoneCallIndicatorTest**: Phone call duration logic, emergency indicator detection

#### Receiver Tests
- **BootReceiverTest**: Boot completion handling, service initialization
- **EmergencyBroadcastReceiverTest**: Emergency broadcast handling, intent actions

#### Integration & Validation Tests
- **AppConstantsTest**: Cross-component consistency, configuration validation
- **BroadcastIntentTest**: Intent action patterns, broadcast security, component integration
- **SecurityAndEdgeCaseTest**: Security validation, edge cases, null safety, resource exhaustion prevention

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

## Emergency Communication Channels

The app uses multiple communication channels that work in different scenarios:

### SMS Emergency Broadcasts
- **Technology**: SMS over cellular signaling network (MAP/SS7 protocol)
- **Requirements**: Cellular network infrastructure (NOT WiFi or mobile data)
- **Availability**:
  - ✅ Works when mobile data is down (cellular network still operational)
  - ✅ Works when internet backbone fails (cellular network still operational)
  - ❌ Does NOT work over WiFi networks
  - ❌ Does NOT work when cellular network completely fails
- **Use Case**: Notify emergency contacts who may not have the app installed
- **Configuration**: Enable in Settings and configure emergency contacts

### WiFi-Based Communication
- **Technology**: WiFi scanning, hotspot creation, WiFi Direct
- **Requirements**: Power at WiFi access points, device WiFi capability
- **Availability**:
  - ✅ Works independently of cellular infrastructure
  - ✅ Works when mobile data fails
  - ✅ Works when internet backbone fails
  - ✅ Local propagation even with complete infrastructure failure
  - ⚠️ Limited range (typically 50-100 meters)
- **Use Case**: Ad-hoc networking when traditional infrastructure fails

### Bluetooth Communication
- **Technology**: Bluetooth discovery, advertising, mesh networking
- **Requirements**: Bluetooth enabled on devices
- **Availability**:
  - ✅ Works completely independently of all infrastructure
  - ✅ Works in all failure scenarios
  - ⚠️ Shorter range than WiFi (typically 10-50 meters)
- **Use Case**: Short-range peer-to-peer emergency networking

**Important**: SMS and WiFi are DIFFERENT channels. SMS cannot work over WiFi - it requires cellular network. The simulation now correctly models which communication channels are available in different infrastructure failure scenarios.

## Documentation

For detailed information about the project, see:

- **[RELEASE_KEYSTORE_SETUP.md](RELEASE_KEYSTORE_SETUP.md)**: Guide for setting up release keystore to enable updateable APKs
- **[RELEASE_WORKFLOW.md](RELEASE_WORKFLOW.md)**: Automated build and release workflow documentation
- **[EMERGENCY_PROTOCOL_SPEC.md](EMERGENCY_PROTOCOL_SPEC.md)**: Standard Emergency Protocol Specification (SEPS) v1.0
- **[INTEROPERABILITY_GUIDE.md](INTEROPERABILITY_GUIDE.md)**: Guide for implementing SEPS in other emergency apps
- **[NOTFALL_SZENARIEN.md](NOTFALL_SZENARIEN.md)**: Comprehensive emergency scenarios, flowcharts, and improvement recommendations (German)
- **[IMPLEMENTATION.md](IMPLEMENTATION.md)**: Technical implementation details
- **[FLOW.md](FLOW.md)**: System flow diagrams and architecture
- **[SUMMARY.md](SUMMARY.md)**: Project summary and overview
- **[TESTING.md](TESTING.md)**: Testing guide and procedures
- **[MESH_ROUTING.md](MESH_ROUTING.md)**: Mesh network routing implementation and protocol details
- **[TRUST_SYSTEM.md](TRUST_SYSTEM.md)**: Trust-based message evaluation system with algorithm documentation
- **[OFFLINE_MAP.md](OFFLINE_MAP.md)**: Offline map integration with OSM, participant tracking, safe zones, and routing
- **[SECURE_CONNECTIONS.md](SECURE_CONNECTIONS.md)**: Secure connection tracking in mesh routing
- **[SIMULATION.md](SIMULATION.md)**: Emergency propagation simulation tool documentation
- **[WIFI_AUTO_CONNECT.md](WIFI_AUTO_CONNECT.md)**: WiFi auto-connect feature for expanding mesh network coverage

## Future Enhancements

- [ ] Peer-to-peer messaging over ad-hoc connections
- [x] **Mesh network with multi-hop routing** - Implemented! Messages relay through intermediate devices
- [x] **Emergency location sharing** - Implemented! GPS coordinates broadcast to network participants
- [x] **Help requests with location** - Implemented! Send emergency help requests with GPS position
- [x] **Standard emergency protocols (SEPS)** - Implemented! Interoperability with other emergency apps
- [x] **Trust-based message evaluation** - Implemented! Evaluate message trustworthiness based on sender trust, hop count, and connection security
- [x] **Secure connection tracking** - Implemented! Track and prefer secure routes in mesh network
- [x] **Emergency propagation simulation** - Implemented! Visual simulation of message spread through network
- [x] **Interactive map visualization with real-time updates** - Implemented! OSM-based offline maps with live participant tracking
- [x] **Offline map integration** - Implemented! OpenStreetMap-based offline maps with safe zones and routing
- [ ] Advanced battery optimization for standby mode
- [x] **Flashlight Morse code signaling** - Implemented! Visual emergency signals using LED
- [x] **Ultrasound signaling** - Implemented! Inaudible audio-based emergency beacon
- [x] Support for WiFi Direct communication
- [x] SMS emergency broadcast to contacts
- [x] **NFC tap-to-join for quick network setup** - Implemented! Tap devices together to share credentials
- [ ] Contact-based emergency signaling (calling frequent contacts)
- [ ] iOS version for cross-platform compatibility
- [ ] Desktop/Web client for emergency coordination centers

For a complete list of potential improvements with detailed analysis, see [NOTFALL_SZENARIEN.md](NOTFALL_SZENARIEN.md#verbesserungsvorschläge).

## License

This is a concept implementation for emergency communication.
