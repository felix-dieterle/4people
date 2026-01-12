# Implementation Details - Ad-hoc Emergency Communication

## Overview

This implementation fulfills the requirement for ad-hoc emergency communication when all traditional infrastructure fails (cable, mobile data, and telephone).

## Key Features Implemented

### 1. Single-Click Activation
- MainActivity provides a single button to activate/deactivate emergency mode
- Handles all required Android permissions automatically
- Shows real-time status of all communication channels

### 2. Multi-Channel Communication

#### Bluetooth
- Activates Bluetooth discovery to find nearby devices
- Makes device discoverable with emergency naming pattern: `4people-<unique-id>`
- Continuously scans for other emergency devices
- Automatically detects other devices with the same app pattern

#### WiFi Scanning
- Scans for WiFi networks every 10 seconds
- Detects emergency networks with pattern: `4people-<unique-id>`
- Logs and notifies when emergency networks are detected

#### Hotspot
- Attempts to create a local WiFi hotspot
- Uses emergency naming pattern for automatic recognition
- Note: Android 8+ restricts hotspot creation; uses LocalOnlyHotspot API where available

### 3. Standby Mode
- BootReceiver starts on device boot
- App is ready to receive emergency broadcasts
- Low battery consumption in standby
- Can be activated by detecting emergency patterns from other devices

### 4. Emergency Detection
- Automatic pattern recognition: `4people-<id>`
- Works across Bluetooth device names and WiFi SSIDs
- Broadcasts emergency status to other nearby devices
- EmergencyBroadcastReceiver handles incoming emergency signals

## Architecture

### Service Layer
**AdHocCommunicationService** (Foreground Service)
- Manages all communication channels
- Runs as foreground service to prevent Android from killing it
- Shows persistent notification when active
- Implements:
  - Bluetooth discovery and advertising
  - WiFi scanning with periodic updates
  - Hotspot creation (platform-dependent)
  - Emergency broadcast handling

### Receiver Layer
**BootReceiver**
- Starts on device boot
- Initializes standby mode
- Ready for emergency activation

**EmergencyBroadcastReceiver**
- Listens for emergency broadcasts
- Can trigger automatic activation
- Notifies user of detected emergencies

### UI Layer
**MainActivity**
- Emergency activation/deactivation button
- Real-time status display
- Permission management
- User notifications

## Permission Handling

The app implements runtime permission requests for:
- Bluetooth (BLUETOOTH_SCAN, BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE)
- Location (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
- WiFi (ACCESS_WIFI_STATE, CHANGE_WIFI_STATE)
- Notifications (POST_NOTIFICATIONS on Android 13+)

All permissions are requested with proper rationale dialogs.

## Emergency Pattern Protocol

### Device Naming
All devices in emergency mode use: `4people-<8-character-uuid>`

Example:
- Bluetooth: `4people-a1b2c3d4`
- WiFi SSID: `4people-a1b2c3d4` (where supported)

### Detection Algorithm
1. Start periodic scanning (Bluetooth + WiFi)
2. For each discovered device/network:
   - Check if name starts with "4people-"
   - If match found, trigger emergency detection
   - Broadcast to app components
   - Notify user

## Platform Considerations

### Android Version Compatibility
- Minimum SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)
- Handles API differences for Bluetooth and WiFi APIs

### Manufacturer Restrictions
- Some manufacturers limit background scanning
- Hotspot creation may require manual activation
- Battery optimization may affect standby mode

### Battery Optimization
- Service runs in foreground to prevent termination
- Periodic WiFi scanning (10-second intervals) balances detection vs. battery
- Bluetooth discovery is continuous when active

## Testing Recommendations

### Manual Testing
1. Install app on multiple devices
2. Activate emergency mode on one device
3. Verify other devices detect the emergency pattern
4. Check Bluetooth device name changes
5. Verify WiFi scanning detects emergency networks
6. Test permission flows
7. Test boot receiver initialization

### Integration Testing
- Test with airplane mode enabled
- Test with WiFi/mobile data disabled
- Test cross-device detection
- Test battery consumption over time
- Test in areas with many WiFi networks

## Known Limitations

1. **Hotspot Creation**: Restricted on Android 8+, requires LocalOnlyHotspot API
2. **Background Scanning**: May be limited by manufacturer restrictions
3. **Battery Consumption**: Active scanning drains battery faster
4. **Range**: Bluetooth limited to ~10-100m, WiFi to ~50-100m
5. **Permissions**: Requires multiple sensitive permissions

## Future Enhancements

### Short-term
- [ ] Battery optimization for standby mode
- [ ] Automatic activation when emergency detected
- [ ] Better hotspot management UI
- [ ] Signal strength indicators

### Medium-term
- [ ] Peer-to-peer messaging protocol
- [ ] Mesh network formation
- [ ] Emergency location sharing
- [ ] Offline map integration

### Long-term
- [ ] WiFi Direct support
- [ ] NFC tap-to-connect
- [ ] Cross-platform support (iOS)
- [ ] Encryption for secure communication

## Security Considerations

- All communication currently unencrypted (emergency scenarios prioritize availability)
- Device IDs are random UUIDs (not personally identifiable)
- No personal data transmitted in discovery phase
- Future versions should implement end-to-end encryption for messaging

## Code Quality

- Written in Kotlin with modern Android best practices
- Uses AndroidX libraries
- Implements proper lifecycle management
- Handles runtime permissions correctly
- Includes comprehensive error handling and logging
- Follows Material Design guidelines for UI
