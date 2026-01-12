# Ultraschall-Signalisierung und Taschenlampen-Morse-Code

## Übersicht

Diese Funktionen erweitern die 4people-App um zwei zusätzliche Kommunikationskanäle für Notfallsituationen:

1. **Flashlight Morse Code** - Visuelle LED-basierte Signalisierung
2. **Ultrasound Signaling** - Audio-basierte Signalisierung über unhörbare Frequenzen

## Betriebsmodi

Die App arbeitet in zwei Hauptmodi, die unterschiedliche Überwachungsstrategien verwenden:

### Standby-Modus (Passive Überwachung)
In diesem Modus läuft die App im Hintergrund mit minimalem Batterieverbrauch:
- **WiFi-Scanning**: Alle 30 Sekunden (adaptiv je nach Batteriestand)
- **Telefon-Anruf-Indikatoren**: Kontinuierliche Überwachung auf kurze Anrufe
- **Ultrasound-Listening**: **Kontinuierliche passive Erkennung** von Ultraschall-Notfall-Signalen
- **Flashlight**: Nicht aktiv (nur visuell erkennbar wenn manuell gesendet)

**Überwachte Kanäle im Standby:**
1. WiFi-Netzwerke mit "4people-*" Pattern (periodisch)
2. Kurze Telefonanrufe < 5 Sekunden (ereignisbasiert)
3. Ultraschall-Notfall-Beacons (kontinuierlich)
4. Emergency-Broadcasts von anderen Geräten (ereignisbasiert)

### Emergency-Modus (Aktive Signalisierung + Überwachung)
In diesem Modus sind alle Kommunikationskanäle voll aktiviert:
- **WiFi-Scanning**: Alle 10 Sekunden (häufigere Überprüfung)
- **Bluetooth**: Discovery und Advertising aktiv
- **WiFi Hotspot**: Aktiv (falls unterstützt)
- **WiFi Direct**: Peer-to-Peer Discovery aktiv
- **Ultrasound-Transmission**: **Aktives Senden** von 3-Puls-Notfall-Beacons (falls aktiviert)
- **Ultrasound-Listening**: Weiterhin aktiv
- **Flashlight Morse**: **Aktives Senden** von "4PEOPLE" Identifikations-Signal (falls aktiviert)
- **SMS-Broadcast**: Notfall-SMS an Kontakte (falls aktiviert)

**Alle Kanäle im Emergency-Modus:**
1. WiFi-Netzwerk-Scanning (häufiger)
2. Bluetooth Discovery & Advertising
3. WiFi Hotspot Creation
4. WiFi Direct P2P
5. Telefon-Anruf-Indikatoren (weiterhin)
6. Ultraschall-Transmission + Listening
7. Flashlight Morse-Code-Signalisierung
8. SMS Emergency Broadcast

### Übergang zwischen Modi
- **Standby → Emergency**: Automatisch bei Erkennung eines Notfall-Indikators (wenn Auto-Aktivierung an) oder manuell
- **Emergency → Standby**: Manuell durch Benutzer

---

## Flashlight Morse Code (Taschenlampen-Morse-Code)

### Beschreibung
Nutzt die LED-Taschenlampe des Geräts, um Morse-Code-Signale zu senden. Ideal für große Distanzen bei Sichtverbindung.

### Eigenschaften
- **Reichweite**: Bis zu 1 km bei direkter Sichtverbindung
- **Energieverbrauch**: Sehr gering
- **Sichtbarkeit**: Tag und Nacht sichtbar
- **Keine Netzwerk erforderlich**: Funktioniert vollständig offline

### Signalmuster

#### SOS-Signal
- Muster: `... --- ...` (3 kurz, 3 lang, 3 kurz)
- Wiederholt sich alle 5 Sekunden
- Internationales Notsignal

#### Emergency-Identifikation
- Muster: "4PEOPLE" in Morse-Code
- Wiederholt sich alle 10 Sekunden
- Identifiziert das Gerät als Teil des 4people-Netzwerks

