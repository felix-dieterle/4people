# Error Logging to File System

## Overview

This implementation adds persistent error logging to the 4people application. Errors and crashes are now automatically logged to the file system in the Downloads directory, making it easier to investigate issues that occur in the field.

## Features

### 1. Centralized Error Logging (`ErrorLogger`)

The `ErrorLogger` utility class provides a centralized way to log errors to both Android's logcat and the file system.

**Key Features:**
- Automatic file rotation (keeps max 10 log files)
- Size-based log rotation (2 MB per file)
- **Logs stored in Downloads directory as .txt files for easy access**
- Thread-safe logging
- Timestamps for all log entries
- Full stack traces for exceptions
- **Logs include INFO, WARN, ERROR, and CRASH levels**

**Location:**
- **Android 10+**: `/Android/data/com.fourpeople.adhoc/files/Download/4people_logs/`
- **Android 9 and below**: `/Download/4people_logs/`
- **Fallback (if external storage unavailable)**: `/data/data/com.fourpeople.adhoc/files/4people_logs/`
- File naming pattern: `4people_log_YYYYMMDD_HHmmss.txt`

### 2. Global Crash Handler

The application now includes a global uncaught exception handler that automatically logs all crashes to the file system before the app terminates.

**Implementation:**
- Registered in `FourPeopleApplication.onCreate()`
- Logs crash information including thread name and full stack trace
- Does not interfere with Android's default crash handling

### 3. Comprehensive Startup Logging

Error logging has been integrated into all critical startup and lifecycle methods:
- **`FourPeopleApplication`** - Application initialization and crash handler setup
- **`MainActivity`** - All lifecycle methods (onCreate, onNewIntent, onResume)
- **`MainActivity` setup methods** - setupUI, setupTabs, setupLogView, registerEmergencyReceiver, setupNFC
- **`EmergencyWidget`** - Widget updates and emergency mode activation
- **`PanicWidget`** - Widget updates and panic mode activation
- **Intent handling** - All intent actions are logged for debugging

## Usage

### Basic Error Logging

```kotlin
import com.fourpeople.adhoc.util.ErrorLogger

// Log an info message
ErrorLogger.logInfo("MyTag", "Operation completed successfully")

// Log a warning message
ErrorLogger.logWarning("MyTag", "This is a warning message")

// Log an error message
ErrorLogger.logError("MyTag", "Something went wrong")

// Log an error with an exception
try {
    riskyOperation()
} catch (e: Exception) {
    ErrorLogger.logError("MyTag", "Operation failed", e)
}
```

### Access Log Files

```kotlin
// Get all log files (sorted by newest first)
val logFiles = ErrorLogger.getLogFiles()

// Get log directory path for display
val logPath = ErrorLogger.getLogDirectoryPath()

// Read log content
logFiles.firstOrNull()?.readText()?.let { content ->
    println(content)
}
```

### Clear All Logs

```kotlin
ErrorLogger.clearLogs()
```

## Log Format

Each log entry follows this format:

```
YYYY-MM-DD HH:mm:ss.SSS [LEVEL] TAG: Message
    Exception stack trace (if applicable)

```

**Example:**
```
2026-01-30 21:03:45.123 [INFO] FourPeopleApplication: Application started successfully
2026-01-30 21:03:45.124 [INFO] FourPeopleApplication: Log directory: /storage/emulated/0/Android/data/com.fourpeople.adhoc/files/Download/4people_logs
2026-01-30 21:03:45.200 [ERROR] MainActivity: onCreate called with action: android.intent.action.MAIN
2026-01-30 21:03:45.250 [INFO] MainActivity: setupUI started
2026-01-30 21:03:45.300 [ERROR] AdHocCommService: Failed to activate Bluetooth, continuing with other channels
    java.lang.SecurityException: Need BLUETOOTH permission
        at com.fourpeople.adhoc.service.AdHocCommunicationService.activateBluetooth(AdHocCommunicationService.kt:287)
        ...

```

