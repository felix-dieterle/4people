# Sichere Verbindungen im Mesh-Routing / Secure Connections in Mesh Routing

## Übersicht / Overview

Das Mesh-Routing-System unterstützt nun die Verfolgung der Verbindungssicherheit über Nachrichten-Pfade hinweg.

The mesh routing system now supports tracking connection security across message paths.

---

## Implementierung / Implementation

### RouteEntry.ConnectionSecurity

Jede Route kann nun eine von drei Sicherheitsstufen haben:

Each route can now have one of three security levels:

```kotlin
enum class ConnectionSecurity {
    SECURE,    // Verschlüsselte/authentifizierte Verbindung
    INSECURE,  // Unverschlüsselte Verbindung
    UNKNOWN    // Sicherheitsstatus nicht bestimmt
}
```

**Beispiele / Examples:**
- `SECURE`: Bluetooth mit Pairing, WPA2/WPA3 WiFi
- `INSECURE`: Offenes WiFi, ungepaartes Bluetooth
- `UNKNOWN`: Sicherheit nicht ermittelt oder gemischte Sicherheit im Pfad

### MeshMessage.hasInsecureHop

Nachrichten verfolgen nun, ob sie über mindestens eine unsichere Verbindung geleitet wurden:

Messages now track whether they've been routed through at least one insecure connection:

```kotlin
data class MeshMessage(
    ...
    val hasInsecureHop: Boolean = false
)
```

Beim Weiterleiten einer Nachricht:

When forwarding a message:

```kotlin
// Sichere Weiterleitung / Secure forwarding
message.forward(isSecureHop = true)

// Unsichere Weiterleitung / Insecure forwarding
message.forward(isSecureHop = false)
```

---

## Trust Score Integration

Der Trust Score berücksichtigt nun die Verbindungssicherheit:

The trust score now considers connection security:

### Neue Formel / New Formula

```
Trust Score = (Sender Trust × 0.5) + (Hop Score × 0.3) + 
              (Connection Security × 0.1) + (Verification × 0.15)

Wobei / Where:
- Sichere Verbindung: +0.1 (10% Bonus)
- Unsichere Verbindung: -0.1 (10% Strafe)
```

### Beispielszenarien / Example Scenarios

#### Szenario 1: Sichere Direktverbindung
**Scenario 1: Secure Direct Connection**

- Absender / Sender: Familie (Stufe 3) / Family (Level 3)
- Hops: 0
- Sicherheit / Security: Vollständig sicher / Fully secure
- Verifikationen / Verifications: Keine / None

**Berechnung / Calculation:**
```
Sender Trust: 1.0 × 0.5 = 0.50
Hop Score: 1.0 × 0.3 = 0.30
Security Bonus: +0.1
Base Score: 0.90
Final Score: 0.90 ✅ (Sehr hoch / Very High)
```

#### Szenario 2: Unsichere Verbindung im Pfad
**Scenario 2: Insecure Connection in Path**

- Absender / Sender: Freund (Stufe 2) / Friend (Level 2)
- Hops: 2
- Sicherheit / Security: Mindestens 1 unsicherer Hop / At least 1 insecure hop
- Verifikationen / Verifications: Keine / None

**Berechnung / Calculation:**
```
Sender Trust: 0.67 × 0.5 = 0.335
Hop Penalty: 2 × 0.1 = 0.2
Hop Score: (1.0 - 0.2) × 0.3 = 0.24
Security Penalty: -0.1
Base Score: 0.475
Final Score: 0.48 ⚠️ (Mittel / Medium)
```

**Vergleich ohne Sicherheitscheck / Comparison without security check:**
- Alter Score / Old score: 0.72 (Hoch / High)
- Neuer Score / New score: 0.48 (Mittel / Medium)
- Differenz / Difference: -0.24 (33% Reduktion / reduction)

#### Szenario 3: Sicherer Pfad vs. Unsicherer Pfad
**Scenario 3: Secure Path vs. Insecure Path**

**Sicherer Pfad / Secure Path:**
- Absender / Sender: Bekannter (Stufe 1) / Known (Level 1)
- Hops: 1
- Sicherheit / Security: Vollständig sicher / Fully secure
- **Score: 0.57 👍**

**Unsicherer Pfad / Insecure Path:**
- Absender / Sender: Bekannter (Stufe 1) / Known (Level 1)
- Hops: 1
- Sicherheit / Security: Unsichere Verbindung / Insecure connection
- **Score: 0.37 ⚡**

**Unterschied / Difference:** 0.20 Punkte (35% niedriger / lower)

---

## Verwendung / Usage

### Route mit Sicherheitsinformation erstellen
### Create Route with Security Information

