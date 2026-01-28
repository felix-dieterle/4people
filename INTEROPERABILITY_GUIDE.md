# Interoperability Guide for Emergency Apps

## Overview

This guide explains how emergency communication apps can interoperate with the 4people app and other SEPS-compliant apps to create a unified emergency response network.

## Why Interoperability Matters

During infrastructure failures, having multiple incompatible emergency apps reduces the effectiveness of emergency communication. By supporting standard protocols, different apps can:

- **Share emergency alerts** across app boundaries
- **Extend network coverage** by relaying messages through any compatible app
- **Pool resources** for better emergency response
- **Prevent fragmentation** of emergency networks

## Standard Emergency Protocol Specification (SEPS)

The 4people app implements SEPS v1.0, a standard protocol for emergency communication. Full specification: [EMERGENCY_PROTOCOL_SPEC.md](EMERGENCY_PROTOCOL_SPEC.md)

### Key Features

- **JSON-based messages** for easy implementation
- **Multiple message types** for different emergency scenarios
- **Mesh routing** with automatic route discovery
- **Version negotiation** for backward compatibility
- **Optional encryption** for secure communications

## Quick Start for App Developers

### 1. Minimum Implementation (Level 1 Compliance)

To be SEPS-compliant at the basic level, your app must:

#### A. Use SEPS Naming Pattern

```kotlin
// Bluetooth device name
val bluetoothName = "SEPS-${yourAppId}-${deviceId}"
// Example: "SEPS-com.example.emergency-abc123"

// WiFi SSID (if creating hotspot)
val wifiSSID = "SEPS-${yourAppId}-${deviceId}"
```

#### B. Recognize SEPS Devices

```kotlin
fun isSepsDevice(name: String): Boolean {
    return name.startsWith("SEPS-")
}

// Scan for SEPS devices
if (isSepsDevice(discoveredDeviceName)) {
    // Connect and exchange messages
}
```

#### C. Send EMERGENCY_ALERT Messages

```kotlin
fun sendEmergencyAlert() {
    val message = JSONObject().apply {
        put("seps_version", "1.0")
        put("message_id", UUID.randomUUID().toString())
        put("timestamp", System.currentTimeMillis())
        put("sender", JSONObject().apply {
            put("app_id", "com.yourapp.emergency")
            put("device_id", deviceId)
            put("app_version", "1.0.0")
        })
        put("message_type", "EMERGENCY_ALERT")
        put("routing", JSONObject().apply {
            put("ttl", 10)
            put("hop_count", 0)
            put("destination", "BROADCAST")
            put("sequence", sequenceNumber++)
        })
        put("payload", JSONObject().apply {
            put("severity", "EXTREME")
            put("category", "INFRASTRUCTURE_FAILURE")
            put("description", "Emergency activated")
            put("contacts", networkSize)
        })
    }
    
    broadcast(message.toString())
}
```

#### D. Receive and Forward Messages

```kotlin
fun onMessageReceived(jsonString: String) {
    try {
        val message = JSONObject(jsonString)
        
        // Verify SEPS version
        if (!message.getString("seps_version").startsWith("1.")) {
            return // Unsupported version
        }
        
        // Process message
        processMessage(message)
        
        // Forward if needed
        val routing = message.getJSONObject("routing")
        if (routing.getInt("ttl") > 0) {
            forwardMessage(message)
        }
    } catch (e: Exception) {
        // Not a SEPS message
    }
}

fun forwardMessage(message: JSONObject) {
    val routing = message.getJSONObject("routing")
    routing.put("ttl", routing.getInt("ttl") - 1)
    routing.put("hop_count", routing.getInt("hop_count") + 1)
    broadcast(message.toString())
}
```

### 2. Recommended Implementation (Level 2 Compliance)

For better interoperability, implement:

#### A. HELLO Messages for Neighbor Discovery

```kotlin
// Send every 30 seconds
fun sendHello() {
    val message = createSepsMessage(
        messageType = "HELLO",
        payload = JSONObject().apply {
            put("app_capabilities", JSONArray().apply {
                put("SEPS_V1")
                put("LOCATION_SHARING")
                put("MESH_ROUTING")
            })
            put("supported_transports", JSONArray().apply {
                put("BLUETOOTH")
                put("WIFI_DIRECT")
            })
            put("battery_level", getBatteryLevel())
            put("active_participants", getNetworkSize())
        }
    )
    broadcast(message)
}
```

#### B. HELP_REQUEST Messages

```kotlin
fun sendHelpRequest(location: Location, helpType: String) {
    val message = createSepsMessage(
        messageType = "HELP_REQUEST",
        payload = JSONObject().apply {
            put("urgency", "IMMEDIATE")
            put("help_type", helpType) // "MEDICAL", "RESCUE", etc.
            put("description", "Need assistance")
            put("location", JSONObject().apply {
                put("latitude", location.latitude)
                put("longitude", location.longitude)
                put("accuracy", location.accuracy)
            })
            put("requester_status", "SAFE_BUT_NEED_HELP")
        }
    )
    broadcast(message)
}
```

#### C. LOCATION_UPDATE Messages

```kotlin
// Send every 30-60 seconds
fun sendLocationUpdate(location: Location) {
    val message = createSepsMessage(
        messageType = "LOCATION_UPDATE",
        payload = JSONObject().apply {
            put("location", JSONObject().apply {
                put("latitude", location.latitude)
                put("longitude", location.longitude)
                put("accuracy", location.accuracy)
            })
            put("status", "SAFE") // or "NEED_HELP", "INJURED", etc.
            put("battery_level", getBatteryLevel())
            put("network_size", getNetworkSize())
        }
    )
    broadcast(message)
}
```