## Log Rotation

The error logger automatically manages log files:

1. **Size Rotation**: When a log file exceeds 2 MB, a new file is created
2. **Count Rotation**: Only the 10 most recent log files are kept
3. **Automatic Cleanup**: Old files are automatically deleted when the limit is exceeded

This prevents excessive disk usage while maintaining enough history for debugging.

## Testing

Unit tests are provided in `ErrorLoggerTest.kt` to verify:
- Log file creation
- Error and warning logging
- Exception stack trace logging
- Crash logging
- Log file retrieval
- Log clearing functionality
- Basic rotation behavior

## Security Considerations

- Logs are stored in app-specific storage in the Downloads directory
- On Android 10+, logs are in a scoped directory accessible only to the app
- On older Android versions, logs are in the public Downloads directory
- Log files are automatically removed when the app is uninstalled (on Android 10+)
- No sensitive user data should be logged
- Logs contain only error messages, exceptions, and stack traces

## Accessing Logs for Debugging

### During Development

Use Android Debug Bridge (adb) to access log files:

```bash
# For Android 10+ (scoped storage)
adb shell ls -la /sdcard/Android/data/com.fourpeople.adhoc/files/Download/4people_logs/

# Pull a specific log file
adb pull /sdcard/Android/data/com.fourpeople.adhoc/files/Download/4people_logs/4people_log_20260130_210345.txt

# Pull all log files
adb pull /sdcard/Android/data/com.fourpeople.adhoc/files/Download/4people_logs/
```

### In Production

For production devices:
1. Users can access log files directly in their Downloads folder (via file manager)
2. Log files are .txt format and can be easily shared via email or messaging apps
3. The log directory path is displayed on app startup for easy reference

## Startup Problem Diagnosis

The enhanced logging now captures:
- **Application initialization** - Full startup sequence with timestamps
- **MainActivity lifecycle** - onCreate, onNewIntent, onResume with action details
- **All setup methods** - UI, tabs, log view, receivers, NFC
- **Intent handling** - All intent actions received (shortcuts, widgets, normal launch)
- **Widget interactions** - Emergency and panic widget clicks and state changes
- **All exceptions** - Complete stack traces with context

This makes it easy to identify:
- Why the app might close immediately after launch
- Which intent triggered the app launch
- Where in the initialization sequence failures occur
- Widget click handling issues
- Permission-related problems

## Future Enhancements

Potential improvements for the error logging system:

1. **Settings UI**: Add settings to enable/disable file logging or adjust log levels
2. **Log Viewer**: In-app log viewer for users and support teams
3. **Log Export**: Enhanced sharing options from within the app
4. **Filtering**: Add log level filtering (DEBUG, INFO, WARN, ERROR)
5. **Remote Logging**: Optional remote logging to a server for analytics
6. **Compression**: Compress old log files to save space

## Integration Checklist

When adding error logging to new code:

- [ ] Import `ErrorLogger` in your class
- [ ] Replace `Log.e()` calls with `ErrorLogger.logError()`
- [ ] Replace `Log.w()` calls with `ErrorLogger.logWarning()`
- [ ] Add `ErrorLogger.logInfo()` for important operations
- [ ] Ensure all caught exceptions are logged
- [ ] Add appropriate context in error messages
- [ ] Test that errors are logged correctly

## Example Integration

Before:
```kotlin
try {
    dangerousOperation()
} catch (e: Exception) {
    Log.e(TAG, "Operation failed", e)
}
```

After:
```kotlin
import com.fourpeople.adhoc.util.ErrorLogger

try {
    dangerousOperation()
} catch (e: Exception) {
    ErrorLogger.logError(TAG, "Operation failed", e)
}
```

The `ErrorLogger.logError()` method will:
1. Log to Android logcat (same as before)
2. Write the error to a .txt file in Downloads with timestamp
3. Include the full stack trace
4. Automatically rotate logs if needed
