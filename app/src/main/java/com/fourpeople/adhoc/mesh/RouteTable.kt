package com.fourpeople.adhoc.mesh

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages routing information for the mesh network.
 * 
 * Maintains routes to known destinations and neighbor information.
 * Routes are discovered on-demand and cached for efficiency.
 */
class RouteTable {
    
    companion object {
        private const val TAG = "RouteTable"
    }
    
    // Map of destination ID to route entry
    private val routes = ConcurrentHashMap<String, RouteEntry>()
    
    // Set of direct neighbor IDs (one-hop neighbors)
    private val neighbors = ConcurrentHashMap.newKeySet<String>()
    
    /**
     * Adds or updates a route to a destination.
     * Only updates if the new route is better than the existing one.
     */
    fun addRoute(route: RouteEntry) {
        val existing = routes[route.destinationId]
        
        if (route.isBetterThan(existing)) {
            routes[route.destinationId] = route
            Log.d(TAG, "Route updated: ${route.destinationId} via ${route.nextHopId} (${route.hopCount} hops)")
        }
    }
    
    /**
     * Retrieves a route to a destination.
     * Returns null if no valid route exists.
     */
    fun getRoute(destinationId: String): RouteEntry? {
        val route = routes[destinationId]
        
        // Remove expired routes
        if (route != null && route.isExpired()) {
            routes.remove(destinationId)
            Log.d(TAG, "Expired route removed: $destinationId")
            return null
        }
        
        return if (route?.isValid == true) route else null
    }
    
    /**
     * Removes a route to a destination.
     */
    fun removeRoute(destinationId: String) {
        routes.remove(destinationId)
        Log.d(TAG, "Route removed: $destinationId")
    }
    
    /**
     * Adds a direct neighbor.
     */
    fun addNeighbor(neighborId: String) {
        if (neighbors.add(neighborId)) {
            Log.d(TAG, "Neighbor added: $neighborId")
            
            // Add direct route to neighbor
            addRoute(RouteEntry(
                destinationId = neighborId,
                nextHopId = neighborId,
                hopCount = 1,
                sequenceNumber = 0
            ))
        }
    }
    
    /**
     * Removes a neighbor and all routes through it.
     */
    fun removeNeighbor(neighborId: String) {
        neighbors.remove(neighborId)
        Log.d(TAG, "Neighbor removed: $neighborId")
        
        // Remove all routes through this neighbor
        val routesToRemove = routes.filter { it.value.nextHopId == neighborId }
        routesToRemove.keys.forEach { removeRoute(it) }
    }
    
    /**
     * Checks if a node is a direct neighbor.
     */
    fun isNeighbor(nodeId: String): Boolean = neighbors.contains(nodeId)
    
    /**
     * Gets all current neighbors.
     */
    fun getNeighbors(): Set<String> = neighbors.toSet()
    
    /**
     * Gets all known routes.
     */
    fun getAllRoutes(): Map<String, RouteEntry> = routes.toMap()
    
    /**
     * Clears all routing information.
     */
    fun clear() {
        routes.clear()
        neighbors.clear()
        Log.d(TAG, "Route table cleared")
    }
    
    /**
     * Performs periodic maintenance on the route table.
     * Removes expired routes and stale neighbor information.
     */
    fun performMaintenance() {
        // Remove expired routes
        val expiredRoutes = routes.filter { it.value.isExpired() }
        expiredRoutes.keys.forEach { removeRoute(it) }
        
        if (expiredRoutes.isNotEmpty()) {
            Log.d(TAG, "Maintenance: Removed ${expiredRoutes.size} expired routes")
        }
    }
}