### 3. Advanced Implementation (Level 3 Compliance)

For full feature parity:

- Implement all message types (see [EMERGENCY_PROTOCOL_SPEC.md](EMERGENCY_PROTOCOL_SPEC.md))
- Add cryptographic signatures
- Implement trust-based message filtering
- Support SAFE_ZONE management
- Optimize for battery efficiency

## Testing Interoperability

### 1. Test with 4people App

The 4people app serves as a reference implementation:

```bash
# Download the app
# Visit: https://github.com/felix-dieterle/4people/releases

# Install on test device
adb install 4people-release.apk
```

### 2. Verification Checklist

- [ ] Your app can discover 4people devices (SEPS-4people-*)
- [ ] Your app can send messages to 4people devices
- [ ] Your app can receive and forward 4people messages
- [ ] 4people can discover your app's devices (SEPS-yourapp-*)
- [ ] 4people can receive your app's messages
- [ ] Messages are forwarded through mixed networks (your app + 4people)

### 3. Test Scenarios

#### Scenario 1: Direct Communication
```
[Your App Device A] <---> [4people Device B]
```
- Device A sends EMERGENCY_ALERT
- Device B receives and displays alert

#### Scenario 2: Multi-Hop Routing
```
[Your App Device A] <---> [4people Device B] <---> [Your App Device C]
```
- Device A sends message to Device C
- Device B forwards the message
- Device C receives the message

#### Scenario 3: Mixed Network
```
[Your App] <---> [4people] <---> [Another SEPS App] <---> [Your App]
```
- All devices can exchange messages
- Network operates as unified mesh

## Platform-Specific Considerations

### Android

- **Bluetooth**: Use BluetoothAdapter for device discovery and RFCOMM for communication
- **WiFi**: Use WifiP2pManager for WiFi Direct, WifiManager for scanning
- **Permissions**: Request BLUETOOTH, LOCATION, WIFI permissions
- **Background**: Use foreground service for continuous operation

### iOS

- **Bluetooth**: Use CoreBluetooth framework
- **WiFi**: Limited WiFi Direct support, use Multipeer Connectivity framework
- **Permissions**: Request Bluetooth and Location permissions
- **Background**: Enable Background Modes for Bluetooth

### Web/Desktop

- **WebRTC**: Use for peer-to-peer communication
- **WebBluetooth**: Limited browser support
- **Limitations**: Cannot create WiFi hotspots or access raw Bluetooth

## Common Pitfalls

### 1. Incorrect Device Naming

❌ **Wrong**:
```kotlin
val name = "MyApp-${deviceId}" // Won't be discovered
```

✅ **Correct**:
```kotlin
val name = "SEPS-com.myapp-${deviceId}" // SEPS-compliant
```

### 2. Not Forwarding Messages

❌ **Wrong**:
```kotlin
// Only process messages for this device
if (destination == myDeviceId) {
    processMessage(message)
}
```

✅ **Correct**:
```kotlin
// Process and forward
if (destination == myDeviceId || destination == "BROADCAST") {
    processMessage(message)
}
if (ttl > 0 && destination != myDeviceId) {
    forwardMessage(message)
}
```

### 3. Ignoring Unknown Fields

❌ **Wrong**:
```kotlin
// Reject messages with unknown fields
if (message.has("unknown_field")) {
    throw Exception("Unknown field")
}
```

✅ **Correct**:
```kotlin
// Ignore unknown fields for forward compatibility
val knownField = message.optString("known_field", "default")
// Unknown fields are automatically ignored
```

### 4. Hardcoded Protocol Version

❌ **Wrong**:
```kotlin
if (message.getString("seps_version") != "1.0") {
    return // Reject
}
```

✅ **Correct**:
```kotlin
if (!message.getString("seps_version").startsWith("1.")) {
    return // Accept any 1.x version
}
```

## Sample Implementation

See the 4people reference implementation:
- **Protocol Messages**: [SepsMessage.kt](app/src/main/java/com/fourpeople/adhoc/protocol/SepsMessage.kt)
- **Codec**: [SepsCodec.kt](app/src/main/java/com/fourpeople/adhoc/protocol/SepsCodec.kt)
- **Protocol Handler**: [SepsProtocolHandler.kt](app/src/main/java/com/fourpeople/adhoc/protocol/SepsProtocolHandler.kt)
- **Tests**: [app/src/test/java/com/fourpeople/adhoc/protocol/](app/src/test/java/com/fourpeople/adhoc/protocol/)

## Contributing to SEPS

The SEPS specification is open for community input:

1. **Report Issues**: Found a problem? Open an issue on GitHub
2. **Suggest Improvements**: Have ideas? Submit a pull request
3. **Share Implementations**: Built a SEPS app? Add it to our list
4. **Test Interoperability**: Help test with different apps and devices

## License

SEPS specification: CC0 1.0 Universal (Public Domain)
4people implementation: MIT License

Anyone can implement SEPS without licensing restrictions.

## Support

- **GitHub Issues**: https://github.com/felix-dieterle/4people/issues
- **Specification**: [EMERGENCY_PROTOCOL_SPEC.md](EMERGENCY_PROTOCOL_SPEC.md)
- **Reference Implementation**: https://github.com/felix-dieterle/4people

## Summary

By implementing SEPS, your emergency app can:
- ✅ Communicate with 4people and other SEPS apps
- ✅ Extend network coverage through multi-hop routing
- ✅ Share emergency alerts and help requests
- ✅ Create unified emergency response networks
- ✅ Improve resilience through redundancy

Start with Level 1 compliance (basic message exchange) and gradually add more features for enhanced interoperability.
