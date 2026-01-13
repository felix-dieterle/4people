# Trust-Based Message Evaluation System

## Übersicht / Overview

Das Trust-System bewertet die Vertrauenswürdigkeit von Nachrichten im Notfall-Kommunikationsnetz basierend auf:
1. **Vertrauensstufe des Absenders** (Trust level of the sender)
2. **Anzahl der Stationen (Hops)** vom ursprünglichen Absender (Number of hops from original sender)
3. **Verbindungssicherheit** über den Nachrichten-Pfad (Connection security across the message path)
4. **Bestätigungen und Ablehnungen** von anderen Kontakten (Confirmations and rejections from other contacts)

The trust system evaluates message trustworthiness in the emergency communication network based on:
1. **Sender's trust level** - How much you trust the original sender
2. **Hop count** - Number of intermediate devices the message passed through
3. **Connection security** - Whether the path contains insecure connections
4. **Verifications** - Confirmations or rejections from other contacts

> **Neu / New:** Siehe [SECURE_CONNECTIONS.md](SECURE_CONNECTIONS.md) für Details zur Verbindungssicherheit im Mesh-Routing.
> See [SECURE_CONNECTIONS.md](SECURE_CONNECTIONS.md) for details on connection security in mesh routing.

---

## Vertrauensstufen / Trust Levels

Das System verwendet vier Vertrauensstufen:

The system uses four trust levels:

| Stufe / Level | Bezeichnung / Name | Beschreibung / Description | Trust Factor |
|---------------|-------------------|----------------------------|--------------|
| 0 | Unbekannt / Unknown | Absender nicht in Kontakten / Sender not in contacts | 0.0 |
| 1 | Bekannter Kontakt / Known Contact | Automatisch aus Telefonbuch, E-Mail, Messenger / Automatically from phone, email, messenger contacts | 0.33 |
| 2 | Freund / Friend | Manuell eingestellt / Manually set | 0.67 |
| 3 | Eng/Familie / Close/Family | Manuell eingestellt, höchstes Vertrauen / Manually set, highest trust | 1.0 |

### Einstellen der Vertrauensstufen / Setting Trust Levels

- **Stufe 0**: Automatisch für unbekannte Absender
  - **Level 0**: Automatic for unknown senders

- **Stufe 1**: Automatisch beim Import von Kontakten
  - **Level 1**: Automatic when importing contacts

- **Stufe 2-3**: Manuell über die App einstellbar
  - **Levels 2-3**: Manually configurable through the app
  - Zukünftig: Vorschläge basierend auf Kontakthäufigkeit
  - Future: Suggestions based on contact frequency

---

## Trust Score Algorithmus / Trust Score Algorithm

### Formel / Formula

Der Trust Score wird wie folgt berechnet (aktualisiert mit Verbindungssicherheit):

The trust score is calculated as follows (updated with connection security):

```
Base Score = (Sender Trust Factor × 0.5) + (Hop Score × 0.3) + (Connection Security × 0.1)

Hop Score = max(0, 1 - (hop_count × 0.1))
  - Maximum penalty: 50% (5+ hops)

Connection Security Score:
  - Secure path (no insecure hops): +0.1 (10% bonus)
  - Insecure path (≥1 insecure hop): -0.1 (10% penalty)

Verification Adjustment = ±0.15 maximum
  - Based on weighted confirmations/rejections
  - Higher trust verifiers have more weight

Final Score = Base Score + Verification Adjustment
  - Clamped to range [0.0, 1.0]
```

### Gewichtung / Weighting

- **50%** Vertrauensstufe des Absenders / Sender's trust level
- **30%** Hop-Anzahl (Nähe zum Original) / Hop count (proximity to source)
- **10%** Verbindungssicherheit (neu!) / Connection security (new!)
- **±15%** Bestätigungen/Ablehnungen / Verifications

### Hop-Penalty

Jeder Hop reduziert die Vertrauenswürdigkeit um 10% (max. 50%):

Each hop reduces trustworthiness by 10% (max 50%):

- 0 Hops: Kein Abzug / No penalty
- 1 Hop: -10%
- 2 Hops: -20%
- 3 Hops: -30%
- 4 Hops: -40%
- 5+ Hops: -50% (Maximum)

---

## Beispielszenarien / Example Scenarios

### Szenario 1: Direkte Nachricht von engem Kontakt
### Scenario 1: Direct message from close contact

