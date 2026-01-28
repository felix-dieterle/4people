# Standard Emergency Protocol Specification (SEPS)

## Version 1.0

## Overview

The Standard Emergency Protocol Specification (SEPS) defines a common, interoperable protocol for emergency communication apps to exchange messages during infrastructure failures. This enables different emergency apps to work together, improving overall coverage and resilience.

## Goals

1. **Interoperability**: Enable different emergency apps to communicate seamlessly
2. **Simplicity**: Easy to implement across different platforms
3. **Extensibility**: Support future enhancements without breaking compatibility
4. **Efficiency**: Minimal overhead for resource-constrained environments
5. **Security**: Support for cryptographic verification and trust assessment

## Protocol Design Principles

### Inspired by Existing Standards

SEPS draws inspiration from:
- **CAP (Common Alerting Protocol)**: Standardized alert message format
- **EDXL (Emergency Data Exchange Language)**: Emergency data exchange standards
- **AODV (Ad-hoc On-Demand Distance Vector)**: Mesh routing protocol
- **IEEE 802.11s**: Wireless mesh networking standards

### Key Differences

Unlike CAP/EDXL which assume internet connectivity, SEPS is designed for:
- Completely offline, ad-hoc environments
- Peer-to-peer mesh networking
- Multiple communication channels (Bluetooth, WiFi, WiFi Direct, NFC, ultrasound)
- Constrained devices with limited battery and processing power

## Protocol Layers

### Layer 1: Transport Layer
Handles physical communication over various channels:
- Bluetooth Classic (RFCOMM)
- Bluetooth Low Energy (BLE)
- WiFi Direct
- WiFi (hotspot/client)
- NFC (for credential exchange)
- Ultrasound (for beacon signaling)

### Layer 2: Routing Layer
Manages multi-hop message forwarding:
- AODV-like on-demand route discovery
- Route caching and maintenance
- TTL-based loop prevention
- Duplicate message detection

### Layer 3: Protocol Layer
Defines message structure and semantics (THIS SPECIFICATION)

## Message Format

### JSON-based Wire Format

All SEPS messages use JSON for maximum compatibility and human readability.

```json
{
  "seps_version": "1.0",
  "message_id": "uuid-v4",
  "timestamp": 1706432123456,
  "sender": {
    "app_id": "com.example.emergencyapp",
    "device_id": "unique-device-identifier",
    "app_version": "1.0.0"
  },
  "message_type": "EMERGENCY_ALERT",
  "routing": {
    "ttl": 10,
    "hop_count": 0,
    "destination": "BROADCAST",
    "sequence": 42
  },
  "payload": {
    // Message-type-specific data
  },
  "signature": "optional-cryptographic-signature"
}
```

### Field Descriptions

#### Core Fields

- **seps_version** (string, required): Protocol version (e.g., "1.0")
- **message_id** (string, required): Unique identifier (UUID v4 recommended)
- **timestamp** (number, required): Unix timestamp in milliseconds
- **sender** (object, required): Sender identification
  - **app_id** (string, required): Reverse domain notation app identifier
  - **device_id** (string, required): Unique device identifier
  - **app_version** (string, required): Application version
- **message_type** (string, required): See Message Types section
- **routing** (object, required): Routing metadata
  - **ttl** (number, required): Time-to-live (hop count)
  - **hop_count** (number, required): Number of hops traversed
  - **destination** (string, required): Destination device ID or "BROADCAST"
  - **sequence** (number, required): Sequence number for route freshness
- **payload** (object, required): Message-type-specific data
- **signature** (string, optional): Cryptographic signature for verification

## Message Types

### 1. EMERGENCY_ALERT

Broadcast emergency status activation.

**Payload:**
```json
{
  "severity": "EXTREME",
  "category": "INFRASTRUCTURE_FAILURE",
  "description": "Power outage in downtown area",
  "location": {
    "latitude": 52.5200,
    "longitude": 13.4050,
    "accuracy": 10.0
  },
  "contacts": 5
}
```

