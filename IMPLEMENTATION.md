# Implementation Details - Ad-hoc Emergency Communication

## Overview

This implementation fulfills the requirement for ad-hoc emergency communication when all traditional infrastructure fails (cable, mobile data, and telephone).

## Key Features Implemented

### 1. Single-Click Activation
- MainActivity provides a single button to activate/deactivate emergency mode
- Handles all required Android permissions automatically
- Shows real-time status of all communication channels
- Settings access for configuring standby and auto-activation

### 2. Multi-Channel Communication

#### Bluetooth
- Activates Bluetooth discovery to find nearby devices
- Makes device discoverable with emergency naming pattern: `4people-<unique-id>`
- Continuously scans for other emergency devices
- Automatically detects other devices with the same app pattern

#### WiFi Scanning
- **Active Mode**: Scans for WiFi networks every 10 seconds
- **Standby Mode**: Scans for WiFi networks every 30 seconds
- Detects emergency networks with pattern: `4people-<unique-id>`
- Logs and notifies when emergency networks are detected

#### Hotspot
- Attempts to create a local WiFi hotspot
- Uses emergency naming pattern for automatic recognition
- Note: Android 8+ restricts hotspot creation; uses LocalOnlyHotspot API where available

### 3. Standby Mode (Enhanced)
- BootReceiver starts StandbyMonitoringService on device boot
- Periodic WiFi scanning at 30-second intervals (battery optimized)
- Phone call indicator detection
- Low battery consumption in standby
- Can automatically activate or notify user when emergency is detected

### 4. Emergency Detection (Enhanced)
- Automatic pattern recognition: `4people-<id>`
- Works across Bluetooth device names and WiFi SSIDs
- **Phone Call Indicators**: Detects brief incoming calls (< 5 seconds) as emergency signals
- Broadcasts emergency status to other nearby devices
- User-configurable auto-activation or notification preference
- EmergencyBroadcastReceiver handles incoming emergency signals

### 5. User Configuration
- Settings activity for managing standby monitoring
- Toggle auto-activation on emergency detection
- Manual control over standby monitoring service

## Architecture

### Service Layer
**AdHocCommunicationService** (Foreground Service)
- Manages all communication channels in active emergency mode
- Runs as foreground service to prevent Android from killing it
- Shows persistent notification when active
- Implements:
  - Bluetooth discovery and advertising
  - WiFi scanning with periodic updates (10 seconds)
  - Hotspot creation (platform-dependent)
  - Emergency broadcast handling

**StandbyMonitoringService** (Foreground Service)
- Lightweight background monitoring service
- Periodic WiFi scanning at 30-second intervals (battery optimized)
- Listens for phone call indicators
- Can auto-activate emergency mode or show notification
- Runs continuously after boot

### Receiver Layer
**BootReceiver**
- Starts StandbyMonitoringService on device boot
- Initializes standby mode automatically
- Ready for emergency activation

**EmergencyBroadcastReceiver**
- Listens for emergency broadcasts
- Can trigger automatic activation
- Notifies user of detected emergencies

**PhoneCallIndicatorReceiver**
- Monitors phone state changes
- Detects brief incoming calls (< 5 seconds)
- Sends emergency indicators to StandbyMonitoringService

### UI Layer
**MainActivity**
- Emergency activation/deactivation button
- Real-time status display
- Permission management
- User notifications
- Settings access

**SettingsActivity**
- Configure standby monitoring
- Toggle auto-activation preference
- View emergency indicators information

## Permission Handling

The app implements runtime permission requests for:
- Bluetooth (BLUETOOTH_SCAN, BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE)
- Location (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
- WiFi (ACCESS_WIFI_STATE, CHANGE_WIFI_STATE)
- Phone State (READ_PHONE_STATE for call indicator detection)
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
   - Active Mode: WiFi scans every 10 seconds
   - Standby Mode: WiFi scans every 30 seconds
2. Monitor phone state for brief incoming calls (< 5 seconds)
3. For each discovered device/network/indicator:
   - Check if name starts with "4people-" or if it's a phone indicator
   - If match found, trigger emergency detection
   - Broadcast to app components
   - Notify user or auto-activate based on preferences

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
2. Enable standby monitoring in Settings
3. Test phone call indicator:
   - Call one device and hang up immediately
   - Verify brief call is detected as emergency indicator
4. Activate emergency mode on one device
5. Verify other devices detect the emergency pattern
6. Test auto-activation vs. notification preference
7. Check Bluetooth device name changes
8. Verify WiFi scanning detects emergency networks
9. Test permission flows
10. Test boot receiver initialization
11. Verify standby mode battery consumption

### Integration Testing
- Test with airplane mode enabled
- Test with WiFi/mobile data disabled
- Test cross-device detection
- Test battery consumption over time in standby
- Test in areas with many WiFi networks
- Test phone call indicator with various call durations
- Test auto-activation behavior

## Known Limitations

1. **Hotspot Creation**: Restricted on Android 8+, requires LocalOnlyHotspot API
2. **Background Scanning**: May be limited by manufacturer restrictions
3. **Battery Consumption**: Active scanning drains battery faster
4. **Range**: Bluetooth limited to ~10-100m, WiFi to ~50-100m
5. **Permissions**: Requires multiple sensitive permissions

## Future Enhancements

### Short-term
- [x] Battery optimization for standby mode (30s scan intervals)
- [x] Automatic activation when emergency detected
- [ ] Better hotspot management UI
- [ ] Signal strength indicators
- [x] Phone call indicator detection

### Medium-term
- [ ] Peer-to-peer messaging protocol
- [ ] Mesh network formation
- [ ] Emergency location sharing
- [ ] Offline map integration
- [ ] Contact-based emergency signaling (calling frequent contacts)

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