**Situation:**
- Absender / Sender: Familienmitglied (Stufe 3) / Family member (Level 3)
- Hops: 0 (direkte Verbindung) / 0 (direct connection)
- Verifikationen / Verifications: Keine / None

**Berechnung / Calculation:**
```
Sender Trust Factor = 1.0
Base Score from Sender = 1.0 × 0.6 = 0.60
Hop Score = 1.0 × 0.4 = 0.40
Base Score = 0.60 + 0.40 = 1.00
Verification Adjustment = 0.00
Final Score = 1.00
```

**Bewertung / Rating:** ✅ Sehr hoch / Very High (1.00)

**Bedeutung / Meaning:** Höchstmögliche Vertrauenswürdigkeit - direkte Nachricht von engster Person.
Maximum trustworthiness - direct message from closest person.

---

### Szenario 2: Nachricht von Freund über 2 Stationen
### Scenario 2: Message from friend via 2 hops

**Situation:**
- Absender / Sender: Freund (Stufe 2) / Friend (Level 2)
- Hops: 2
- Verifikationen / Verifications: Keine / None

**Berechnung / Calculation:**
```
Sender Trust Factor = 0.67
Base Score from Sender = 0.67 × 0.6 = 0.40
Hop Penalty = 2 × 0.1 = 0.2
Hop Score = (1.0 - 0.2) × 0.4 = 0.32
Base Score = 0.40 + 0.32 = 0.72
Verification Adjustment = 0.00
Final Score = 0.72
```

**Bewertung / Rating:** 👍 Hoch / High (0.72)

**Bedeutung / Meaning:** Gute Vertrauenswürdigkeit trotz 2 Hops, da Freund als Absender.
Good trustworthiness despite 2 hops, because friend is the sender.

---

### Szenario 3: Unbekannter Absender über 5 Stationen
### Scenario 3: Unknown sender via 5 hops

**Situation:**
- Absender / Sender: Unbekannt (Stufe 0) / Unknown (Level 0)
- Hops: 5
- Verifikationen / Verifications: Keine / None

**Berechnung / Calculation:**
```
Sender Trust Factor = 0.0
Base Score from Sender = 0.0 × 0.6 = 0.00
Hop Penalty = 5 × 0.1 = 0.5 (max penalty reached)
Hop Score = (1.0 - 0.5) × 0.4 = 0.20
Base Score = 0.00 + 0.20 = 0.20
Verification Adjustment = 0.00
Final Score = 0.20
```

**Bewertung / Rating:** ⚡ Niedrig / Low (0.20)

**Bedeutung / Meaning:** Geringe Vertrauenswürdigkeit - unbekannter Absender und viele Hops.
Low trustworthiness - unknown sender and many hops.

---

### Szenario 4: Bestätigung durch vertrauenswürdige Kontakte
### Scenario 4: Confirmation by trusted contacts

**Situation:**
- Absender / Sender: Bekannter Kontakt (Stufe 1) / Known contact (Level 1)
- Hops: 3
- Verifikationen / Verifications:
  - Bestätigt von / Confirmed by: Familienmitglied (Stufe 3) / Family (Level 3)
  - Bestätigt von / Confirmed by: Freund (Stufe 2) / Friend (Level 2)

**Berechnung ohne Verifikationen / Calculation without verifications:**
```
Sender Trust Factor = 0.33
Base Score from Sender = 0.33 × 0.6 = 0.20
Hop Penalty = 3 × 0.1 = 0.3
Hop Score = (1.0 - 0.3) × 0.4 = 0.28
Base Score = 0.20 + 0.28 = 0.48
```

**Verifikation Adjustment / Verification Adjustment:**
```
Verifier 1 (Level 3): Trust Factor = 1.0, Confirmed = +1.0
Verifier 2 (Level 2): Trust Factor = 0.67, Confirmed = +0.67
Total Adjustment = (1.0 + 0.67) / (1.0 + 0.67) = 1.0
Scaled Adjustment = 1.0 × 0.15 = +0.15
```

**Finale Berechnung / Final Calculation:**
```
Final Score = 0.48 + 0.15 = 0.63
```

**Bewertung / Rating:** 👍 Hoch / High (0.63)

**Bedeutung / Meaning:** Ursprünglich mittlere Vertrauenswürdigkeit wird durch Bestätigungen von vertrauenswürdigen Kontakten erhöht.
Originally medium trustworthiness is increased by confirmations from trusted contacts.

