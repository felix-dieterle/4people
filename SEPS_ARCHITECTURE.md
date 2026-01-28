# SEPS Protocol Architecture

## Overview Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                  Emergency Network Ecosystem                     │
│                                                                   │
│  ┌──────────┐        ┌──────────┐        ┌──────────┐          │
│  │ 4people  │◄─SEPS─►│  App B   │◄─SEPS─►│  App C   │          │
│  │  v1.0    │        │  v1.0    │        │  v1.0    │          │
│  └──────────┘        └──────────┘        └──────────┘          │
│       ▲                   ▲                   ▲                  │
│       │                   │                   │                  │
│       └───────────────────┴───────────────────┘                  │
│              Unified Emergency Network                           │
└─────────────────────────────────────────────────────────────────┘
```

## SEPS Message Flow

```
┌────────────┐                                      ┌────────────┐
│  Device A  │                                      │  Device C  │
│ (4people)  │                                      │ (Other App)│
└─────┬──────┘                                      └──────▲─────┘
      │                                                     │
      │ 1. EMERGENCY_ALERT                                 │
      │    (SEPS JSON)                                     │
      │                                                     │
      │         ┌────────────┐                             │
      └────────►│  Device B  │─────────────────────────────┘
                │ (4people)  │  2. Forward Message
                └────────────┘     (SEPS JSON)

Timeline:
  t=0s:  Device A detects emergency, activates
  t=1s:  Device A broadcasts EMERGENCY_ALERT via SEPS
  t=2s:  Device B receives, recognizes SEPS message
  t=3s:  Device B forwards to Device C (different app!)
  t=4s:  Device C receives and processes SEPS message
  
Result: Cross-app emergency communication achieved!
```

## Protocol Stack

```
┌─────────────────────────────────────────────────┐
│           Application Layer                      │
│   ┌─────────────────────────────────────────┐   │
│   │  Emergency Alert, Help Request, etc.    │   │
│   └─────────────────────────────────────────┘   │
├─────────────────────────────────────────────────┤
│           SEPS Protocol Layer (This!)           │
│   ┌─────────────────────────────────────────┐   │
│   │  SepsMessage                            │   │
│   │  - JSON encoding/decoding               │   │
│   │  - Message type handling                │   │
│   │  - Version negotiation                  │   │
│   │  - Payload validation                   │   │
│   └─────────────────────────────────────────┘   │
├─────────────────────────────────────────────────┤
│           Routing Layer                         │
│   ┌─────────────────────────────────────────┐   │
│   │  MeshRoutingManager                     │   │
│   │  - Route discovery (RREQ/RREP)          │   │
│   │  - Multi-hop forwarding                 │   │
│   │  - Duplicate detection                  │   │
│   └─────────────────────────────────────────┘   │
├─────────────────────────────────────────────────┤
│           Transport Layer                       │
│   ┌──────────┬──────────┬──────────────────┐   │
│   │Bluetooth │ WiFi     │ WiFi Direct      │   │
│   │          │          │                  │   │
│   └──────────┴──────────┴──────────────────┘   │
└─────────────────────────────────────────────────┘
```

## Component Interaction

```
┌──────────────────────────────────────────────────────────┐
│                    AdHocCommunicationService              │
│                                                           │
│  ┌────────────────┐          ┌────────────────┐         │
│  │ Legacy Handler │          │ SEPS Handler   │         │
│  │ (4people-*)    │          │ (SEPS-*)       │         │
│  └───────┬────────┘          └────────┬───────┘         │
│          │                             │                  │
│          │  ┌──────────────────────┐  │                  │
│          └─►│ Message Dispatcher   │◄─┘                  │
│             │ (Route messages to   │                     │
│             │  appropriate handler)│                     │
│             └──────────┬───────────┘                     │
│                        │                                  │
│                        ▼                                  │
│             ┌──────────────────────┐                     │
│             │ MeshRoutingManager   │                     │
│             └──────────┬───────────┘                     │
│                        │                                  │
│                        ▼                                  │
│             ┌──────────────────────┐                     │
│             │ Transport Layer      │                     │
│             │ (BT/WiFi/WiFi Direct)│                     │
│             └──────────────────────┘                     │
└──────────────────────────────────────────────────────────┘
```

## Message Format Comparison

### Legacy 4people Format (Internal)
```kotlin
MeshMessage(
    messageId = "uuid",
    sourceId = "4people-abc",
    destinationId = "BROADCAST",
    payload = "Emergency!",
    messageType = DATA,
    ttl = 10
)
```

### SEPS Format (Interoperable)
```json
{
  "seps_version": "1.0",
  "message_id": "uuid",
  "sender": {
    "app_id": "com.fourpeople.adhoc",
    "device_id": "4people-abc",
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
    "description": "Emergency!"
  }
}
```

## Conversion Flow

```
┌────────────────┐                    ┌────────────────┐
│  MeshMessage   │                    │  SepsMessage   │
│   (Internal)   │                    │  (External)    │
└────────┬───────┘                    └────────▲───────┘
         │                                     │
         │                                     │
         │   meshMessageToSeps()               │
         └─────────────────────────────────────┘
                  SepsCodec
         ┌─────────────────────────────────────┐
         │   sepsToMeshMessage()               │
         ▼                                     │
