package com.fourpeople.adhoc.mesh

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Tests for RouteEntry and routing logic.
 */
class RouteEntryTest {
    
    @Test
    fun routeEntryCreation() {
        val route = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 2,
            sequenceNumber = 5
        )
        
        assertEquals("device2", route.destinationId)
        assertEquals("device1", route.nextHopId)
        assertEquals(2, route.hopCount)
        assertEquals(5, route.sequenceNumber)
        assertTrue(route.isValid)
    }
    
    @Test
    fun routeExpirationTimeout() {
        assertEquals(30_000L, RouteEntry.ROUTE_TIMEOUT_MS)
    }
    
    @Test
    fun freshRouteIsNotExpired() {
        val route = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 2,
            sequenceNumber = 5
        )
        
        assertFalse(route.isExpired())
    }
    
    @Test
    fun routeComparisonBySequenceNumber() {
        val route1 = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 3,
            sequenceNumber = 5
        )
        
        val route2 = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 2,
            sequenceNumber = 3
        )
        
        // route1 has higher sequence number, so it's better
        assertTrue(route1.isBetterThan(route2))
        assertFalse(route2.isBetterThan(route1))
    }
    
    @Test
    fun routeComparisonByHopCount() {
        val route1 = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 2,
            sequenceNumber = 5
        )
        
        val route2 = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 3,
            sequenceNumber = 5
        )
        
        // Same sequence number, route1 has fewer hops
        assertTrue(route1.isBetterThan(route2))
        assertFalse(route2.isBetterThan(route1))
    }
    
    @Test
    fun validRouteIsBetterThanNull() {
        val route = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 2,
            sequenceNumber = 5
        )
        
        assertTrue(route.isBetterThan(null))
    }
    
    @Test
    fun validRouteIsBetterThanInvalidRoute() {
        val validRoute = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 5,
            sequenceNumber = 1,
            isValid = true
        )
        
        val invalidRoute = RouteEntry(
            destinationId = "device2",
            nextHopId = "device1",
            hopCount = 2,
            sequenceNumber = 10,
            isValid = false
        )
        
        assertTrue(validRoute.isBetterThan(invalidRoute))
    }
}