---

### Szenario 5: Ablehnung durch vertrauenswürdige Kontakte
### Scenario 5: Rejection by trusted contacts

**Situation:**
- Absender / Sender: Freund (Stufe 2) / Friend (Level 2)
- Hops: 1
- Verifikationen / Verifications:
  - Abgelehnt von / Rejected by: Familienmitglied (Stufe 3) / Family (Level 3)

**Berechnung ohne Verifikationen / Calculation without verifications:**
```
Sender Trust Factor = 0.67
Base Score from Sender = 0.67 × 0.6 = 0.40
Hop Penalty = 1 × 0.1 = 0.1
Hop Score = (1.0 - 0.1) × 0.4 = 0.36
Base Score = 0.40 + 0.36 = 0.76
```

**Verifikation Adjustment / Verification Adjustment:**
```
Verifier 1 (Level 3): Trust Factor = 1.0, Rejected = -1.0
Total Adjustment = -1.0 / 1.0 = -1.0
Scaled Adjustment = -1.0 × 0.15 = -0.15
```

**Finale Berechnung / Final Calculation:**
```
Final Score = 0.76 - 0.15 = 0.61
```

**Bewertung / Rating:** 👍 Hoch → ⚠️ Mittel / High → Medium (0.61)

**Bedeutung / Meaning:** Hohe Vertrauenswürdigkeit wird durch Ablehnung eines engen Kontakts leicht reduziert - Vorsicht geboten.
High trustworthiness is slightly reduced by rejection from close contact - caution advised.

---

## Verwendung / Usage

### 1. TrustManager initialisieren / Initialize TrustManager

```kotlin
val trustManager = TrustManager(context)

// Bekannte Kontakte importieren / Import known contacts
trustManager.importKnownContacts(listOf("alice", "bob", "charlie"))

// Vertrauensstufe manuell setzen / Set trust level manually
trustManager.setTrustLevel("mom", ContactTrustLevel.CLOSE_FAMILY, isManuallySet = true)
trustManager.setTrustLevel("best_friend", ContactTrustLevel.FRIEND, isManuallySet = true)
```

### 2. MessageTrustCalculator verwenden / Use MessageTrustCalculator

```kotlin
val calculator = MessageTrustCalculator(trustManager)

// Trust Score für eine Nachricht berechnen / Calculate trust score for a message
val evaluation = calculator.evaluateMessage(
    messageId = "msg123",
    originalSenderId = "mom",
    hopCount = 0,
    verifications = emptyList()
)

println("Trust Score: ${evaluation.overallTrustScore}") // 1.00
println("Rating: ${MessageTrustEvaluation.getTrustRating(evaluation.overallTrustScore)}") // "Very High"
println("Emoji: ${MessageTrustEvaluation.getTrustEmoji(evaluation.overallTrustScore)}") // "✅"
```

### 3. Verifikationen hinzufügen / Add verifications

```kotlin
val verificationManager = MessageVerificationManager(context)

// Nachricht bestätigen / Confirm message
verificationManager.addVerification(
    messageId = "msg123",
    verifierId = "alice",
    isConfirmed = true,
    comment = "Kann ich bestätigen"
)

// Nachricht ablehnen / Reject message
verificationManager.addVerification(
    messageId = "msg456",
    verifierId = "bob",
    isConfirmed = false,
    comment = "Stimmt nicht"
)

// Verifikationen abrufen / Get verifications
val verifications = verificationManager.getVerifications("msg123")

// Neu bewerten mit Verifikationen / Re-evaluate with verifications
val newEvaluation = calculator.evaluateMessage(
    messageId = "msg123",
    originalSenderId = "unknown_person",
    hopCount = 4,
    verifications = verifications
)
```

### 4. Nachrichten filtern und sortieren / Filter and sort messages

```kotlin
// Nach minimaler Vertrauensstufe filtern / Filter by minimum trust
val trustedMessages = calculator.filterByMinTrust(evaluations, minTrustScore = 0.6)

// Nach Vertrauenswürdigkeit sortieren / Sort by trust
val sortedMessages = calculator.sortByTrust(evaluations)

// Nur hochvertrauenswürdige Nachrichten anzeigen / Show only high-trust messages
val highTrustMessages = evaluations.filter { it.isHighTrust() }
```

---

## Integration in MeshMessage

