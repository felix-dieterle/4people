package com.fourpeople.adhoc

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fourpeople.adhoc.databinding.ActivityLocationMapBinding
import com.fourpeople.adhoc.location.LocationData

/**
 * Activity to display locations of all participants in the emergency network.
 * Shows a list of participants with their GPS coordinates and help status.
 */
class LocationMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationMapBinding
    private var adapter: LocationAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.location_map_title)

        setupRecyclerView()
        
        // Note: In a full implementation, this would bind to the service
        // to get location updates in real-time. For now, we'll show a placeholder.
        showPlaceholder()
    }

    private fun setupRecyclerView() {
        adapter = LocationAdapter()
        binding.participantRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.participantRecyclerView.adapter = adapter
    }

    private fun showPlaceholder() {
        // Placeholder implementation
        Toast.makeText(
            this,
            getString(R.string.location_map_active),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    /**
     * Adapter for displaying participant locations.
     */
    private inner class LocationAdapter : RecyclerView.Adapter<LocationViewHolder>() {
        private val locations = mutableListOf<LocationData>()

        fun updateLocations(newLocations: List<LocationData>) {
            locations.clear()
            locations.addAll(newLocations)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): LocationViewHolder {
            val view = layoutInflater.inflate(android.R.layout.simple_list_item_2, parent, false)
            return LocationViewHolder(view)
        }

        override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
            holder.bind(locations[position])
        }

        override fun getItemCount() = locations.size
    }

    /**
     * ViewHolder for participant location items.
     */
    private class LocationViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        private val text1: android.widget.TextView = view.findViewById(android.R.id.text1)
        private val text2: android.widget.TextView = view.findViewById(android.R.id.text2)

        fun bind(location: LocationData) {
            val deviceLabel = if (location.isHelpRequest) {
                "🆘 ${location.deviceId} - HELP NEEDED"
            } else {
                location.deviceId
            }
            
            // Sanitize help message by limiting length and removing control characters
            val sanitizedHelpMessage = location.helpMessage
                ?.take(100)  // Limit to 100 characters
                ?.replace(Regex("[\\p{C}]"), "")  // Remove control characters
            
            text1.text = deviceLabel
            text2.text = "Lat: ${String.format("%.6f", location.latitude)}, " +
                        "Lon: ${String.format("%.6f", location.longitude)}\n" +
                        "Accuracy: ${location.accuracy}m" +
                        if (sanitizedHelpMessage != null) " - $sanitizedHelpMessage" else ""
        }
    }
}
