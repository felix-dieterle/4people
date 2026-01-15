# Offline Map Integration

## Overview

The 4people app now includes an integrated OpenStreetMap-based offline map for emergency navigation and coordination. This feature allows users to:

- View all network participants on a real-time map
- Mark and share safe zones (collection points)
- Display routes to nearest safe zones
- Track help requests visually
- Navigate without requiring internet connectivity

## Features

### OSM-based Offline Maps

The implementation uses **osmdroid** (version 6.1.18), a popular open-source library for displaying OpenStreetMap tiles in Android applications. Key characteristics:

- **Offline capability**: Map tiles can be cached locally for use without internet
- **No API key required**: Unlike Google Maps, OSM doesn't require API keys
- **Open source**: Fully compatible with emergency/humanitarian applications
- **Standard OSM tiles**: Uses Mapnik tile source for familiar map appearance

### Real-time Participant Tracking

All participants in the emergency network are displayed on the map with:

- **🟢 Green markers**: Regular network participants
- **🆘 Red markers**: Participants requesting help
- **Live updates**: Locations update automatically every 30 seconds
- **Device identification**: Each marker shows the device ID
- **Location accuracy**: Displays GPS accuracy information

### Safe Zones (Collection Points)

Users can mark safe zones on the map:

- **🏠 Safe zone markers**: Designated emergency gathering points
- **Interactive adding**: Tap the + FAB button to add a safe zone at map center
- **Named locations**: Each safe zone has a user-defined name
- **Shared across network**: Safe zones are synchronized via SafeZoneManager
- **Validity tracking**: Safe zones expire after 24 hours

### Route Display

The map shows routes to safety:

- **Tap to route**: Click any safe zone marker to display a route
- **Distance calculation**: Shows distance in meters to the safe zone
- **Visual path**: Blue line drawn from current position to destination
- **Nearest safe zone**: SafeZoneManager can identify closest safe zone

## Technical Architecture

### Data Flow

```
LocationSharingManager (GPS) → LocationDataStore (Singleton) → OfflineMapActivity (UI)
                                       ↑
                               AdHocCommunicationService
                               (Receives remote locations)

SafeZoneManager (Singleton) ⟷ OfflineMapActivity (UI)
```

### Key Components

1. **OfflineMapActivity**: Main map display activity
   - Implements LocationDataStore.LocationUpdateListener
   - Implements SafeZoneManager.SafeZoneUpdateListener
   - Manages osmdroid MapView and overlays
   - Handles user interactions (adding safe zones, viewing routes)

2. **LocationDataStore**: Singleton for location data
   - Thread-safe ConcurrentHashMap storage
   - Real-time update notifications to listeners
   - Automatic cleanup of stale data (>10 minutes)
   - Centralized access point for all location data

3. **SafeZoneManager**: Singleton for safe zones
   - Manages safe zone creation and sharing
   - Calculates distances using Haversine formula
   - Identifies nearest safe zones
   - Automatic cleanup of invalid zones (>24 hours)

4. **SafeZone**: Data class for collection points
   - ID, name, coordinates, description
   - Capacity information
   - Timestamp for validity checking

### Integration Points

The offline map integrates with existing components:

- **LocationSharingManager**: Automatically updates LocationDataStore with GPS locations
- **AdHocCommunicationService**: Feeds received location broadcasts to LocationDataStore
- **LocationMapActivity**: Provides button to launch offline map view
- **Mesh network**: Location data propagates through the mesh network

## Usage

### Accessing the Map

1. Open the app and activate emergency communication
2. Navigate to "View Participant Map" from the main screen
3. Click "Open Offline Map" button
4. The interactive OSM map will open

### Viewing Participants

- Participant markers appear automatically as locations are received
- Green markers (🟢) show regular participants
- Red markers (🆘) show help requests
- Tap any marker to see details

### Adding Safe Zones

1. Pan the map to the desired location
2. Tap the + FAB (Floating Action Button) in the bottom-right
3. Enter a name for the safe zone
4. Confirm to add the marker
5. The safe zone is now visible to all app users in the network

### Viewing Routes

1. Tap any safe zone marker (🏠)
2. A blue route line appears from your position to the safe zone
3. Distance in meters is displayed in a toast message
4. The marker info window shows the safe zone details

### Centering on Your Location

- Tap the "Center on My Location" button in the legend panel
- The map will animate to your current position

## Configuration

### Map Settings

Default map configuration in OfflineMapActivity:

```kotlin
companion object {
    private const val DEFAULT_ZOOM = 15.0
    private const val DEFAULT_LAT = 51.5074 // London as default
    private const val DEFAULT_LON = -0.1278
}
```

### Tile Source

Uses standard OpenStreetMap tiles via:

```kotlin
setTileSource(TileSourceFactory.MAPNIK)
```

### Offline Tile Storage

osmdroid automatically caches tiles to:
- `/storage/emulated/0/osmdroid/tiles/` (Android)

For true offline operation, pre-download tiles for your area of interest.

## Permissions

The offline map requires:

- `ACCESS_FINE_LOCATION`: For GPS positioning
- `ACCESS_COARSE_LOCATION`: For network-based positioning
- `INTERNET`: For downloading map tiles (optional in offline mode)
- `WRITE_EXTERNAL_STORAGE`: For caching tiles (API ≤28)
- `READ_EXTERNAL_STORAGE`: For reading cached tiles (API ≤32)

## Limitations

1. **Internet for initial tiles**: First-time use requires internet to download map tiles
2. **Storage usage**: Cached map tiles can consume significant storage
3. **Route accuracy**: Current implementation shows straight-line routes, not real road routing
4. **Safe zone persistence**: Safe zones are in-memory only, not persisted to disk
5. **Map detail**: Offline capability depends on pre-cached tile coverage

## Future Enhancements

Potential improvements to the offline map feature:

- [ ] Pre-bundled offline tiles for common emergency areas
- [ ] Real routing algorithms (A*, Dijkstra) for accurate paths
- [ ] Persistent safe zone storage (SQLite database)
- [ ] Heatmap overlay showing network coverage density
- [ ] Historical participant movement trails
- [ ] Distance measurement tool
- [ ] Area measurement for safe zones
- [ ] Custom map styles (night mode, high-contrast)
- [ ] Export map snapshots as images
- [ ] Integration with OpenRouteService for turn-by-turn directions

## References

- **osmdroid**: https://github.com/osmdroid/osmdroid
- **OpenStreetMap**: https://www.openstreetmap.org/
- **OSM Tile Servers**: https://wiki.openstreetmap.org/wiki/Tile_servers
- **Haversine Formula**: https://en.wikipedia.org/wiki/Haversine_formula
