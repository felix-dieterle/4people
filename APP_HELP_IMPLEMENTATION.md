# App-Hilfe mit Ablaufdiagrammen

## Übersicht

Diese Implementierung fügt eine neue HelpActivity zur 4people-App hinzu, die verschiedene Szenarien anhand von beispielhaften Ablaufdiagrammen anschaulich macht, ausgehend vom Ruhezustand (Idle State).

## Implementierte Funktionen

### 1. HelpActivity
Eine neue Activity mit Tab-Navigation, die vier verschiedene Szenarien darstellt:

- **Ruhezustand (Idle State)**: Zeigt, was passiert, wenn die App im Hintergrund läuft
- **Notfall-Aktivierung (Emergency Mode)**: Der komplette Ablauf bei Notfall-Erkennung
- **Panic Mode**: Progressive Eskalation des Panic-Modus
- **Netzwerk-Kaskadeneffekt**: Wie sich das Notfall-Netzwerk ausbreitet

### 2. Visuelle Ablaufdiagramme
Jedes Szenario enthält:
- ASCII-basierte Flussdiagramme für mobile Darstellung
- Schritt-für-Schritt Erklärungen
- Wichtige Informationen zu Batterieverbrauch, Reichweite und Funktionen
- Deutsche Beschreibungen (passend zur Problemstellung)

### 3. Benutzerfreundliche Navigation
- Tab-basierte Navigation zwischen Szenarien
- Direkter Zugriff aus MainActivity über die Hilfe-Buttons
- Zurück-Navigation zur MainActivity

## Geänderte Dateien

### Neue Dateien
1. **HelpActivity.kt**: Haupt-Activity mit ViewPager2 und TabLayout
2. **HelpPagerAdapter.kt**: Adapter für Tab-Navigation
3. **HelpFlowFragment.kt**: Fragment zur Darstellung der Ablaufdiagramme
4. **activity_help.xml**: Layout für HelpActivity
5. **fragment_help_flow.xml**: Layout für die Fragment-Ansicht

### Modifizierte Dateien
1. **MainActivity.kt**: 
   - `showEmergencyModeHelp()` öffnet jetzt HelpActivity (Tab: Emergency Mode)
   - `showPanicModeHelp()` öffnet jetzt HelpActivity (Tab: Panic Mode)

2. **strings.xml**: 
   - Neue Strings für Titel und Tab-Namen
   - Vier umfangreiche Flow-Diagramm-Inhalte in deutscher Sprache
   - CDATA-Sections für HTML-Formatierung

3. **AndroidManifest.xml**: 
   - HelpActivity registriert mit parentActivity

4. **build.gradle.kts**: 
   - ViewPager2-Dependency hinzugefügt

## Ablaufdiagramm-Inhalte

### Ruhezustand (Idle State)
Zeigt den automatischen Start beim Geräteboot:
- BootReceiver startet StandbyMonitoringService
- WiFi-Scanning alle 30 Sekunden
- Telefon-Anruf-Überwachung
- Batterie-Optimierung (1-2%/h)

### Notfall-Aktivierung
Detaillierter Ablauf der Notfall-Erkennung:
- Verschiedene Erkennungsmethoden (WiFi/Anruf/Manuell)
- Auto-Aktivierung vs. Benachrichtigung
- Aktivierte Features (Bluetooth, WiFi, GPS, Mesh, etc.)
- Batterieverbrauch (5-10%/h)

### Panic Mode
Progressive Eskalationsstufen:
- Timer: Bestätigung alle 30 Sekunden
- Stufe 1: Sanfte Warnung (Vibration/Sound)
- Stufe 2: Massiver Alarm (Taschenlampe, Sirene)
- Stufe 3: Notfall-Kontakt-Benachrichtigung
- Progressive Intervalle (3min → 6min → 12min → 24min)

