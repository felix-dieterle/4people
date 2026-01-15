package com.fourpeople.adhoc

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fourpeople.adhoc.databinding.ActivitySimulationBinding
import com.fourpeople.adhoc.simulation.*
import java.util.*

/**
 * Activity for visualizing the emergency event propagation simulation.
 * 
 * Features:
 * - Visual map showing people, WiFi networks, and event
 * - Time controls (play/pause, speed adjustment)
 * - Parameter configuration (people count, app adoption rate)
 * - Real-time statistics
 */
class SimulationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySimulationBinding
    private var simulationEngine: SimulationEngine? = null
    private var isRunning = false
    private var simulationSpeed = 1 // 1x, 2x, 5x, 10x
    private var currentScenario: SimulationScenario? = null
    private var useScenario = false // Track if we're using a scenario or custom settings
    
    private val updateHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                // Update simulation (100ms real time)
                simulationEngine?.update(100L * simulationSpeed)
                updateUI()
                
                // Schedule next update
                updateHandler.postDelayed(this, 100L)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimulationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Emergency Propagation Simulation"
        
        setupUI()
        initializeSimulation()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopSimulation()
    }
    
    private fun setupUI() {
        // Scenario selection
        setupScenarioSelector()
        
        // Play/Pause button
        binding.playPauseButton.setOnClickListener {
            if (isRunning) {
                pauseSimulation()
            } else {
                startSimulation()
            }
        }
        
        // Reset button
        binding.resetButton.setOnClickListener {
            resetSimulation()
        }
        
        // Start Event button
        binding.startEventButton.setOnClickListener {
            simulationEngine?.startEvent()
            binding.startEventButton.isEnabled = false
        }
        
        // Info button
        binding.infoButton.setOnClickListener {
            showSimulationInfo()
        }
        
        // Speed control
        val speedOptions = arrayOf("1x", "2x", "5x", "10x")
        val speedAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speedOptions)
        speedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.speedSpinner.adapter = speedAdapter
        binding.speedSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                simulationSpeed = when (position) {
                    0 -> 1
                    1 -> 2
                    2 -> 5
                    3 -> 10
                    else -> 1
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // People count slider
        binding.peopleCountSlider.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val count = 10 + progress * 2 // 10 to 210 people
                binding.peopleCountText.text = "People: $count"
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                if (!isRunning) {
                    initializeSimulation()
                }
            }
        })
        
        // App adoption slider
        binding.appAdoptionSlider.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val percentage = 5 + progress // 5% to 105% (capped at 90%)
                val actualPercentage = percentage.coerceAtMost(90)
                binding.appAdoptionText.text = "App Adoption: $actualPercentage%"
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                if (!isRunning) {
                    initializeSimulation()
                }
            }
        })
        
        // Initialize labels
        binding.peopleCountText.text = "People: ${10 + binding.peopleCountSlider.progress * 2}"
        binding.appAdoptionText.text = "App Adoption: ${(5 + binding.appAdoptionSlider.progress).coerceAtMost(90)}%"
    }
    
    private fun setupScenarioSelector() {
        // Add "Custom" option at the beginning
        val scenarioNames = arrayOf("Eigene Einstellungen") + SimulationScenario.getScenarioNames()
        val scenarioAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, scenarioNames)
        scenarioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.scenarioSpinner.adapter = scenarioAdapter
        
        binding.scenarioSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    // Custom settings
                    useScenario = false
                    currentScenario = null
                    binding.peopleCountSlider.isEnabled = true
                    binding.appAdoptionSlider.isEnabled = true
                } else {
                    // Predefined scenario
                    useScenario = true
                    currentScenario = SimulationScenario.getScenario(position - 1)
                    binding.peopleCountSlider.isEnabled = false
                    binding.appAdoptionSlider.isEnabled = false
                }
                
                if (!isRunning) {
                    initializeSimulation()
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun initializeSimulation() {
        // Example area: roughly 1km x 1km around a center point
        // Using Berlin coordinates as example
        val centerLat = 52.5200
        val centerLon = 13.4050
        val latRange = 0.005 // roughly 500m
        val lonRange = 0.007 // roughly 500m
        
        simulationEngine = if (useScenario && currentScenario != null) {
            // Use predefined scenario
            val scenario = currentScenario!!
            
            // Update UI to show scenario parameters (read-only)
            binding.peopleCountText.text = "People: ${scenario.peopleCount}"
            binding.appAdoptionText.text = "App Adoption: ${(scenario.appAdoptionRate * 100).toInt()}%"
            
            SimulationEngine(
                areaLatMin = centerLat - latRange,
                areaLatMax = centerLat + latRange,
                areaLonMin = centerLon - lonRange,
                areaLonMax = centerLon + lonRange,
                scenario = scenario
            )
        } else {
            // Use custom settings from sliders
            val peopleCount = 10 + binding.peopleCountSlider.progress * 2
            val appAdoptionRate = (5 + binding.appAdoptionSlider.progress).coerceAtMost(90) / 100.0
            
            binding.peopleCountText.text = "People: $peopleCount"
            binding.appAdoptionText.text = "App Adoption: ${(appAdoptionRate * 100).toInt()}%"
            
            SimulationEngine(
                areaLatMin = centerLat - latRange,
                areaLatMax = centerLat + latRange,
                areaLonMin = centerLon - lonRange,
                areaLonMax = centerLon + lonRange,
                peopleCount = peopleCount,
                appAdoptionRate = appAdoptionRate
            )
        }
        
        simulationEngine?.addStateChangeListener(object : SimulationEngine.StateChangeListener {
            override fun onSimulationUpdated(
                people: List<SimulationPerson>,
                wifiNetworks: List<SimulationWiFi>,
                event: SimulationEvent?,
                simulationTime: Long
            ) {
                runOnUiThread {
                    binding.simulationMapView.updateSimulation(people, wifiNetworks, event)
                }
            }
        })
        
        simulationEngine?.initialize()
        binding.startEventButton.isEnabled = true
        updateUI()
    }
    
    private fun startSimulation() {
        isRunning = true
        binding.playPauseButton.text = "Pause"
        updateHandler.post(updateRunnable)
    }
    
    private fun pauseSimulation() {
        isRunning = false
        binding.playPauseButton.text = "Play"
        updateHandler.removeCallbacks(updateRunnable)
    }
    
    private fun stopSimulation() {
        isRunning = false
        updateHandler.removeCallbacks(updateRunnable)
    }
    
    private fun resetSimulation() {
        pauseSimulation()
        initializeSimulation()
        binding.timeText.text = "Time: 00:00"
    }
    
    private fun updateUI() {
        val stats = simulationEngine?.getStatistics() ?: return
        
        // Update time
        val seconds = stats.simulationTime / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        binding.timeText.text = String.format("Time: %02d:%02d", minutes, secs)
        
        // Update statistics
        binding.statsText.text = buildString {
            append("Total People: ${stats.totalPeople}\n")
            append("With App: ${stats.peopleWithApp} (${(stats.peopleWithApp * 100.0 / stats.totalPeople).toInt()}%)\n")
            append("Informed: ${stats.peopleInformed}\n")
            append("Uninformed: ${stats.peopleUninformed}\n")
            append("WiFi Networks: ${stats.wifiNetworks}\n")
            
            // Show infrastructure status
            val failureMode = when (stats.infrastructureFailure) {
                InfrastructureFailureMode.MOBILE_DATA_ONLY -> "Mobile Data Only"
                InfrastructureFailureMode.DATA_BACKBONE -> "Data Backbone"
                InfrastructureFailureMode.COMPLETE_FAILURE -> "Complete Failure"
            }
            append("Infrastructure: $failureMode\n")
            append("SMS Available: ${if (stats.smsAvailable) "✅ Yes" else "❌ No"}\n")
            
            if (stats.eventOccurred && stats.peopleWithApp > 0) {
                append("Coverage: ${(stats.peopleInformed * 100.0 / stats.peopleWithApp).toInt()}%")
            }
        }
    }
    
    private fun showSimulationInfo() {
        val infoText = buildString {
            append("📊 Wie funktioniert die Simulation?\n\n")
            
            append("🎯 EVENT-ERKENNUNG\n")
            append("• Wenn ein Notfall startet (roter Kreis), erkennen alle Personen mit App im Umkreis von 100m das Event sofort\n")
            append("• Diese Personen werden \"informiert\" und ändern ihre Farbe von grün zu gold\n\n")
            
            append("📡 NACHRICHTENVERBREITUNG\n")
            append("Die Nachricht verbreitet sich auf mehrere Arten:\n\n")
            
            append("1️⃣ Direkte Peer-to-Peer (Bluetooth/WiFi Direct)\n")
            append("   • Informierte Personen teilen die Nachricht mit uninformierten Personen im Umkreis von 100m\n")
            append("   • Bei Personen in Gebäuden (graue Mitte) ist die Reichweite um 40% reduziert\n\n")
            
            append("2️⃣ WiFi-Netzwerke (blaue Kreise)\n")
            append("   • WiFi-Sofortpropagierung nur bei 'Nur Mobile Daten ausgefallen' (WiFi Backbone intakt)\n")
            append("   • Wenn eine informierte Person in Reichweite eines WiFi-Netzwerks ist, werden ALLE anderen Personen im selben Netzwerk sofort informiert (nur mit Internet)\n")
            append("   • Bei anderen Modi (Backbone/Komplett-Ausfall): WiFi funktioniert nur lokal ohne Sofortpropagierung\n")
            append("   • WiFi-Reichweite: 50m\n\n")
            
            append("3️⃣ Mündliche Übertragung (nur kritische Szenarien)\n")
            append("   • Bei schweren Infrastrukturausfällen (Backbone oder komplett) informieren Personen andere verbal\n")
            append("   • Reichweite: 15-30m je nach Umgebung (Stadt vs. Dorf)\n")
            append("   • Funktioniert auch bei Personen OHNE App\n")
            append("   • In Gebäuden reduzierte Reichweite (Wände dämpfen Schall)\n\n")
            
            append("🏃 ANNÄHERUNGSVERHALTEN\n")
            append("• Bei kritischen Szenarien (Backbone/Komplett-Ausfall) zeigen informierte Personen Annäherungsverhalten\n")
            append("• Orangefarbene Umrandung = Person nähert sich aktiv jemandem, um zu informieren\n")
            append("• Annäherungsgeschwindigkeit: 7 km/h (schneller als normales Gehen mit 5 km/h)\n")
            append("• Reichweite variiert: 50m (Großstadt) bis 150m (Dorf)\n\n")
            
            append("🏢 PERSONEN-SYMBOLE\n")
            append("🟢 Grün = Hat die App, noch nicht informiert\n")
            append("🟡 Gold = Hat die App und ist informiert\n")
            append("⚫ Grau = Hat die App nicht\n")
            append("Schwarze Umrandung = Person bewegt sich normal\n")
            append("🟠 Orange Umrandung = Person nähert sich jemandem aktiv (dicker)\n")
            append("Graue Mitte = Person ist in Gebäude (reduzierte Signalreichweite)\n\n")
            
            append("📶 INFRASTRUKTUR-AUSFALLMODI\n\n")
            
            append("Nur Mobile Daten ausgefallen:\n")
            append("✅ SMS verfügbar\n")
            append("✅ WiFi funktioniert mit Internet (Backbone intakt)\n")
            append("✅ WiFi-Sofortpropagierung aktiviert\n")
            append("❌ Keine mündliche Übertragung\n")
            append("❌ Kein Annäherungsverhalten\n\n")
            
            append("Daten Backbone ausgefallen:\n")
            append("✅ SMS verfügbar\n")
            append("✅ WiFi lokal funktioniert (kein Internet)\n")
            append("❌ Keine WiFi-Sofortpropagierung (Backbone fehlt)\n")
            append("✅ Mündliche Übertragung aktiv\n")
            append("✅ Annäherungsverhalten aktiv\n\n")
            
            append("Telefon auch ausgefallen:\n")
            append("❌ SMS NICHT verfügbar\n")
            append("✅ Nur lokales WiFi/Bluetooth (kein Internet)\n")
            append("❌ Keine WiFi-Sofortpropagierung (Backbone fehlt)\n")
            append("✅ Mündliche Übertragung aktiv\n")
            append("✅ Annäherungsverhalten aktiv\n\n")
            
            append("📈 WICHTIGE METRIKEN\n")
            append("• Abdeckung = Informierte / Personen mit App\n")
            append("• Zeit zeigt simulierte Zeit (nicht Echtzeit)\n")
            append("• Geschwindigkeitsregler (1x-10x) beschleunigt die Simulation\n\n")
            
            append("💡 TIPPS\n")
            append("• Beobachten Sie, wie sich die Nachricht von der Event-Position ausbreitet\n")
            append("• Im Modus 'Nur Mobile Daten ausgefallen': WiFi-Netzwerke erweitern die Reichweite plötzlich durch Sofortpropagierung\n")
            append("• In anderen Modi: WiFi funktioniert nur lokal ohne Sofortpropagierung\n")
            append("• In kritischen Szenarien sehen Sie orangefarbene Personen, die aktiv andere suchen\n")
            append("• Personen in Gebäuden (graue Mitte) haben kürzere Reichweiten\n")
            append("• Bewegende Personen helfen, die Nachricht in neue Gebiete zu tragen")
        }
        
        AlertDialog.Builder(this)
            .setTitle("ℹ️ Simulations-Informationen")
            .setMessage(infoText)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
