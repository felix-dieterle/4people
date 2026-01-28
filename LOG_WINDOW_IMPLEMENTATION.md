# Log Window Implementation

## Overview

This implementation adds a comprehensive log window to the 4people emergency communication app that tracks and displays all actions, events, messages, and state changes in real-time.

## Features

### 1. LogManager Singleton (`util/LogManager.kt`)

A centralized logging manager that collects and manages all log entries:

**Log Levels:**
- `INFO` - General information (Blue)
- `WARNING` - Warning messages (Orange)
- `ERROR` - Error messages (Red)
- `EVENT` - User actions and system events (Green)
- `STATE_CHANGE` - State transitions (Purple)
- `MESSAGE` - Communication messages (Cyan)

**Key Features:**
- Thread-safe log collection using `CopyOnWriteArrayList`
- Maximum 1000 log entries (automatically removes oldest)
- Real-time listener notifications for UI updates
- Timestamp for each entry (millisecond precision)
- Formatted output with timestamps and log levels

**Usage:**
```kotlin
// Log different types of entries
LogManager.logInfo("Tag", "Information message")
LogManager.logWarning("Tag", "Warning message")
LogManager.logError("Tag", "Error message")
LogManager.logEvent("Tag", "User clicked button")
LogManager.logStateChange("Tag", "Service started")
LogManager.logMessage("Tag", "Message received from device-123")

// Get all log entries
val logs = LogManager.getLogEntries()

// Clear all logs
LogManager.clearLogs()

// Register a listener for real-time updates
LogManager.addListener(object : LogManager.LogListener {
    override fun onNewLogEntry(entry: LogManager.LogEntry) {
        // Handle new log entry
    }
})
```

### 2. LogWindowActivity

A dedicated activity that displays all log entries in a scrollable list:

**Features:**
- RecyclerView-based list for efficient scrolling
- Real-time updates as new logs are added
- Auto-scroll to newest entries
- Color-coded log levels for easy visual scanning
- Clear logs button in the toolbar
- Empty state message when no logs exist
- Each log entry displays:
  - Timestamp (HH:mm:ss.SSS format)
  - Log level (color-coded and bold)
  - Component tag
  - Message

**Layout Files:**
- `activity_log_window.xml` - Main activity layout
- `item_log_entry.xml` - Individual log entry card layout
- `menu_log_window.xml` - Toolbar menu with clear button

### 3. Integration Points

The LogManager has been integrated into key components of the app:

#### MainActivity
- Application startup
- Emergency mode activation/deactivation
- Panic mode activation/deactivation
- Help request sending
- State changes (Bluetooth, WiFi, Hotspot, Location)
- Emergency detection from other devices
- Infrastructure status updates
- Infrastructure failures

#### AdHocCommunicationService
- Emergency mode starting/stopping
- Communication channel activation (Bluetooth, WiFi, Hotspot, WiFi Direct)
- Mesh message reception
- Help request handling
- Emergency message detection

**Example Log Entries:**
```
14:32:15.123 [INFO] MainActivity: Application started
14:32:22.456 [EVENT] MainActivity: Emergency mode activated by user
14:32:22.789 [STATE_CHANGE] AdHocCommService: Emergency mode starting
14:32:23.012 [STATE_CHANGE] AdHocCommService: Bluetooth channel activated
14:32:23.234 [STATE_CHANGE] AdHocCommService: WiFi scanning activated
14:32:23.456 [STATE_CHANGE] MainActivity: Bluetooth: active
14:32:45.890 [MESSAGE] AdHocCommService: Mesh message from device-abc123: LOCATION_UPDATE
14:33:12.234 [EVENT] AdHocCommService: Help request from device-xyz789 (5.2km)
14:34:01.567 [WARNING] MainActivity: Infrastructure failure: Cellular network unavailable
14:35:15.678 [EVENT] MainActivity: Panic mode activated by user
```

### 4. UI Integration

A new log button (info icon) has been added to the MainActivity toolbar:
- Located next to the settings button
- Opens LogWindowActivity when clicked
- Accessible at any time during app usage