```kotlin
val secureRoute = RouteEntry(
    destinationId = "target_device",
    nextHopId = "neighbor_device",
    hopCount = 1,
    sequenceNumber = 42,
    connectionSecurity = RouteEntry.ConnectionSecurity.SECURE
)

val insecureRoute = RouteEntry(
    destinationId = "target_device",
    nextHopId = "neighbor_device",
    hopCount = 1,
    sequenceNumber = 42,
    connectionSecurity = RouteEntry.ConnectionSecurity.INSECURE
)
```

### Nachricht mit Sicherheitsverfolgung weiterleiten
### Forward Message with Security Tracking

```kotlin
// Empfangene Nachricht / Received message
val receivedMessage: MeshMessage = ...

// Route abrufen / Get route
val route = routeTable.getRoute(receivedMessage.destinationId)

// Sicherheit der aktuellen Verbindung bestimmen
// Determine security of current connection
val isSecureConnection = route?.isSecure() ?: false

// Nachricht weiterleiten / Forward message
val forwardedMessage = receivedMessage.forward(isSecureHop = isSecureConnection)
```

### Trust Score mit Sicherheit berechnen
### Calculate Trust Score with Security

```kotlin
val calculator = MessageTrustCalculator(trustManager)

val evaluation = calculator.evaluateMessage(
    messageId = message.messageId,
    originalSenderId = message.sourceId,
    hopCount = message.hopCount,
    verifications = verifications,
    hasInsecureHop = message.hasInsecureHop  // Neue Parameter / New parameter
)

// Sicherheitsstatus prüfen / Check security status
if (message.hasInsecureHop) {
    println("⚠️ Warnung: Nachricht über unsichere Verbindung")
    println("⚠️ Warning: Message via insecure connection")
}
```

---

## Route-Priorisierung / Route Prioritization

Routen mit gleicher Sequenznummer und Hop-Anzahl werden nun nach Sicherheit priorisiert:

Routes with the same sequence number and hop count are now prioritized by security:

```kotlin
// Route A: 3 Hops, SECURE
// Route B: 3 Hops, INSECURE
// → Route A wird bevorzugt / Route A is preferred

route.isBetterThan(otherRoute)
```

**Priorität / Priority:**
1. Höhere Sequenznummer (frischer) / Higher sequence number (fresher)
2. Weniger Hops (kürzer) / Fewer hops (shorter)
3. Sichere Verbindung (neu!) / Secure connection (new!)

---

## Zukünftige Erweiterungen / Future Enhancements

### 1. Granulare Sicherheitsstufen
**Granular Security Levels**

Statt nur SECURE/INSECURE/UNKNOWN:

Instead of just SECURE/INSECURE/UNKNOWN:

```kotlin
enum class ConnectionSecurity {
    NONE,           // Keine Verschlüsselung / No encryption
    WEP,            // Schwache Verschlüsselung / Weak encryption
    WPA2_PSK,       // Standard WiFi-Sicherheit / Standard WiFi security
    WPA3,           // Moderne WiFi-Sicherheit / Modern WiFi security
    BLUETOOTH_LE,   // Bluetooth Low Energy pairing
    END_TO_END      // Ende-zu-Ende verschlüsselt / End-to-end encrypted
}
```

### 2. Per-Hop-Sicherheitsverfolgung
**Per-Hop Security Tracking**

Verfolge Sicherheit für jeden einzelnen Hop:

Track security for each individual hop:

```kotlin
data class MeshMessage(
    ...
    val securityPath: List<ConnectionSecurity> = emptyList()
)

// Ermöglicht granulare Analyse wie:
// Allows granular analysis like:
// "3 sichere Hops, dann 1 unsicherer"
// "3 secure hops, then 1 insecure"
```

### 3. Zertifikatsvalidierung
**Certificate Validation**

Für Ende-zu-Ende-Verschlüsselung:

For end-to-end encryption:

```kotlin
data class RouteEntry(
    ...
    val certificateFingerprint: String? = null,
    val isCertificateValid: Boolean = false
)
```

### 4. Dynamische Sicherheitsbewertung
**Dynamic Security Assessment**

Automatische Erkennung basierend auf Transportschicht:

Automatic detection based on transport layer:

```kotlin
fun detectConnectionSecurity(transport: TransportType): ConnectionSecurity {
    return when (transport) {
        TransportType.BLUETOOTH_PAIRED -> ConnectionSecurity.SECURE
        TransportType.BLUETOOTH_UNPAIRED -> ConnectionSecurity.INSECURE
        TransportType.WIFI_WPA3 -> ConnectionSecurity.SECURE
        TransportType.WIFI_OPEN -> ConnectionSecurity.INSECURE
        else -> ConnectionSecurity.UNKNOWN
    }
}
```