**Fields:**
- **severity** (string): EXTREME, SEVERE, MODERATE, MINOR, UNKNOWN
- **category** (string): INFRASTRUCTURE_FAILURE, NATURAL_DISASTER, SECURITY_THREAT, OTHER
- **description** (string): Human-readable description
- **location** (object, optional): GPS coordinates
- **contacts** (number): Number of people in immediate network

### 2. HELP_REQUEST

Emergency help request with location.

**Payload:**
```json
{
  "urgency": "IMMEDIATE",
  "help_type": "MEDICAL",
  "description": "Person injured, needs medical assistance",
  "location": {
    "latitude": 52.5200,
    "longitude": 13.4050,
    "accuracy": 10.0,
    "altitude": 45.0
  },
  "requester_status": "INJURED",
  "contact_info": "encrypted-contact-data"
}
```

**Fields:**
- **urgency** (string): IMMEDIATE, URGENT, MODERATE, LOW
- **help_type** (string): MEDICAL, RESCUE, SHELTER, SUPPLIES, INFORMATION, OTHER
- **description** (string): Details about the situation
- **location** (object, required): GPS coordinates
- **requester_status** (string): INJURED, TRAPPED, STRANDED, SAFE_BUT_NEED_HELP
- **contact_info** (string, optional): Encrypted contact information

### 3. LOCATION_UPDATE

Broadcast location for participant tracking.

**Payload:**
```json
{
  "location": {
    "latitude": 52.5200,
    "longitude": 13.4050,
    "accuracy": 10.0,
    "altitude": 45.0,
    "speed": 1.5,
    "bearing": 180.0
  },
  "status": "SAFE",
  "battery_level": 45,
  "network_size": 12
}
```

**Fields:**
- **location** (object, required): GPS coordinates
- **status** (string): SAFE, NEED_HELP, INJURED, IN_DANGER, UNKNOWN
- **battery_level** (number): Battery percentage (0-100)
- **network_size** (number): Number of devices in immediate mesh

### 4. SAFE_ZONE

Define or update a safe collection point.

**Payload:**
```json
{
  "zone_id": "uuid-v4",
  "zone_type": "SHELTER",
  "location": {
    "latitude": 52.5200,
    "longitude": 13.4050,
    "accuracy": 10.0
  },
  "name": "City Hall Emergency Shelter",
  "capacity": 200,
  "occupied": 45,
  "amenities": ["WATER", "MEDICAL", "POWER"],
  "description": "Safe shelter with medical facilities"
}
```

**Fields:**
- **zone_id** (string, required): Unique zone identifier
- **zone_type** (string): SHELTER, MEDICAL, SUPPLY, EVACUATION, MEETING_POINT
- **location** (object, required): GPS coordinates
- **name** (string): Human-readable name
- **capacity** (number, optional): Maximum capacity
- **occupied** (number, optional): Current occupancy
- **amenities** (array, optional): Available resources
- **description** (string): Additional information

### 5. ROUTE_REQUEST (RREQ)

Request route discovery to destination.

**Payload:**
```json
{
  "destination_id": "target-device-id",
  "originator_sequence": 42,
  "destination_sequence": 40,
  "hop_count": 0
}
```

### 6. ROUTE_REPLY (RREP)

Reply with discovered route.

**Payload:**
```json
{
  "destination_id": "target-device-id",
  "destination_sequence": 42,
  "hop_count": 3,
  "lifetime": 30000
}
```

### 7. HELLO

Periodic neighbor discovery beacon.

**Payload:**
```json
{
  "app_capabilities": ["LOCATION_SHARING", "MESH_ROUTING", "SAFE_ZONES"],
  "supported_transports": ["BLUETOOTH", "WIFI_DIRECT"],
  "battery_level": 75,
  "active_participants": 8
}
```

### 8. TEXT_MESSAGE