## Technical Details

### Thread Safety

The LogManager uses `CopyOnWriteArrayList` for both log entries and listeners, ensuring thread-safe operations from multiple components including:
- Main UI thread
- Background services
- Broadcast receivers
- Mesh networking threads

### Memory Management

- Maximum of 1000 log entries maintained
- Oldest entries automatically removed when limit is exceeded
- Listener exceptions are caught to prevent cascading failures
- RecyclerView adapter efficiently handles large lists

### Performance Considerations

- Real-time updates use listener pattern to avoid polling
- RecyclerView for efficient scrolling and memory usage
- Log entries use immutable data classes
- Formatted strings are computed on-demand

## Usage Examples

### Viewing Logs

1. Open the 4people app
2. Tap the info (ℹ️) button in the top-right corner of the main screen
3. Scroll through the log entries
4. Tap "Clear Logs" to remove all entries (with confirmation dialog)

### Adding Logs to New Components

```kotlin
import com.fourpeople.adhoc.util.LogManager

class MyNewComponent {
    fun performAction() {
        LogManager.logEvent("MyComponent", "Action performed")
        
        try {
            // Do something
            LogManager.logStateChange("MyComponent", "State changed to X")
        } catch (e: Exception) {
            LogManager.logError("MyComponent", "Failed to perform action: ${e.message}")
        }
    }
}
```

## Testing

The log window functionality can be tested by:

1. **App Lifecycle:**
   - Open the app → See "Application started" log
   - Close and reopen → See new "Application started" log

2. **Emergency Mode:**
   - Activate emergency mode → See activation event and state changes
   - Deactivate → See deactivation event

3. **Panic Mode:**
   - Activate panic mode → See activation event
   - Deactivate → See deactivation event

4. **Communication:**
   - Send help request → See event with radius
   - Receive mesh message → See message log
   - Detect emergency → See detection log

5. **State Changes:**
   - Toggle services → See state change logs for each component
   - Infrastructure changes → See status updates and failures

6. **Log Management:**
   - Add many entries → Verify scrolling works
   - Clear logs → Verify confirmation and clearing works
   - Close and reopen log window → Verify entries persist

## Future Enhancements

Potential improvements:

1. **Filtering:** Add filters for log levels, components, or time ranges
2. **Search:** Search functionality for finding specific log entries
3. **Export:** Export logs to file for sharing or debugging
4. **Persistence:** Save logs to disk for survival across app restarts
5. **Log Levels Control:** Settings to enable/disable specific log levels
6. **Performance Stats:** Show statistics about events and messages
7. **Dark Mode:** Support for dark theme
8. **Timestamps:** Relative timestamps ("2 minutes ago")

## Files Changed/Added

### New Files:
- `app/src/main/java/com/fourpeople/adhoc/util/LogManager.kt`
- `app/src/main/java/com/fourpeople/adhoc/LogWindowActivity.kt`
- `app/src/main/res/layout/activity_log_window.xml`
- `app/src/main/res/layout/item_log_entry.xml`
- `app/src/main/res/menu/menu_log_window.xml`

### Modified Files:
- `app/src/main/AndroidManifest.xml` - Added LogWindowActivity
- `app/src/main/res/layout/activity_main.xml` - Added log button
- `app/src/main/java/com/fourpeople/adhoc/MainActivity.kt` - Added logging calls and button handler
- `app/src/main/java/com/fourpeople/adhoc/service/AdHocCommunicationService.kt` - Added logging calls

## Compatibility

- Minimum SDK: 26 (Android 8.0 Oreo)
- Target SDK: 34 (Android 14)
- Uses standard Android components (RecyclerView, Material Design)
- No additional dependencies required

## Summary

The log window implementation provides a powerful debugging and monitoring tool for the 4people emergency communication app. It captures all significant events, state changes, and messages, making it easy to:

- Debug issues during development
- Monitor app behavior in real-time
- Understand the flow of emergency communications
- Track infrastructure health
- Verify proper operation of all features

The implementation is lightweight, thread-safe, and integrates seamlessly with the existing codebase without affecting app performance.
