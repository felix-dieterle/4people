package com.fourpeople.adhoc.mesh

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Tests for RouteTable functionality.
 */
class RouteTableTest {
    
    private lateinit var routeTable: RouteTable
    
    @Before
    fun setup() {
        routeTable = RouteTable()
    }
    
    @Test
    fun addAndRetrieveRoute() {
        val route = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 2,
            sequenceNumber = 5
        )
        
        routeTable.addRoute(route)
        
        val retrieved = routeTable.getRoute("device2")
        assertNotNull(retrieved)
        assertEquals("device2", retrieved?.destinationId)
        assertEquals("device1", retrieved?.nextHopId)
    }
    
    @Test
    fun getNonExistentRoute() {
        val route = routeTable.getRoute("nonexistent")
        assertNull(route)
    }
    
    @Test
    fun updateRouteWithBetterRoute() {
        val route1 = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 3,
            sequenceNumber = 5
        )
        routeTable.addRoute(route1)
        
        val route2 = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 2,
            sequenceNumber = 5
        )
        routeTable.addRoute(route2)
        
        val retrieved = routeTable.getRoute("device2")
        assertEquals(2, retrieved?.hopCount)
    }
    
    @Test
    fun doNotUpdateWithWorseRoute() {
        val route1 = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 2,
            sequenceNumber = 5
        )
        routeTable.addRoute(route1)
        
        val route2 = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 3,
            sequenceNumber = 5
        )
        routeTable.addRoute(route2)
        
        val retrieved = routeTable.getRoute("device2")
        assertEquals(2, retrieved?.hopCount)
    }
    
    @Test
    fun removeRoute() {
        val route = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 2,
            sequenceNumber = 5
        )
        
        routeTable.addRoute(route)
        assertNotNull(routeTable.getRoute("device2"))
        
        routeTable.removeRoute("device2")
        assertNull(routeTable.getRoute("device2"))
    }
    
    @Test
    fun addAndCheckNeighbor() {
        routeTable.addNeighbor("device1")
        
        assertTrue(routeTable.isNeighbor("device1"))
        assertFalse(routeTable.isNeighbor("device2"))
    }
    
    @Test
    fun addNeighborCreatesDirectRoute() {
        routeTable.addNeighbor("device1")
        
        val route = routeTable.getRoute("device1")
        assertNotNull(route)
        assertEquals("device1", route?.destinationId)
        assertEquals("device1", route?.nextHopId)
        assertEquals(1, route?.hopCount)
    }
    
    @Test
    fun removeNeighbor() {
        routeTable.addNeighbor("device1")
        assertTrue(routeTable.isNeighbor("device1"))
        
        routeTable.removeNeighbor("device1")
        assertFalse(routeTable.isNeighbor("device1"))
    }
    
    @Test
    fun removeNeighborInvalidatesRoutesThrough() {
        // Add neighbor
        routeTable.addNeighbor("device1")
        
        // Add route through neighbor
        val route = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 2,
            sequenceNumber = 5
        )
        routeTable.addRoute(route)
        
        assertNotNull(routeTable.getRoute("device2"))
        
        // Remove neighbor
        routeTable.removeNeighbor("device1")
        
        // Route through that neighbor should be removed
        assertNull(routeTable.getRoute("device2"))
    }
    
    @Test
    fun getNeighbors() {
        routeTable.addNeighbor("device1")
        routeTable.addNeighbor("device2")
        routeTable.addNeighbor("device3")
        
        val neighbors = routeTable.getNeighbors()
        assertEquals(3, neighbors.size)
        assertTrue(neighbors.contains("device1"))
        assertTrue(neighbors.contains("device2"))
        assertTrue(neighbors.contains("device3"))
    }
    
    @Test
    fun getAllRoutes() {
        val route1 = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 2,
            sequenceNumber = 5
        )
        val route2 = RouteEntry(
            destinationId = "device3",
            nextHopId = "device1",
            hopCount = 3,
            sequenceNumber = 7
        )
        
        routeTable.addRoute(route1)
        routeTable.addRoute(route2)
        
        val routes = routeTable.getAllRoutes()
        assertTrue(routes.size >= 2)
        assertTrue(routes.containsKey("device2"))
        assertTrue(routes.containsKey("device3"))
    }
    
    @Test
    fun clearRoutesAndNeighbors() {
        routeTable.addNeighbor("device1")
        routeTable.addRoute(RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 2,
            sequenceNumber = 5
        ))
        
        routeTable.clear()
        
        assertEquals(0, routeTable.getNeighbors().size)
        assertEquals(0, routeTable.getAllRoutes().size)
    }
    
    @Test
    fun addDuplicateNeighborOnlyCountsOnce() {
        routeTable.addNeighbor("device1")
        routeTable.addNeighbor("device1")
        
        assertEquals(1, routeTable.getNeighbors().size)
    }
}