Short text communication between users.

**Payload:**
```json
{
  "text": "Message content (max 256 chars)",
  "priority": "NORMAL",
  "requires_ack": true
}
```

**Fields:**
- **text** (string): Message content (max 256 characters)
- **priority** (string): HIGH, NORMAL, LOW
- **requires_ack** (boolean): Whether delivery confirmation is needed

## Protocol Negotiation

### Capability Advertisement

Apps advertise their capabilities in HELLO messages:

```json
{
  "app_capabilities": [
    "SEPS_V1",
    "LOCATION_SHARING",
    "MESH_ROUTING",
    "SAFE_ZONES",
    "ENCRYPTION"
  ],
  "supported_transports": [
    "BLUETOOTH",
    "WIFI_DIRECT",
    "WIFI"
  ]
}
```

### Version Compatibility

- Apps MUST support the version they advertise
- Apps MAY support multiple versions
- When communicating with an app using a different version:
  - Use the lowest common version
  - Ignore unknown fields (forward compatibility)
  - Provide sensible defaults for missing fields (backward compatibility)

## Security Considerations

### Message Authentication

Optional cryptographic signatures ensure message authenticity:

1. **Signature Generation**:
   - Compute JSON string of all fields except "signature"
   - Sign with private key (Ed25519 recommended)
   - Include Base64-encoded signature in "signature" field

2. **Signature Verification**:
   - Extract signature field
   - Recompute JSON string of remaining fields
   - Verify signature against sender's public key

### Trust Levels

Messages can be evaluated based on:
- Cryptographic signature validity
- Sender trust level (previously known device)
- Hop count (more hops = less trustworthy)
- Peer verifications (other devices confirm message)
- Connection security (encrypted vs. unencrypted hops)

### Privacy Considerations

- Device IDs SHOULD be randomly generated and rotated periodically
- Personal information SHOULD be encrypted
- Location data SHOULD only be shared when necessary
- Apps SHOULD allow users to control what information is shared

## Implementation Guidelines

### For App Developers

1. **Minimum Viable Implementation**:
   - Support EMERGENCY_ALERT and HELLO messages
   - Implement basic routing (RREQ/RREP)
   - At least one transport (Bluetooth recommended)

2. **Recommended Implementation**:
   - All message types
   - Multiple transports
   - Signature verification
   - Trust-based filtering

3. **Testing**:
   - Test interoperability with reference implementation (4people app)
   - Test in various network topologies
   - Test with different protocol versions

### Reference Implementation

The 4people Android app provides a reference implementation:
- Repository: https://github.com/felix-dieterle/4people
- Supports all SEPS v1.0 features
- Open source (MIT License)
- Available for testing and validation

### Library Support

Future work may include:
- SEPS codec libraries for various platforms
- Validation tools
- Test suites for compliance checking
- Protocol analyzers for debugging

## Network Discovery

### Emergency Network Identification

Apps implementing SEPS SHOULD be discoverable through:

1. **Bluetooth Device Name Pattern**: `SEPS-<app-id>-<device-id>`
   - Example: `SEPS-4people-abc123`

2. **WiFi SSID Pattern**: `SEPS-<app-id>-<device-id>`
   - Example: `SEPS-4people-abc123`

3. **WiFi Direct Service**: `_seps._tcp`
   - Service info includes app_id and protocol version

4. **NFC NDEF Record**: Application/SEPS
   - Contains capability advertisement

### Legacy Compatibility

Apps MAY continue using their existing naming patterns and SHOULD:
- Monitor for both SEPS and legacy patterns
- Advertise both SEPS and legacy identifiers during transition period

## QoS and Performance

### Message Priorities

1. **CRITICAL**: HELP_REQUEST
2. **HIGH**: EMERGENCY_ALERT, ROUTE_ERROR
3. **NORMAL**: LOCATION_UPDATE, SAFE_ZONE, TEXT_MESSAGE
4. **LOW**: HELLO