Die vorhandene `MeshMessage`-Klasse enthält bereits die notwendigen Felder:

The existing `MeshMessage` class already contains the necessary fields:

```kotlin
data class MeshMessage(
    val sourceId: String,        // Original sender (used as originalSenderId)
    val hopCount: Int = 0,      // Number of hops (used directly)
    ...
)
```

**Verwendung / Usage:**

```kotlin
// Wenn eine Nachricht empfangen wird / When receiving a message
val trustEvaluation = calculator.evaluateMessage(
    messageId = message.messageId,
    originalSenderId = message.sourceId,  // Original sender stays the same
    hopCount = message.hopCount,          // Increments with each forward
    verifications = verificationManager.getVerifications(message.messageId)
)
```

---

## Trust Score Interpretation

| Score Range | Rating | Emoji | Bedeutung / Meaning |
|-------------|--------|-------|---------------------|
| 0.80 - 1.00 | Very High | ✅ | Sehr vertrauenswürdig - direkt von engem Kontakt / Very trustworthy - direct from close contact |
| 0.60 - 0.79 | High | 👍 | Vertrauenswürdig - guter Kontakt oder bestätigt / Trustworthy - good contact or verified |
| 0.40 - 0.59 | Medium | ⚠️ | Mittel - mit Vorsicht behandeln / Medium - treat with caution |
| 0.20 - 0.39 | Low | ⚡ | Niedrig - skeptisch sein / Low - be skeptical |
| 0.00 - 0.19 | Very Low | ❌ | Sehr niedrig - höchste Vorsicht / Very low - highest caution |

---

## Einfache Handhabung / Simple Usability

### Für Endbenutzer / For End Users

1. **Automatisch**: Kontakte aus Telefonbuch werden automatisch als "Bekannt" (Stufe 1) importiert
   - **Automatic**: Contacts from phone book are automatically imported as "Known" (Level 1)

2. **Visuell**: Nachrichten zeigen Trust-Emoji (✅, 👍, ⚠️, ⚡, ❌)
   - **Visual**: Messages show trust emoji

3. **Ein-Klick-Verifikation**: "Bestätigen" oder "Ablehnen" Button für jede Nachricht
   - **One-click verification**: "Confirm" or "Reject" button for each message

4. **Optionale Details**: Trust Score und Details auf Anfrage anzeigen
   - **Optional details**: Show trust score and details on request

### Für Entwickler / For Developers

1. **Einfache API**: Drei Hauptklassen (TrustManager, MessageTrustCalculator, MessageVerificationManager)
   - **Simple API**: Three main classes

2. **Automatische Persistenz**: SharedPreferences-basierte Speicherung
   - **Automatic persistence**: SharedPreferences-based storage

3. **Thread-safe**: Concurrent-Collections für Multi-Threading
   - **Thread-safe**: Concurrent collections for multi-threading

4. **Wartungsarm**: Automatische Cleanup-Funktionen für alte Daten
   - **Low maintenance**: Automatic cleanup functions for old data

---

## Zukünftige Erweiterungen / Future Enhancements

1. **Intelligente Vorschläge**: Trust-Level-Vorschläge basierend auf:
   - **Smart suggestions**: Trust level suggestions based on:
   - Kontakthäufigkeit / Contact frequency
   - Anrufdauer / Call duration
   - Nachrichtenanzahl / Message count

2. **Zeitbasierte Anpassung**: Trust Score nimmt ab, wenn Kontakt länger nicht verwendet
   - **Time-based adjustment**: Trust score decreases if contact not used for long time

3. **Gruppen-Vertrauen**: Vertrauensstufen für Gruppen (z.B. "Familie", "Arbeit")
   - **Group trust**: Trust levels for groups (e.g., "Family", "Work")

4. **Reputation-System**: Globale Reputation basierend auf Verifikationsgenauigkeit
   - **Reputation system**: Global reputation based on verification accuracy

---

## Testen / Testing

Siehe Testdateien unter `app/src/test/java/com/fourpeople/adhoc/trust/`:

See test files under `app/src/test/java/com/fourpeople/adhoc/trust/`:

- `TrustManagerTest.kt` - Tests für Kontakt-Vertrauensstufen
- `MessageTrustCalculatorTest.kt` - Tests für Trust-Score-Berechnung
- `MessageVerificationManagerTest.kt` - Tests für Verifikationsverwaltung

```bash
# Tests ausführen / Run tests
./gradlew test
```
