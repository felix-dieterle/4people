# SEPS Protocol Implementation

This directory contains the implementation of the Standard Emergency Protocol Specification (SEPS) v1.0 for the 4people app.

## Files

- **SepsMessage.kt** - Core SEPS message data structures and JSON serialization
- **SepsCodec.kt** - Conversion utilities between internal formats and SEPS
- **SepsProtocolHandler.kt** - Protocol handler for receiving and processing SEPS messages

## Purpose

The SEPS implementation enables interoperability with other emergency communication apps. Any app that implements SEPS can:
- Exchange emergency alerts and help requests
- Share location information
- Relay messages through mixed app networks
- Collaborate during infrastructure failures

## Usage

### Creating SEPS Messages

```kotlin
// Emergency alert
val alert = SepsCodec.createEmergencyAlert(
    deviceId = "device-123",
    severity = "EXTREME",
    category = "INFRASTRUCTURE_FAILURE",
    description = "Power outage in city center",
    location = SepsLocation(52.5200, 13.4050, 10.0)
)

// Help request
val helpRequest = SepsCodec.createHelpRequest(
    deviceId = "device-123",
    urgency = "IMMEDIATE",
    helpType = "MEDICAL",
    description = "Person injured, needs assistance",
    location = SepsLocation(52.5210, 13.4060, 5.0)
)
```

### Converting Between Formats

```kotlin
// Convert internal MeshMessage to SEPS format
val meshMessage = MeshMessage(/* ... */)
val sepsMessage = SepsCodec.meshMessageToSeps(meshMessage, deviceId)

// Convert SEPS message to internal format
val sepsMessage = SepsMessage.fromJsonString(jsonString)
val meshMessage = SepsCodec.sepsToMeshMessage(sepsMessage)
```

### Processing Incoming SEPS Messages

```kotlin
val handler = SepsProtocolHandler(context, deviceId, meshRoutingManager)

// Try to process data as SEPS message
val handled = handler.processIncomingData(receivedData)
if (handled) {
    // Was a SEPS message, processed successfully
} else {
    // Not SEPS, handle with other protocols
}
```

### Device Naming

For SEPS-compliant device discovery:

```kotlin
// Get SEPS device name
val sepsName = SepsProtocolHandler.getSepsDeviceName(deviceId)
// Returns: "SEPS-4people-<deviceId>"

// Check if a device is SEPS-compliant
if (SepsProtocolHandler.isSepsDevice(discoveredName)) {
    // This is a SEPS device, can exchange messages
}
```

## Integration Points

To fully integrate SEPS into the app:

1. **Device Discovery**: Advertise using SEPS naming pattern in addition to legacy pattern
2. **Message Processing**: Check incoming messages with `SepsProtocolHandler.processIncomingData()`
3. **Message Broadcasting**: Convert outgoing messages to SEPS format for interoperability
4. **Callbacks**: Register handlers for SEPS-specific events (emergency alerts, help requests, etc.)

## Testing

See the test directory for comprehensive tests:
- `SepsMessageTest.kt` - Message serialization/deserialization tests
- `SepsCodecTest.kt` - Codec conversion tests
- `SepsProtocolHandlerTest.kt` - Protocol handler tests

Run tests with:
```bash
./gradlew test --tests "com.fourpeople.adhoc.protocol.*"
```

## Documentation

- **Protocol Specification**: [/EMERGENCY_PROTOCOL_SPEC.md](../../EMERGENCY_PROTOCOL_SPEC.md)
- **Interoperability Guide**: [/INTEROPERABILITY_GUIDE.md](../../INTEROPERABILITY_GUIDE.md)
- **Message Types**: See SepsMessageType enum in SepsMessage.kt

## Version Compatibility

This implementation supports SEPS v1.0. Future versions will maintain backward compatibility:
- Accept messages from v1.x (e.g., v1.0, v1.1, v1.2)
- Ignore unknown fields for forward compatibility
- Provide sensible defaults for missing optional fields

## License

This SEPS implementation is part of the 4people app and is released under the MIT License.
The SEPS specification itself is CC0 1.0 Universal (Public Domain).