### Battery Optimization

- HELLO messages: Every 30 seconds in standby, every 10 seconds when active
- Location updates: Every 30-60 seconds
- Route maintenance: Expire stale routes after 30 seconds
- Adaptive scanning based on battery level

### Bandwidth Considerations

- Keep messages compact (typical size: 500-2000 bytes)
- Use broadcast sparingly
- Implement message deduplication
- Consider message aggregation for efficiency

## Extensibility

### Adding New Message Types

Apps MAY define custom message types:
- Use reverse domain notation: `com.example.CUSTOM_TYPE`
- Include in capability advertisement
- Other apps will forward but may not process

### Custom Fields

Apps MAY add custom fields to payloads:
- Prefix with app identifier: `com_example_custom_field`
- Other apps MUST ignore unknown fields

### Future Protocol Versions

When introducing breaking changes:
- Increment major version (1.0 → 2.0)
- Maintain backward compatibility when possible
- Support multiple versions during transition

## Appendix A: Example Messages

### Example 1: Emergency Activation

```json
{
  "seps_version": "1.0",
  "message_id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1706432123456,
  "sender": {
    "app_id": "com.fourpeople.adhoc",
    "device_id": "4people-abc123",
    "app_version": "1.0.34"
  },
  "message_type": "EMERGENCY_ALERT",
  "routing": {
    "ttl": 10,
    "hop_count": 0,
    "destination": "BROADCAST",
    "sequence": 1
  },
  "payload": {
    "severity": "EXTREME",
    "category": "INFRASTRUCTURE_FAILURE",
    "description": "Power outage in city center",
    "location": {
      "latitude": 52.5200,
      "longitude": 13.4050,
      "accuracy": 10.0
    },
    "contacts": 3
  }
}
```

### Example 2: Help Request

```json
{
  "seps_version": "1.0",
  "message_id": "661f9511-f30c-52e5-b827-557766551111",
  "timestamp": 1706432234567,
  "sender": {
    "app_id": "com.fourpeople.adhoc",
    "device_id": "4people-xyz789",
    "app_version": "1.0.34"
  },
  "message_type": "HELP_REQUEST",
  "routing": {
    "ttl": 10,
    "hop_count": 0,
    "destination": "BROADCAST",
    "sequence": 5
  },
  "payload": {
    "urgency": "IMMEDIATE",
    "help_type": "MEDICAL",
    "description": "Person with chest pain, difficulty breathing",
    "location": {
      "latitude": 52.5210,
      "longitude": 13.4060,
      "accuracy": 5.0
    },
    "requester_status": "SAFE_BUT_NEED_HELP"
  }
}
```

## Appendix B: Compliance Checklist

For an app to be SEPS-compliant:

### Required (Level 1):
- ✅ Support SEPS v1.0 JSON format
- ✅ Implement EMERGENCY_ALERT message type
- ✅ Implement HELLO message type
- ✅ Implement basic routing (forward messages with TTL)
- ✅ Support at least Bluetooth transport
- ✅ Use SEPS naming pattern for discovery

### Recommended (Level 2):
- ✅ Support HELP_REQUEST and LOCATION_UPDATE
- ✅ Implement RREQ/RREP routing protocol
- ✅ Support multiple transports
- ✅ Implement message deduplication
- ✅ Support protocol version negotiation

### Advanced (Level 3):
- ✅ Support all message types
- ✅ Implement cryptographic signatures
- ✅ Implement trust-based filtering
- ✅ Support SAFE_ZONE management
- ✅ Optimize for battery efficiency
- ✅ Support backward compatibility with legacy formats

## Appendix C: Change Log

### Version 1.0 (2026-01-28)
- Initial specification release
- Core message types defined
- JSON-based wire format
- AODV-inspired routing
- Security and privacy considerations

## License

This specification is released under CC0 1.0 Universal (Public Domain).
Anyone can implement SEPS without licensing restrictions.
