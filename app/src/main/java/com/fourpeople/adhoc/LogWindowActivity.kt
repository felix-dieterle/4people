package com.fourpeople.adhoc

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fourpeople.adhoc.util.LogManager

/**
 * Activity that displays a log window showing all actions, events, messages, and state changes.
 */
class LogWindowActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LogAdapter
    private lateinit var emptyView: TextView
    
    private val logListener = object : LogManager.LogListener {
        override fun onNewLogEntry(entry: LogManager.LogEntry) {
            runOnUiThread {
                adapter.addEntry(entry)
                updateEmptyView()
                // Auto-scroll to bottom
                recyclerView.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_window)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "System Log"
        
        emptyView = findViewById(R.id.emptyView)
        recyclerView = findViewById(R.id.logRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = LogAdapter(LogManager.getLogEntries().toMutableList())
        recyclerView.adapter = adapter
        
        updateEmptyView()
        
        // Scroll to bottom
        if (adapter.itemCount > 0) {
            recyclerView.scrollToPosition(adapter.itemCount - 1)
        }
        
        // Register listener for new log entries
        LogManager.addListener(logListener)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        LogManager.removeListener(logListener)
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_log_window, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_clear_logs -> {
                showClearLogsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showClearLogsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Logs")
            .setMessage("Are you sure you want to clear all log entries?")
            .setPositiveButton("Clear") { _, _ ->
                LogManager.clearLogs()
                adapter.clearEntries()
                updateEmptyView()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateEmptyView() {
        if (adapter.itemCount == 0) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    private class LogAdapter(
        private val entries: MutableList<LogManager.LogEntry>
    ) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {
        
        class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
            val levelTextView: TextView = itemView.findViewById(R.id.levelTextView)
            val tagTextView: TextView = itemView.findViewById(R.id.tagTextView)
            val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_log_entry, parent, false)
            return LogViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
            val entry = entries[position]
            
            holder.timestampTextView.text = entry.getFormattedTimestamp()
            holder.levelTextView.text = entry.level.name
            holder.tagTextView.text = entry.tag
            holder.messageTextView.text = entry.message
            
            // Color code by level
            val color = when (entry.level) {
                LogManager.LogLevel.INFO -> Color.parseColor("#2196F3") // Blue
                LogManager.LogLevel.WARNING -> Color.parseColor("#FF9800") // Orange
                LogManager.LogLevel.ERROR -> Color.parseColor("#F44336") // Red
                LogManager.LogLevel.EVENT -> Color.parseColor("#4CAF50") // Green
                LogManager.LogLevel.STATE_CHANGE -> Color.parseColor("#9C27B0") // Purple
                LogManager.LogLevel.MESSAGE -> Color.parseColor("#00BCD4") // Cyan
            }
            holder.levelTextView.setTextColor(color)
        }
        
        override fun getItemCount(): Int = entries.size
        
        fun addEntry(entry: LogManager.LogEntry) {
            entries.add(entry)
            notifyItemInserted(entries.size - 1)
        }
        
        fun clearEntries() {
            entries.clear()
            notifyDataSetChanged()
        }
    }
}
