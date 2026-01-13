# Panic Mode Feature Documentation

## Overview

Panic Mode is a safety feature that provides automatic escalation of alerts when a user is in distress and cannot respond to regular check-ins. It's designed for situations where a person needs ongoing monitoring and quick intervention if they become incapacitated or unable to respond.

## How It Works

Panic Mode operates through four distinct phases with progressive escalation:

### Phase 1: Confirmation Phase
- **Duration**: Ongoing (every 30 seconds)
- **Behavior**: 
  - App requests user confirmation via notification
  - User must tap "I'm OK" button to confirm
  - If confirmed, cycle resets and continues
  
### Phase 2: Gentle Warning Phase
- **Trigger**: User doesn't confirm within 30 seconds
- **Duration**: 30 seconds
- **Behavior**:
  - Gentle notification (configurable in settings)
  - Options: Vibration only, Sound only, or Both
  - Default: Vibration only
  - User can still confirm and reset the cycle

### Phase 3: Massive Alert Phase
- **Trigger**: User doesn't confirm for 60 seconds total (30s + 30s)
- **Duration**: 2 minutes
- **Behavior**:
  - **Flashlight**: LED flashes SOS pattern in Morse code
  - **Alarm**: Loud alarm sound at full volume
  - **Vibration**: Strong vibration pattern
  - **GPS**: Captures current location
  - **Backend Notification**: Sends alert to backend with:
    - GPS coordinates
    - Signal strength (cellular)
    - Timestamp
  - **Auto-activate Networks** (optional, configurable):
    - Attempts to enable WiFi
    - Attempts to enable mobile data
  - User can still confirm to stop escalation

### Phase 4: Contact Notification Phase
- **Trigger**: User doesn't confirm for 3 minutes total (30s + 30s + 2min)
- **Behavior**:
  - Contacts emergency contacts progressively
  - First contact: Immediate notification
  - Second contact: After 3 minutes
  - Third contact: After 6 minutes (3 × 2)
  - Fourth contact: After 12 minutes (6 × 2)
  - Pattern continues, doubling interval each time
  - Each contact receives:
    - SMS message with panic alert
    - Last known GPS location
    - Signal strength information
    - Timestamp

## User Interface

### Main Activation
- **Button**: Red "ACTIVATE PANIC MODE" button in MainActivity
- **Location**: Below emergency help request buttons
- **Confirmation**: Requires user confirmation before activation

### Widget
- **Home Screen Widget**: "Panic Mode" widget for quick activation
- **Appearance**: Red button with panic mode icon
- **Usage**: Single tap to activate panic mode

### Notification
- **Persistent Notification**: Shows current phase
- **Actions**:
  - "I'm OK" button - Confirms user is safe
  - "Stop" button - Deactivates panic mode
- **Priority**: High priority with sound and vibration

## Settings

Panic Mode settings are available in Settings → Panic Mode Settings:

### Gentle Warning Type
- **Vibration only** (default): Discrete warning
- **Sound only**: Audio notification
- **Vibration and Sound**: Both types of alert

### Auto-activate Data/WiFi
- When enabled, panic mode will attempt to activate mobile data and WiFi during the massive alert phase
- This ensures better connectivity for sending backend notifications
- **Note**: On Android 10+, WiFi cannot be enabled programmatically

### Emergency Contacts
- Shared with regular emergency mode SMS feature
- Configure via Settings → SMS Emergency Broadcast → Configure Contacts
- Supports multiple contacts
- Each contact receives progressive notifications during Phase 4

## Technical Details

### Timing Constants
```kotlin
CONFIRMATION_INTERVAL = 30000L        // 30 seconds
GENTLE_WARNING_DURATION = 30000L      // 30 seconds  
MASSIVE_ALERT_DURATION = 120000L      // 2 minutes
CONTACT_NOTIFICATION_INITIAL_INTERVAL = 180000L  // 3 minutes
```

### Total Timeline
- 0:00 - Panic mode activated
- 0:30 - Gentle warning starts (no confirmation)
- 1:00 - Massive alert starts (still no confirmation)
- 3:00 - Contact notification phase starts (still no confirmation)
- 3:00 - First emergency contact notified
- 6:00 - Second emergency contact notified
- 12:00 - Third emergency contact notified
- 24:00 - Fourth emergency contact notified
- (Pattern continues with doubling intervals)

### Permissions Required
- **ACCESS_FINE_LOCATION**: For GPS location capture
- **VIBRATE**: For vibration alerts
- **CAMERA**: For flashlight/LED control
- **SEND_SMS**: For emergency contact notifications
- **CHANGE_WIFI_STATE**: For WiFi activation (optional)

### Background Service
- Runs as foreground service with persistent notification
- Uses Android alarm/handler system for timing
- Survives app process termination
- Can be stopped by user action or system

## Use Cases

### 1. Solo Activities
- Hiking alone in remote areas
- Jogging in unfamiliar neighborhoods
- Working alone late at night
- Traveling alone

### 2. Medical Conditions
- People with chronic conditions that may cause sudden incapacity
- Epilepsy, diabetes, heart conditions, etc.
- Regular check-ins provide safety net

### 3. Dangerous Situations
- Walking home alone at night
- Meeting strangers from online platforms
- Entering potentially unsafe areas
- Any situation where quick help might be needed

### 4. Elderly Care
- Monitoring elderly family members
- Quick response if person becomes unresponsive
- Automatic escalation ensures help arrives

## Important Notes

### Battery Consumption
- Moderate battery usage due to:
  - GPS location updates
  - Regular confirmation checks
  - Foreground service
- Recommended to use when device has adequate charge
- Consider charging during extended panic mode sessions

### False Alarms
- User must actively confirm every 30 seconds
- This prevents accidental activations from causing alerts
- Gentle warning phase provides buffer before massive alerts
- User can stop panic mode at any time

### Network Requirements
- **GPS**: Works without network, but accuracy may vary
- **Backend notifications**: Requires internet connection (WiFi or mobile data)
- **SMS to contacts**: Requires cellular network (SMS capability)
- Auto-activate network feature helps ensure connectivity

### Privacy Considerations
- Location data sent to backend during panic mode
- Emergency contacts receive location information
- All data transmission occurs only during active panic mode
- User has full control over activation/deactivation

## Backend Integration

The panic mode sends structured data to backend (placeholder implementation):

```kotlin
{
  "event": "panic_mode_alert",
  "latitude": <GPS latitude>,
  "longitude": <GPS longitude>,
  "signal_strength": <dBm value>,
  "timestamp": <Unix timestamp>
}
```

This can be extended to integrate with emergency response systems or monitoring services.

## Testing

Tests are available in:
- `app/src/test/java/com/fourpeople/adhoc/PanicModeServiceTest.kt`
- `app/src/test/java/com/fourpeople/adhoc/PanicWidgetTest.kt`

Run tests with:
```bash
./gradlew test
```

## Future Enhancements

Potential improvements for panic mode:
1. **Video/Audio Recording**: Start recording during massive alert phase
2. **Photo Capture**: Take photos and send to backend
3. **Call Emergency Services**: Option to automatically call 911/112
4. **Two-way Communication**: Allow contacts to respond and check status
5. **Panic Mode Sharing**: Share panic mode status with trusted contacts in real-time
6. **Geofencing**: Activate panic mode when entering/exiting specific areas
7. **Schedule Support**: Activate panic mode automatically at specific times
8. **Wearable Integration**: Control panic mode from smartwatch
