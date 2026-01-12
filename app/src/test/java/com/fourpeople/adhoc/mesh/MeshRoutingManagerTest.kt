package com.fourpeople.adhoc.mesh

import android.content.Context
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.Mockito.*

/**
 * Tests for MeshRoutingManager functionality.
 */
class MeshRoutingManagerTest {
    
    private lateinit var context: Context
    private lateinit var routingManager: MeshRoutingManager
    private val deviceId = "testDevice"
    
    @Before
    fun setup() {
        context = mock(Context::class.java)
        routingManager = MeshRoutingManager(context, deviceId)
    }
    
    @Test
    fun routingManagerInitialization() {
        assertNotNull(routingManager)
    }
    
    @Test
    fun neighborDiscovery() {
        // Initially no neighbors
        assertEquals(0, routingManager.getNeighborCount())
        
        // Simulate receiving a HELLO message
        val helloMessage = MeshMessage(
            sourceId = "device1",
            destinationId = MeshMessage.BROADCAST_DESTINATION,
            payload = "HELLO",
            messageType = MeshMessage.MessageType.HELLO,
            ttl = 1
        )
        
        routingManager.receiveMessage(helloMessage, "device1")
        
        // Should now have one neighbor
        assertEquals(1, routingManager.getNeighborCount())
    }
    
    @Test
    fun broadcastMessage() {
        val forwarder = mock(MeshRoutingManager.MessageForwarder::class.java)
        routingManager.setMessageForwarder(forwarder)
        
        // Add a neighbor first
        routingManager.receiveMessage(
            MeshMessage(
                sourceId = "device1",
                destinationId = MeshMessage.BROADCAST_DESTINATION,
                payload = "HELLO",
                messageType = MeshMessage.MessageType.HELLO
            ),
            "device1"
        )
        
        // Broadcast a message
        routingManager.broadcastMessage("Test broadcast")
        
        // Verify forwarder was called
        verify(forwarder, atLeastOnce()).forwardMessage(any(), anyString())
    }
    
    @Test
    fun dataMessageDelivery() {
        val listener = mock(MeshRoutingManager.MessageListener::class.java)
        routingManager.setMessageListener(listener)
        
        // Receive a message destined for this device
        val message = MeshMessage(
            sourceId = "device1",
            destinationId = deviceId,
            payload = "Test message",
            messageType = MeshMessage.MessageType.DATA
        )
        
        routingManager.receiveMessage(message, "device1")
        
        // Verify listener was called
        verify(listener, times(1)).onMessageReceived(any())
    }
    
    @Test
    fun duplicateMessageDetection() {
        val listener = mock(MeshRoutingManager.MessageListener::class.java)
        routingManager.setMessageListener(listener)
        
        val message = MeshMessage(
            messageId = "unique123",
            sourceId = "device1",
            destinationId = deviceId,
            payload = "Test",
            messageType = MeshMessage.MessageType.DATA
        )
        
        // Receive message first time
        routingManager.receiveMessage(message, "device1")
        verify(listener, times(1)).onMessageReceived(any())
        
        // Receive same message again (duplicate)
        routingManager.receiveMessage(message, "device1")
        
        // Listener should still only be called once
        verify(listener, times(1)).onMessageReceived(any())
    }
    
    @Test
    fun messageForwardingWithTTL() {
        val forwarder = mock(MeshRoutingManager.MessageForwarder::class.java)
        routingManager.setMessageForwarder(forwarder)
        
        // Add two neighbors
        routingManager.receiveMessage(
            MeshMessage(
                sourceId = "device1",
                destinationId = MeshMessage.BROADCAST_DESTINATION,
                payload = "HELLO",
                messageType = MeshMessage.MessageType.HELLO
            ),
            "device1"
        )
        
        // Receive a broadcast message with TTL
        val message = MeshMessage(
            sourceId = "device2",
            destinationId = MeshMessage.BROADCAST_DESTINATION,
            payload = "Broadcast",
            messageType = MeshMessage.MessageType.DATA,
            ttl = 5
        )
        
        routingManager.receiveMessage(message, "device2")
        
        // Should forward to neighbors
        verify(forwarder, atLeastOnce()).forwardMessage(any(), anyString())
    }
    
    @Test
    fun messageWithZeroTTLNotForwarded() {
        val forwarder = mock(MeshRoutingManager.MessageForwarder::class.java)
        routingManager.setMessageForwarder(forwarder)
        
        // Message with TTL 0 should not be forwarded
        val message = MeshMessage(
            sourceId = "device1",
            destinationId = "device3",
            payload = "Test",
            messageType = MeshMessage.MessageType.DATA,
            ttl = 0
        )
        
        routingManager.receiveMessage(message, "device1")
        
        // Should not forward
        verify(forwarder, never()).forwardMessage(
            argThat { it.destinationId == "device3" },
            anyString()
        )
    }
    
    @Test
    fun routeRequestHandling() {
        val forwarder = mock(MeshRoutingManager.MessageForwarder::class.java)
        routingManager.setMessageForwarder(forwarder)
        
        // Receive route request
        val rreq = MeshMessage(
            sourceId = "device1",
            destinationId = "device3",
            payload = "",
            messageType = MeshMessage.MessageType.ROUTE_REQUEST
        )
        
        routingManager.receiveMessage(rreq, "device1")
        
        // Should either reply or forward
        verify(forwarder, atLeastOnce()).forwardMessage(any(), anyString())
    }
    
    @Test
    fun routeReplyForSelf() {
        val forwarder = mock(MeshRoutingManager.MessageForwarder::class.java)
        routingManager.setMessageForwarder(forwarder)
        
        // Receive route request destined for this device
        val rreq = MeshMessage(
            sourceId = "device1",
            destinationId = deviceId,
            payload = "",
            messageType = MeshMessage.MessageType.ROUTE_REQUEST,
            sequenceNumber = 1
        )
        
        routingManager.receiveMessage(rreq, "device1")
        
        // Should send route reply
        verify(forwarder, atLeastOnce()).forwardMessage(
            argThat { it.messageType == MeshMessage.MessageType.ROUTE_REPLY },
            anyString()
        )
    }
    
    @Test
    fun knownRoutesTracking() {
        // Initially no routes
        assertEquals(0, routingManager.getKnownRoutes().size)
        
        // Receive a message which should create a reverse route
        val message = MeshMessage(
            sourceId = "device1",
            destinationId = deviceId,
            payload = "Test",
            messageType = MeshMessage.MessageType.DATA
        )
        
        routingManager.receiveMessage(message, "device1")
        
        // Should now have at least one route (to device1)
        assertTrue(routingManager.getKnownRoutes().size > 0)
    }
}