### Netzwerk-Kaskadeneffekt
Zeitlicher Verlauf der Netzwerk-Bildung:
- T+0: Initialisierung durch Person 1
- T+30s: Erste Welle (2-3 Personen)
- T+2-5min: Kaskadeneffekt (4-8 Geräte)
- T+10-30min: Etabliertes Mesh-Netzwerk (20+ Geräte)
- Reichweiten-Erweiterung durch Multi-Hop-Routing

## Technische Details

### Verwendete Komponenten
- **ViewPager2**: Für horizontales Wischen zwischen Tabs
- **TabLayout**: Material Design Tabs
- **TabLayoutMediator**: Verbindung zwischen ViewPager2 und TabLayout
- **Fragment**: Für modulare Ansichten
- **ViewBinding**: Typsicherer Zugriff auf Views
- **HtmlCompat**: Für HTML-Formatierung in TextViews

### Formatierung
- ASCII-Art Diagramme mit Unicode-Zeichen (┌─┐│└┘▼►)
- HTML-Formatierung für fettgedruckten Text (`<b>`)
- Zeilenumbrüche mit `<br/>`
- Monospace-Font für bessere Diagramm-Darstellung
- Scrollable Content für lange Diagramme

## Benutzer-Interaktion

### Aus MainActivity
1. Benutzer klickt auf Info-Button (ⓘ) neben "Activate Emergency Communication"
   → HelpActivity öffnet sich mit Tab "Emergency Mode"

2. Benutzer klickt auf Info-Button (ⓘ) neben "Activate Panic Mode"
   → HelpActivity öffnet sich mit Tab "Panic Mode"

### In HelpActivity
- Benutzer kann zwischen 4 Tabs wechseln
- Jeder Tab zeigt ein anderes Szenario
- Zurück-Button führt zur MainActivity

## Vorteile dieser Implementierung

1. **Visuell**: Benutzer sehen genau, was in jedem Zustand passiert
2. **Bildend**: Verständnis für die App-Funktionsweise wird verbessert
3. **Offline**: Alle Inhalte sind lokal verfügbar
4. **Mehrsprachig-fähig**: Durch strings.xml leicht übersetzbar
5. **Wartbar**: Inhalte können einfach in strings.xml aktualisiert werden
6. **Mobil-optimiert**: ASCII-Art funktioniert auf allen Bildschirmgrößen

## Zukünftige Erweiterungen

Mögliche Verbesserungen:
- [ ] Animierte Ablaufdiagramme
- [ ] Interaktive Diagramme mit klickbaren Elementen
- [ ] Dark Mode Unterstützung für bessere Lesbarkeit
- [ ] Zoom-Funktion für Diagramme
- [ ] Export/Teilen-Funktion für Diagramme
- [ ] Zusätzliche Szenarien (z.B. Batterie-Optimierung, Mesh-Routing Details)

## Testplan

Zum Testen der Implementierung:
1. App installieren und starten
2. Auf Info-Button (ⓘ) neben Emergency-Button klicken
3. Verifizieren, dass HelpActivity mit 4 Tabs öffnet
4. Durch alle Tabs wischen und Inhalte prüfen
5. Zurück-Navigation zur MainActivity testen
6. Auf Info-Button (ⓘ) neben Panic-Button klicken
7. Verifizieren, dass HelpActivity mit Panic-Tab öffnet

## Zusammenfassung

Diese Implementierung erfüllt die Anforderung, "die verschiedenen Szenarien in der App Hilfe anhand von beispielhaften Ablaufdiagramme anschaulich zu machen, also ausgehend von dem was zunächst im Ruhezustand passiert".

Die Ablaufdiagramme sind:
- ✅ Beispielhaft und anschaulich
- ✅ Starten vom Ruhezustand (Idle State Tab)
- ✅ Zeigen verschiedene Szenarien (4 Tabs)
- ✅ In deutscher Sprache (wie in der Anforderung)
- ✅ Mobil-optimiert für die App
- ✅ Leicht wartbar und erweiterbar
