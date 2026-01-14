# Emergency Propagation Simulation

## Overview

The Emergency Propagation Simulation is a visual tool that demonstrates how emergency messages spread through a network of people using the 4people app. This simulation helps visualize and understand the effectiveness of different scenarios and configurations.

## New Features

### Predefined Scenarios

The simulation now includes 9 predefined scenarios representing typical emergency situations:

#### Location Types
- **Stadtmitte Großstadt (Big City Center)**: Dense urban environment with many buildings and WiFi networks
- **Stadt (Medium City)**: Moderate density with mixed indoor/outdoor population
- **Dorf (Village)**: Sparse rural area with mostly outdoor population

#### Infrastructure Failure Modes
Each location type has three infrastructure failure scenarios:

1. **Nur Mobile Daten ausgefallen (Only Mobile Data Failed)**
   - Voice calls and SMS still work
   - WiFi networks functional
   - No verbal transmission or approaching behavior
   
2. **Daten Backbone ausgefallen (Data Backbone Failed)**
   - No internet connectivity
   - Local networks still work
   - Phone calls available
   - **Verbal transmission enabled**: People inform others within speaking distance
   - **Approaching behavior enabled**: Informed people actively approach nearby uninformed people
   
3. **Telefon auch ausgefallen (Complete Telephone Failure)**
   - Complete infrastructure collapse
   - Only local ad-hoc networks work
   - **Verbal transmission enabled**: Critical for information spread
   - **Approaching behavior enabled**: People actively seek out others to inform
   - Increased movement as people search for help

### Scenario Parameters

Each scenario is configured with realistic parameters:

**Big City Center (Großstadt)**
- Population: 150 people
- App adoption: 60%
- Indoor ratio: 70% (most people in buildings)
- WiFi density: 2.0 networks per 10 people (high)
- Moving people: 40-50%
- Verbal range: 15m (reduced by buildings)
- Approaching range: 50m (limited by buildings)

**Medium City (Stadt)**
- Population: 80 people
- App adoption: 45%
- Indoor ratio: 50% (mixed)
- WiFi density: 1.5 networks per 10 people
- Moving people: 30-40%
- Verbal range: 20m
- Approaching range: 75m

**Village (Dorf)**
- Population: 40 people
- App adoption: 30% (lower in rural areas)
- Indoor ratio: 30% (mostly outdoor)
- WiFi density: 0.8 networks per 10 people (sparse)
- Moving people: 20-30%
- Verbal range: 30m (open space, better range)
- Approaching range: 150m (better line of sight)

## Features

### Visual Map
- **People**: Displayed as colored circles
  - 🟢 Green: Has the app but not yet informed
  - 🟡 Gold/Yellow: Has the app and received the event notification
  - ⚫ Gray: Does not have the app
  - Black outline: Person is moving
- **WiFi Networks**: Blue circles showing WiFi access points and their range
- **Event**: Red circle marking the event location with a 100-meter detection radius

### Simulation Controls

#### Playback Controls
- **Play/Pause**: Start or pause the simulation
- **Reset**: Reset the simulation to initial state
- **Start Event**: Trigger an emergency event at a random location

#### Speed Control
- **1x**: Real-time speed
- **2x**: 2x faster than real-time
- **5x**: 5x faster than real-time
- **10x**: 10x faster than real-time

#### Configuration Parameters
- **Scenario Selection**: Choose from 9 predefined scenarios or use custom settings
  - **Eigene Einstellungen (Custom)**: Manual configuration
  - **Big City scenarios**: 3 infrastructure failure modes
  - **Medium City scenarios**: 3 infrastructure failure modes
  - **Village scenarios**: 3 infrastructure failure modes
- **People Count**: Adjust the number of people (10-210) when in custom mode
- **App Adoption Rate**: Set the percentage of people who have the app (5%-90%) when in custom mode

**Note**: When a predefined scenario is selected, People Count and App Adoption Rate are automatically set according to the scenario parameters and cannot be manually adjusted.

### Real-Time Statistics

The simulation displays:
- Total number of people
- Number of people with the app
- Number of informed people (who received the event notification)
- Number of uninformed people (with app but not yet notified)
- Number of WiFi networks
- Coverage percentage (informed / total with app)

## How the Simulation Works

### Initialization
1. People are randomly distributed across a 1km × 1km area
2. A percentage of people (based on app adoption rate or scenario) have the app installed
3. A percentage of people are randomly selected to be moving (based on scenario)
4. Indoor/outdoor distribution is assigned based on scenario (0-70% indoor)
5. WiFi networks are randomly placed based on scenario density (0.8-2.0 per 10 people)

### Event Detection
When an event is started:
1. The event occurs at a random location
2. People within 100 meters who have the app immediately detect the event
3. These people become "informed" and are marked in gold/yellow

### Message Propagation
The simulation models multiple propagation mechanisms:

#### 1. Direct Peer-to-Peer (Bluetooth/WiFi Direct)
- Informed people can share the message with uninformed people within 100 meters
- This simulates direct device-to-device communication
- Signal attenuation when people are indoors (60% range reduction)

