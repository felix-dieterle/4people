# Error Logging to File System

## Overview

This implementation adds persistent error logging to the 4people application. Errors and crashes are now automatically logged to the file system, making it easier to investigate issues that occur in the field.

## Features

### 1. Centralized Error Logging (`ErrorLogger`)

The `ErrorLogger` utility class provides a centralized way to log errors to both Android's logcat and the file system.

**Key Features:**
- Automatic file rotation (keeps max 5 log files)
- Size-based log rotation (1 MB per file)
- Logs stored in app-specific storage (no permissions required)
- Thread-safe logging
- Timestamps for all log entries
- Full stack traces for exceptions

**Location:**
- Log files are stored in: `/data/data/com.fourpeople.adhoc/files/error_logs/`
- File naming pattern: `error_log_YYYYMMDD_HHmmss.log`

### 2. Global Crash Handler

The application now includes a global uncaught exception handler that automatically logs all crashes to the file system before the app terminates.

**Implementation:**
- Registered in `FourPeopleApplication.onCreate()`
- Logs crash information including thread name and full stack trace
- Does not interfere with Android's default crash handling

### 3. Integration with Existing Code

Error logging has been integrated into critical parts of the application:
- `AdHocCommunicationService` - All communication channel activation errors
- `WiFiConnectionHelper` - WiFi connection errors
- `MainActivity` - Service interaction errors

## Usage

### Basic Error Logging

```kotlin
import com.fourpeople.adhoc.util.ErrorLogger

// Log an error message
ErrorLogger.logError("MyTag", "Something went wrong")

// Log an error with an exception
try {
    riskyOperation()
} catch (e: Exception) {
    ErrorLogger.logError("MyTag", "Operation failed", e)
}
```

### Log a Warning

```kotlin
ErrorLogger.logWarning("MyTag", "This is a warning message")
```

### Access Log Files

```kotlin
// Get all log files (sorted by newest first)
val logFiles = ErrorLogger.getLogFiles()

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
2026-01-27 20:30:45.123 [ERROR] AdHocCommService: Failed to activate Bluetooth, continuing with other channels
    java.lang.SecurityException: Need BLUETOOTH permission
        at com.fourpeople.adhoc.service.AdHocCommunicationService.activateBluetooth(AdHocCommunicationService.kt:287)
        ...

```

## Log Rotation

The error logger automatically manages log files:

1. **Size Rotation**: When a log file exceeds 1 MB, a new file is created
2. **Count Rotation**: Only the 5 most recent log files are kept
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

- Logs are stored in app-specific storage, which is private to the application
- Log files are automatically removed when the app is uninstalled
- No sensitive user data should be logged
- Logs contain only error messages, exceptions, and stack traces

## Accessing Logs for Debugging

### During Development

Use Android Debug Bridge (adb) to access log files:

```bash
# List log files
adb shell ls -la /data/data/com.fourpeople.adhoc/files/error_logs/

# Pull a specific log file
adb pull /data/data/com.fourpeople.adhoc/files/error_logs/error_log_20260127_203045.log

# Pull all log files
adb pull /data/data/com.fourpeople.adhoc/files/error_logs/
```

### In Production

For production devices, you would need to:
1. Add a UI to view/export logs (not implemented yet)
2. Implement a log sharing feature (e.g., via email or cloud storage)
3. Use Android's backup/restore functionality

## Future Enhancements

Potential improvements for the error logging system:

1. **Settings UI**: Add settings to enable/disable file logging or adjust log levels
2. **Log Viewer**: In-app log viewer for users and support teams
3. **Log Export**: Allow users to export/share logs for support purposes
4. **Filtering**: Add log level filtering (DEBUG, INFO, WARN, ERROR)
5. **Remote Logging**: Optional remote logging to a server for analytics
6. **Compression**: Compress old log files to save space

## Integration Checklist

When adding error logging to new code:

- [ ] Import `ErrorLogger` in your class
- [ ] Replace `Log.e()` calls with `ErrorLogger.logError()`
- [ ] Replace `Log.w()` calls with `ErrorLogger.logWarning()`
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
2. Write the error to a file with timestamp
3. Include the full stack trace
4. Automatically rotate logs if needed