### 5. Sicherheitsrichtlinien
**Security Policies**

Benutzer-definierte Mindestanforderungen:

User-defined minimum requirements:

```kotlin
class SecurityPolicy {
    val minimumTrustScore: Double = 0.5
    val requireSecureConnections: Boolean = false
    val allowInsecureForEmergency: Boolean = true
    
    fun shouldAcceptMessage(message: MeshMessage, trustScore: Double): Boolean {
        if (requireSecureConnections && message.hasInsecureHop) {
            return allowInsecureForEmergency && 
                   message.messageType == MessageType.HELP_REQUEST
        }
        return trustScore >= minimumTrustScore
    }
}
```

### 6. Metriken und Monitoring
**Metrics and Monitoring**

Statistiken über Verbindungssicherheit:

Statistics about connection security:

```kotlin
data class SecurityMetrics(
    val totalMessages: Int,
    val secureMessages: Int,
    val insecureMessages: Int,
    val averageTrustScore: Double,
    val secureRoutes: Int,
    val insecureRoutes: Int
)
```

---

## Migration und Kompatibilität / Migration and Compatibility

### Abwärtskompatibilität / Backward Compatibility

Alte Nachrichten ohne `hasInsecureHop`-Feld werden als `false` (sicher) behandelt:

Old messages without `hasInsecureHop` field are treated as `false` (secure):

```kotlin
// Standard-Wert / Default value
val hasInsecureHop: Boolean = false
```

Alte `RouteEntry` ohne `connectionSecurity` verwenden `UNKNOWN`:

Old `RouteEntry` without `connectionSecurity` use `UNKNOWN`:

```kotlin
// Standard-Wert / Default value
val connectionSecurity: ConnectionSecurity = ConnectionSecurity.UNKNOWN
```

### Schrittweise Einführung / Gradual Rollout

1. **Phase 1** (Aktuell / Current): 
   - Grundlegende Unterstützung / Basic support
   - SECURE/INSECURE/UNKNOWN
   - Manuelle Konfiguration / Manual configuration

2. **Phase 2** (Zukünftig / Future):
   - Automatische Erkennung / Automatic detection
   - Granulare Sicherheitsstufen / Granular security levels
   - Per-Hop-Tracking

3. **Phase 3** (Erweitert / Advanced):
   - Ende-zu-Ende-Verschlüsselung / End-to-end encryption
   - Zertifikatsvalidierung / Certificate validation
   - Sicherheitsrichtlinien / Security policies

---

## Testen / Testing

### Unit Tests

```kotlin
@Test
fun testSecureConnectionBonus() {
    val secureScore = calculator.calculateTrustScore(
        originalSenderId = "friend",
        hopCount = 1,
        hasInsecureHop = false
    )
    
    val insecureScore = calculator.calculateTrustScore(
        originalSenderId = "friend",
        hopCount = 1,
        hasInsecureHop = true
    )
    
    // Sicherer Score sollte höher sein
    // Secure score should be higher
    assertTrue(secureScore > insecureScore)
    assertEquals(0.2, secureScore - insecureScore, 0.01)
}
```

### Integration Tests

```kotlin
@Test
fun testMessageSecurityTracking() {
    val message = MeshMessage(
        sourceId = "alice",
        destinationId = "bob",
        payload = "test",
        hasInsecureHop = false
    )
    
    // Sichere Weiterleitung / Secure forwarding
    val forwarded1 = message.forward(isSecureHop = true)
    assertFalse(forwarded1.hasInsecureHop)
    
    // Unsichere Weiterleitung / Insecure forwarding
    val forwarded2 = forwarded1.forward(isSecureHop = false)
    assertTrue(forwarded2.hasInsecureHop)
    
    // Bleibt unsicher / Remains insecure
    val forwarded3 = forwarded2.forward(isSecureHop = true)
    assertTrue(forwarded3.hasInsecureHop)
}
```

---

## Zusammenfassung / Summary

Diese rudimentäre Implementierung bietet:

This rudimentary implementation provides:

✅ **Verbindungssicherheits-Tracking** / Connection security tracking
✅ **Trust Score Integration** / Trust score integration  
✅ **Route-Priorisierung** / Route prioritization
✅ **Abwärtskompatibilität** / Backward compatibility
✅ **Erweiterbare Architektur** / Extensible architecture

**Nächste Schritte / Next Steps:**
- Automatische Sicherheitserkennung / Automatic security detection
- Ende-zu-Ende-Verschlüsselung / End-to-end encryption
- Benutzer-UI für Sicherheitsinformationen / User UI for security information