#### 2. WiFi Network Propagation
- If an informed person and an uninformed person are both within range of the same WiFi network, the message propagates
- This simulates message sharing through WiFi access points

#### 3. Verbal Transmission (Critical Scenarios Only)
- Enabled in severe infrastructure failures (Data Backbone or Complete Failure)
- People inform others within speaking distance (15-30m depending on environment)
- Range is reduced indoors (walls dampen sound)
- Does not require app on both devices - anyone can be informed verbally

#### 4. Approaching Behavior (Critical Scenarios Only)
- Enabled in severe infrastructure failures
- Informed people actively seek out nearby uninformed people
- Movement speed increases when approaching (2.0 m/s vs 1.4 m/s normal walking)
- Approaching radius varies by location type (50-150m)
- Simulates helpful behavior during emergencies where people actively inform others

### Movement Simulation
- Moving people follow a random walk pattern
- They move at approximately 5 km/h (1.4 m/s)
- Direction changes randomly to simulate natural walking behavior
- People stay within the simulation area boundaries

## Use Cases

### Scenario Testing
Test different scenarios to understand message propagation:

#### Predefined Scenarios
1. **Big City Center Scenarios**: 
   - Test high-density urban environments
   - Observe how buildings affect signal propagation
   - See the effect of verbal transmission in critical situations
   
2. **Medium City Scenarios**:
   - Balanced indoor/outdoor mix
   - Moderate WiFi network density
   - Good for comparing different failure modes
   
3. **Village Scenarios**:
   - Sparse population, larger distances
   - Better line-of-sight for approaching behavior
   - Test how rural areas cope with infrastructure failure

#### Custom Testing
1. **Low Adoption (5-20%)**: How effective is the network with few users?
2. **Medium Adoption (40-60%)**: What is the sweet spot for coverage?
3. **High Adoption (70-90%)**: How quickly can a message reach everyone?

### Coverage Analysis
- Observe how many people receive the notification over time
- Identify coverage gaps in different adoption scenarios
- Understand the role of WiFi networks in bridging gaps

### Movement Impact
- See how moving people help spread messages to different areas
- Observe the difference between static and dynamic populations

### Network Density
- Test with different population densities (10 vs 200 people)
- Understand minimum viable network density for effective propagation

## Technical Implementation

### Components

#### Data Models
- **SimulationPerson**: Represents an individual with location, app status, and movement properties
- **SimulationWiFi**: Represents a WiFi access point with location and range
- **SimulationEvent**: Represents an emergency event with location and detection radius
- **SimulationStatistics**: Tracks real-time simulation metrics

#### Core Engine
- **SimulationEngine**: Manages the simulation state, updates positions, and handles message propagation
  - Distance calculation using Haversine formula
  - Time-based position updates
  - Propagation logic for peer-to-peer and WiFi-based sharing

#### Visualization
- **SimulationMapView**: Custom Android view that renders the simulation state
  - Coordinate transformation (GPS → screen pixels)
  - Color-coded rendering of people, WiFi networks, and events
  - Distance-based scaling for circles

#### User Interface
- **SimulationActivity**: Main activity managing the simulation
  - Real-time updates (100ms intervals)
  - Speed multiplier control
  - Dynamic parameter adjustment

### Performance Considerations

- Updates occur every 100ms in real-time (faster with speed multiplier)
- Efficient distance calculations using concurrent data structures
- State change listeners for UI updates
- Optimized rendering using Android Canvas

## Future Enhancements

Possible improvements to the simulation:

- [ ] Predefined scenario templates (disaster zones, events, urban areas)
- [ ] Obstacle simulation (buildings blocking signals)
- [ ] Battery drain modeling
- [ ] Network congestion simulation
- [ ] Export simulation results for analysis
- [ ] Heat map visualization of message spread
- [ ] Multiple simultaneous events
- [ ] Different movement patterns (commuters, crowds, emergency responders)
- [ ] 3D terrain visualization
- [ ] Real map integration (OpenStreetMap)

## Testing

The simulation includes comprehensive unit tests:

- **SimulationEngineTest**: Tests initialization, event detection, message propagation, movement, and statistics
- **SimulationDataClassesTest**: Tests data model creation and properties

Run tests with:
```bash
./gradlew test
```

## Accessing the Simulation

From the main app screen:
1. Tap the "Open Simulation" button
2. The simulation opens with default parameters
3. Adjust parameters as needed
4. Tap "Start Event" to begin an event
5. Tap "Play" to start the simulation
6. Use speed controls to accelerate time
7. Observe the message propagation on the map

## Practical Insights

The simulation can help answer questions like:
- What is the minimum app adoption rate for effective coverage?
- How important are WiFi networks in message propagation?
- How quickly can a message reach 90% of users?
- What happens in sparse vs. dense populations?
- How do moving people contribute to message spread?

These insights can inform real-world deployment strategies and user education efforts.