### Morse-Code-Timing
Basierend auf internationalem Standard:
- **Kurzes Signal (Punkt)**: 200ms
- **Langes Signal (Strich)**: 600ms (3x Punkt)
- **Pause zwischen Symbolen**: 200ms
- **Pause zwischen Buchstaben**: 600ms (3x Punkt)
- **Pause zwischen Wörtern**: 1400ms (7x Punkt)

### Aktivierung
1. Öffne die **Settings** in der App
2. Aktiviere **"Flashlight Morse Code"**
3. Bei Notfall-Aktivierung sendet die App automatisch das Identifikations-Signal

### Technische Anforderungen
- Android-Gerät mit Kamera-LED/Taschenlampe
- `CAMERA` Berechtigung

---

## Ultrasound Signaling (Ultraschall-Signalisierung)

### Beschreibung
Sendet und empfängt Notfall-Signale über hochfrequente Audio-Töne (19 kHz), die für Menschen typischerweise unhörbar sind.

### Eigenschaften
- **Reichweite**: 5-10 Meter
- **Frequenz**: 19 kHz (Ultraschall-Bereich)
- **Durchdringung**: Kann durch Wände und Türen gehen (begrenzt)
- **Unhörbar**: Für die meisten Menschen nicht wahrnehmbar
- **Funktioniert bei gesperrten Geräten**: Empfang möglich auch wenn Display aus ist

### Funktionsweise

#### Sender (Transmission)
- Sendet 3 Ultraschall-Pulse als Notfall-Beacon
- Jeder Puls dauert 500ms
- Pause zwischen Pulsen: 500ms
- Wiederholt sich alle 3 Sekunden

#### Empfänger (Detection)
- Hört kontinuierlich auf Ultraschall-Signale
- Erkennt das 3-Puls-Muster
- Löst Notfall-Alarm bei Erkennung aus
- Verhindert mehrfache Auslösung (2 Sekunden Verzögerung)

### Aktivierung

#### Senden aktivieren
1. Öffne die **Settings** in der App
2. Aktiviere **"Transmit ultrasound emergency signals"**
3. Bei Notfall-Aktivierung sendet die App automatisch Ultraschall-Signale
   - **Nur im Emergency-Modus** (aktive Signalisierung)

#### Empfang aktivieren (empfohlen)
1. Öffne die **Settings** in der App
2. Aktiviere **"Listen for ultrasound emergency signals"** (standardmäßig aktiv)
3. Die App hört kontinuierlich auf Signale in zwei Modi:
   - **Standby-Modus**: Passive Überwachung mit geringem Batterieverbrauch
   - **Emergency-Modus**: Aktive Überwachung zusammen mit anderen Kanälen

### Technische Details

#### Audio-Spezifikationen
- **Trägerfrequenz**: 19 kHz
- **Sample Rate**: 44.1 kHz
- **Format**: PCM 16-bit Mono
- **Amplitude**: 80% des Maximums
- **Detection Threshold**: 30% Amplitude

#### Anforderungen
- Android-Gerät mit Mikrofon und Lautsprecher
- `RECORD_AUDIO` Berechtigung (für Empfang)
- Lautsprecher-Fähigkeit bis 20+ kHz

### Einschränkungen
- **Umgebungsgeräusche**: Störungen durch laute Umgebung möglich
- **Geräte-Limitierungen**: Nicht alle Lautsprecher/Mikrofone unterstützen 19 kHz
- **Bandbreite**: Sehr begrenzt, nur für einfache Signalisierung
- **Reichweite**: Relativ kurz (5-10m)

---

## Kombination mit anderen Kanälen

Diese beiden Funktionen ergänzen die bestehenden Kommunikationskanäle:

### Bestehende Kanäle
1. Bluetooth Discovery & Advertising
2. WiFi Network Scanning
3. WiFi Hotspot
4. WiFi Direct
5. Phone Call Indicators
6. SMS Broadcast

### Neue Kanäle
7. **Flashlight Morse Code** ← NEU
8. **Ultrasound Signaling** ← NEU