┌────────┴───────┐                    ┌───────┴────────┐
│  MeshMessage   │                    │  SepsMessage   │
│   (Internal)   │                    │  (External)    │
└────────────────┘                    └────────────────┘
```

## Device Discovery

### Legacy Pattern
```
Bluetooth: "4people-abc123"
WiFi SSID: "4people-abc123"

Detected by: Only 4people apps
```

### SEPS Pattern (New)
```
Bluetooth: "SEPS-4people-abc123"
WiFi SSID: "SEPS-4people-abc123"

Detected by: Any SEPS-compliant app!
```

### Dual Support (Recommended)
```
Listen for:
  - "4people-*"      (Legacy compatibility)
  - "SEPS-4people-*" (New SEPS)
  - "SEPS-*"         (Other SEPS apps)

Advertise as:
  - "SEPS-4people-abc123" (Primary)
  - "4people-abc123"      (Backward compat)
```

## Multi-App Network Topology

```
                    Emergency Network
                    
┌─────────────────────────────────────────────────┐
│                                                  │
│    4people        4people       Other App       │
│       A ────────── B ────────── C               │
│       │                          │               │
│       │                          │               │
│    4people                    Other App         │
│       D                          E               │
│       │                          │               │
│       │                          │               │
│    Other App ─────────────── 4people            │
│       F              G           H               │
│                                                  │
└─────────────────────────────────────────────────┘

Connections using SEPS protocol:
A↔B: Both 4people, SEPS communication
B↔C: Different apps, SEPS enables communication
C↔E: Same third-party app, native + SEPS
D↔F: Different apps, SEPS enables communication
F↔G: SEPS multi-hop routing
G↔H: SEPS to 4people communication

Result: Unified network despite app diversity!
```

## Compliance Levels

```
Level 1: Basic
┌──────────────────────────────────┐
│ ✓ SEPS naming pattern            │
│ ✓ EMERGENCY_ALERT send/receive   │
│ ✓ HELLO messages                 │
│ ✓ Basic forwarding (TTL)         │
└──────────────────────────────────┘
         Minimum for interop

Level 2: Recommended
┌──────────────────────────────────┐
│ ✓ All Level 1 features           │
│ ✓ RREQ/RREP routing              │
│ ✓ HELP_REQUEST, LOCATION_UPDATE  │
│ ✓ Multiple transports            │
│ ✓ Message deduplication          │
└──────────────────────────────────┘
         Good interoperability

Level 3: Advanced (4people)
┌──────────────────────────────────┐
│ ✓ All Level 2 features           │
│ ✓ All message types              │
│ ✓ Cryptographic signatures       │
│ ✓ Trust-based filtering          │
│ ✓ SAFE_ZONE management           │
│ ✓ Battery optimization           │
└──────────────────────────────────┘
         Full feature parity
```

## Security Architecture

```
┌────────────────────────────────────────────┐
│         SEPS Message (Optional Signature)  │
└───────────────────┬────────────────────────┘
                    │
                    ▼
         ┌──────────────────────┐
         │  Signature Verification│
         │  (Ed25519)            │
         └──────────┬────────────┘
                    │
                    ▼
         ┌──────────────────────┐
         │  Trust Evaluation    │
         │  - Sender trust      │
         │  - Hop count         │
         │  - Peer confirmations│
         └──────────┬────────────┘
                    │
                    ▼
         ┌──────────────────────┐
         │  Message Processing  │
         │  (Accept/Reject)     │
         └──────────────────────┘
```

## Future Extensions

```
SEPS v1.0 (Current)
    │
    ├─► v1.1: Add encryption
    │
    ├─► v1.2: Add voice messages
    │
    └─► v2.0: Breaking changes
             (with v1.x compat)

Backward Compatibility:
- Apps support multiple versions
- Unknown fields ignored
- Graceful degradation
```

## Summary

The SEPS protocol implementation enables:

✅ **Cross-app communication** - Different emergency apps work together
✅ **Unified network** - Single mesh across multiple app types
✅ **Standard protocol** - JSON-based, easy to implement
✅ **Backward compatibility** - Works with legacy 4people devices
✅ **Future-proof** - Version negotiation built-in
✅ **Open standard** - Anyone can implement (CC0 license)

This fulfills the original request: Standard protocols that enable different emergency apps to collaborate and improve overall coverage during infrastructure failures.
