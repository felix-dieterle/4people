# Emergency Propagation Simulation

## Overview

The Emergency Propagation Simulation is a visual tool that demonstrates how emergency messages spread through a network of people using the 4people app. This simulation helps visualize and understand the effectiveness of different scenarios and configurations.

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
- **People Count**: Adjust the number of people in the simulation (10-210)
- **App Adoption Rate**: Set the percentage of people who have the app (5%-90%)

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
2. A percentage of people (based on app adoption rate) have the app installed
3. 30% of people are randomly selected to be moving
4. WiFi networks are randomly placed (approximately 1 per 10 people)

### Event Detection
When an event is started:
1. The event occurs at a random location
2. People within 100 meters who have the app immediately detect the event
3. These people become "informed" and are marked in gold/yellow

### Message Propagation
The simulation models two propagation mechanisms:

#### 1. Direct Peer-to-Peer (Bluetooth/WiFi Direct)
- Informed people can share the message with uninformed people within 100 meters
- This simulates direct device-to-device communication

#### 2. WiFi Network Propagation
- If an informed person and an uninformed person are both within range of the same WiFi network, the message propagates
- This simulates message sharing through WiFi access points

### Movement Simulation
- Moving people follow a random walk pattern
- They move at approximately 5 km/h (1.4 m/s)
- Direction changes randomly to simulate natural walking behavior
- People stay within the simulation area boundaries

## Use Cases

### Scenario Testing
Test different scenarios to understand message propagation:

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