### Strategie
- **Urban/Dicht besiedelt**: WiFi, Bluetooth, Ultrasound
- **Weitläufig/Outdoor**: Flashlight Morse, Phone Calls, SMS
- **Durch Wände**: Ultrasound, WiFi, Bluetooth
- **Große Distanz**: Flashlight Morse (Sichtverbindung)
- **Backup/Redundanz**: Alle Kanäle aktiviert

---

## Sicherheit & Datenschutz

### Flashlight Morse Code
- ✅ Keine persönlichen Daten übertragen
- ✅ Nur vordefinierte Signalmuster
- ✅ Visuell erkennbar (transparent)
- ⚠️ Bei Nacht sehr auffällig

### Ultrasound Signaling
- ✅ Keine persönlichen Daten übertragen
- ✅ Nur einfaches Beacon-Signal
- ✅ Unhörbar für Menschen
- ⚠️ Theoretisch von Apps mit Mikrofon-Zugriff erkennbar

---

## Verwendungsszenarien

### Szenario 1: Gebäude-Einsturz
**Problem**: Menschen unter Trümmern, kein Mobilfunknetz
**Lösung**: 
- Ultrasound kann durch Trümmer dringen
- Rettungskräfte mit App können Signale orten
- Flashlight als Backup wenn Sichtverbindung

### Szenario 2: Nacht-Evakuierung
**Problem**: Dunkelheit, Menschen suchen sich gegenseitig
**Lösung**:
- Flashlight Morse-Signale weithin sichtbar
- "4PEOPLE" Signal identifiziert App-Nutzer
- Koordination über visuelle Signale

### Szenario 3: Mehrere Stockwerke
**Problem**: Koordination zwischen Etagen ohne Netz
**Lösung**:
- Ultrasound durchdringt Decken/Böden
- Automatische Erkennung auf allen Etagen
- Kaskadeneffekt aktiviert weitere Geräte

### Szenario 4: Outdoor/Wandern
**Problem**: Bergwanderung, Person vermisst, große Distanz
**Lösung**:
- Flashlight Morse sichtbar über große Distanz
- SOS-Signal universell erkennbar
- Auch ohne 4people-App verständlich

---

## Best Practices

### Batterie-Schonung
- ✅ Ultrasound-Senden nur wenn nötig aktivieren
- ✅ Ultrasound-Empfang kann dauerhaft an bleiben (geringer Verbrauch)
- ✅ Flashlight automatisch (sendet nur im Emergency-Modus)

### Effektivität
- ✅ Beide Kanäle gleichzeitig nutzen für maximale Reichweite
- ✅ Flashlight vor allem Nachts sehr effektiv
- ✅ Ultrasound gut für Indoor-Szenarien

### Datenschutz
- ✅ Keine persönlichen Daten in Signalen
- ✅ Nur Notfall-Indikatoren
- ✅ Transparent und erklärbar

---

## Technische Implementierung

### Dateien
- `FlashlightMorseHelper.kt` - Morse-Code-Logik
- `UltrasoundSignalHelper.kt` - Ultraschall-Logik
- `AdHocCommunicationService.kt` - Integration in Service
- `SettingsActivity.kt` - Benutzer-Einstellungen

### Tests
- `FlashlightMorseTest.kt` - Unit-Tests für Morse-Code
- `UltrasoundSignalTest.kt` - Unit-Tests für Ultraschall

### Berechtigungen
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<uses-feature android:name="android.hardware.camera.flash" android:required="false" />
<uses-feature android:name="android.hardware.microphone" android:required="false" />
```

---

## Weiterführende Informationen

Siehe auch:
- [README.md](README.md) - Hauptdokumentation
- [NOTFALL_SZENARIEN.md](NOTFALL_SZENARIEN.md) - Detaillierte Szenarien-Analyse
- [IMPLEMENTATION.md](IMPLEMENTATION.md) - Technische Implementierung

---

## Lizenz

Teil der 4people Emergency Communication App.
Konzept-Implementierung für Ad-hoc-Notfallkommunikation.
