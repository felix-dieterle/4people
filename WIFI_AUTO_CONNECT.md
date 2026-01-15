# WiFi Auto-Connect Feature

## Overview

The WiFi Auto-Connect feature enables devices in emergency mode to simultaneously:
1. **Maintain a WiFi hotspot** for other devices to connect to (where supported)
2. **Connect to existing emergency WiFi networks** to expand mesh coverage

This creates a dual-mode WiFi configuration where devices can act as both access points and clients, maximizing the reach and connectivity of the emergency mesh network.

## Purpose

In emergency situations, devices need to form a mesh network to relay messages across greater distances. By enabling devices to both broadcast a hotspot AND connect to other emergency networks, we create a more resilient and extensive mesh topology.

### Key Benefits

- **Extended Coverage**: Devices can relay messages through multiple hops
- **Automatic Network Formation**: No manual configuration needed
- **Dual-Mode Operation**: Act as both access point and client simultaneously (on supported devices)
- **Mesh Network Expansion**: Each connected device extends the network's reach

## Technical Implementation

### WiFiConnectionHelper Class

Located in: `app/src/main/java/com/fourpeople/adhoc/service/WiFiConnectionHelper.kt`

This helper class manages WiFi connections to emergency networks:

- **Android 10+ (API 29+)**: Uses `WifiNetworkSpecifier` with `ConnectivityManager`
- **Android 8-9 (API 26-28)**: Uses legacy `WifiConfiguration` API (deprecated but functional)

### Automatic Connection Logic

When emergency mode is active:

1. **WiFi Scanning**: Periodic scans for networks with pattern `4people-*` (every 10 seconds)
2. **Network Detection**: Identifies emergency networks from scan results
3. **Auto-Connect**: Attempts to connect to available emergency networks
4. **Status Tracking**: Updates UI with connection status

### Settings Integration

Users can control WiFi auto-connect behavior:

- **Setting**: "WiFi Auto-Connect" in Settings
- **Default**: Enabled (true)
- **Location**: Settings > WiFi Auto-Connect section
- **Storage**: Shared preferences (`emergency_prefs`, key: `wifi_auto_connect_enabled`)

## Android Version Compatibility

### Android 10+ (API 29+)

Uses `WifiNetworkSpecifier` with `NetworkRequest`:
```kotlin
val specifier = WifiNetworkSpecifier.Builder()
    .setSsid(ssid)
    .build()
    
val request = NetworkRequest.Builder()
    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
    .setNetworkSpecifier(specifier)
    .build()
    
connectivityManager.requestNetwork(request, callback)
```

**Advantages**:
- More modern API
- Better resource management
- Callback-based connection status

### Android 8-9 (API 26-28)

Uses legacy `WifiConfiguration` API:
```kotlin
val config = WifiConfiguration()
config.SSID = "\"$ssid\""
config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)

val networkId = wifiManager.addNetwork(config)
wifiManager.enableNetwork(networkId, true)
```

**Limitations**:
- Deprecated API
- May not work on all devices
- No callback for connection status

## Limitations and Considerations

### Simultaneous Hotspot and Client Mode

**Requirements**:
- Android 8.0+ (API 26+)
- Dual-band WiFi capability (2.4GHz + 5GHz)
- Device manufacturer support

**Reality**:
- Not all devices support this configuration
- LocalOnlyHotspot (Android 8+) may conflict with WiFi client mode
- Some manufacturers restrict this functionality

### Current Implementation Strategy

The implementation attempts to:
1. Create a LocalOnlyHotspot (if supported)
2. Connect to emergency WiFi networks as a client
3. Handle gracefully if one or both fail

**Fallback Behavior**:
- If hotspot creation fails: Continue with WiFi client mode
- If WiFi connection fails: Continue with hotspot mode
- If both fail: Fall back to Bluetooth mesh

### Network Security

**Current Implementation**:
- Emergency networks are assumed to be **open** (no password)
- This maximizes accessibility in emergency situations
- No sensitive data is transmitted over WiFi (only mesh routing metadata)

**Future Enhancements** (if needed):
- Support for password-protected networks
- Pre-shared key exchange via NFC
- Network encryption for sensitive data

## User Interface

### Status Indicators

Main Activity displays:
- **WiFi: Active/Inactive** - WiFi scanning status
- **Hotspot: Active/Inactive** - LocalOnlyHotspot status
- **WiFi Connected: Active/Inactive** - Connection to emergency network

### Settings

Settings Activity includes:
- **WiFi Auto-Connect** toggle switch
- Description: "Automatically connect to discovered emergency WiFi networks to expand mesh coverage"
- Toast notifications when setting is changed

## Testing

### Unit Tests

Located in: `app/src/test/java/com/fourpeople/adhoc/WiFiConnectionTest.kt`

Tests include:
- Emergency SSID pattern validation
- Network selection logic
- SSID format verification
- Edge cases (empty lists, special characters, length limits)

### Manual Testing

To test this feature:

1. **Enable WiFi Auto-Connect** in Settings
2. **Activate Emergency Mode** on Device A
3. **Activate Emergency Mode** on Device B
4. **Check Status** on both devices
   - Device A should show "Hotspot: Active"
   - Device B should show "WiFi Connected: Active" if connected to Device A
5. **Verify Mesh Routing** - Messages should relay between devices

## Integration with Mesh Network

The WiFi auto-connect feature integrates with the existing mesh routing system:

1. **Device Discovery**: Devices detect each other via WiFi scanning
2. **Connection**: WiFiConnectionHelper establishes connection
3. **Mesh Routing**: MeshRoutingManager handles message forwarding
4. **Multi-hop**: Messages relay through connected devices

### Mesh Topology Example

```
[Device A] <-- WiFi --> [Device B] <-- Bluetooth --> [Device C]
(Hotspot)              (WiFi Client)                 (BT Only)

Message from A to C:
A --WiFi--> B --Bluetooth--> C
```

## Permissions Required

The WiFi auto-connect feature requires:

- `ACCESS_WIFI_STATE` - Read WiFi state
- `CHANGE_WIFI_STATE` - Modify WiFi configuration
- `ACCESS_FINE_LOCATION` - Required for WiFi scanning (Android 6+)
- `CHANGE_NETWORK_STATE` - Modify network state (Android 10+)

All permissions are already declared in `AndroidManifest.xml`.

## Error Handling

The implementation includes robust error handling:

### Connection Failures

```kotlin
try {
    wifiConnectionHelper?.connectToEmergencyNetwork(ssid)
} catch (e: SecurityException) {
    Log.e(TAG, "Security exception when connecting", e)
    // Continue with other communication channels
}
```

### Graceful Degradation

If WiFi connection fails:
- Continue with hotspot mode (if active)
- Fall back to Bluetooth mesh
- Fall back to WiFi Direct
- No service interruption

### Status Updates

Connection status is broadcast to UI:
- Success: UI shows "WiFi Connected: ✓ Active"
- Failure: UI shows "WiFi Connected: ○ Inactive"
- Logs indicate failure reason

## Future Enhancements

Potential improvements to consider:

1. **Smart Network Selection**
   - Choose strongest signal network
   - Prefer trusted/verified devices
   - Avoid connection loops

2. **Network Credentials**
   - Support password-protected networks
   - Secure credential exchange via NFC
   - Pre-configured network passwords

3. **Connection Quality Monitoring**
   - Signal strength tracking
   - Automatic reconnection on signal loss
   - Switch to better networks when available

4. **Dual-Band Optimization**
   - Hotspot on 5GHz, client on 2.4GHz
   - Maximize simultaneous operation
   - Device capability detection

5. **Battery Optimization**
   - Adaptive connection intervals
   - Connection timeout for weak signals
   - Sleep mode when mesh is stable

## Documentation References

- [README.md](../README.md) - Main project documentation
- [MESH_ROUTING.md](MESH_ROUTING.md) - Mesh network routing details
- [IMPLEMENTATION.md](IMPLEMENTATION.md) - Technical implementation overview
- [TESTING.md](TESTING.md) - Testing procedures

## Implementation Files

### Source Code
- `WiFiConnectionHelper.kt` - WiFi connection management
- `AdHocCommunicationService.kt` - Service integration
- `SettingsActivity.kt` - Settings UI
- `MainActivity.kt` - Status display

### Resources
- `activity_settings.xml` - Settings UI layout
- `activity_main.xml` - Main UI layout
- `strings.xml` - String resources

### Tests
- `WiFiConnectionTest.kt` - Unit tests for WiFi connection logic

## License

Part of the 4people Emergency Communication App.
Concept implementation for ad-hoc emergency networking.
