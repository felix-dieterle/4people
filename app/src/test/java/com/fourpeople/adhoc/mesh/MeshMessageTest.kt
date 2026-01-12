package com.fourpeople.adhoc.mesh

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for MeshMessage data structure and functionality.
 */
class MeshMessageTest {
    
    @Test
    fun meshMessageCreation() {
        val message = MeshMessage(
            sourceId = "device1",
            destinationId = "device2",
            payload = "Test message"
        )
        
        assertEquals("device1", message.sourceId)
        assertEquals("device2", message.destinationId)
        assertEquals("Test message", message.payload)
        assertEquals(MeshMessage.MessageType.DATA, message.messageType)
        assertEquals(MeshMessage.DEFAULT_TTL, message.ttl)
        assertEquals(0, message.hopCount)
    }
    
    @Test
    fun meshMessageDefaultTTL() {
        assertEquals(10, MeshMessage.DEFAULT_TTL)
    }
    
    @Test
    fun meshMessageForwarding() {
        val original = MeshMessage(
            sourceId = "device1",
            destinationId = "device2",
            payload = "Test",
            ttl = 5,
            hopCount = 2
        )
        
        val forwarded = original.forward()
        
        assertEquals(4, forwarded.ttl)
        assertEquals(3, forwarded.hopCount)
        assertEquals(original.sourceId, forwarded.sourceId)
        assertEquals(original.destinationId, forwarded.destinationId)
        assertEquals(original.payload, forwarded.payload)
    }
    
    @Test
    fun meshMessageCanForward() {
        val message1 = MeshMessage(
            sourceId = "device1",
            destinationId = "device2",
            payload = "Test",
            ttl = 5
        )
        assertTrue(message1.canForward())
        
        val message2 = MeshMessage(
            sourceId = "device1",
            destinationId = "device2",
            payload = "Test",
            ttl = 0
        )
        assertFalse(message2.canForward())
    }
    
    @Test
    fun meshMessageBroadcastDetection() {
        val broadcast = MeshMessage(
            sourceId = "device1",
            destinationId = MeshMessage.BROADCAST_DESTINATION,
            payload = "Broadcast message"
        )
        assertTrue(broadcast.isBroadcast())
        
        val unicast = MeshMessage(
            sourceId = "device1",
            destinationId = "device2",
            payload = "Unicast message"
        )
        assertFalse(unicast.isBroadcast())
    }
    
    @Test
    fun messageTypesAvailable() {
        // Verify all message types exist
        assertNotNull(MeshMessage.MessageType.DATA)
        assertNotNull(MeshMessage.MessageType.ROUTE_REQUEST)
        assertNotNull(MeshMessage.MessageType.ROUTE_REPLY)
        assertNotNull(MeshMessage.MessageType.ROUTE_ERROR)
        assertNotNull(MeshMessage.MessageType.HELLO)
    }
    
    @Test
    fun messageIdUniqueness() {
        val message1 = MeshMessage(
            sourceId = "device1",
            destinationId = "device2",
            payload = "Test"
        )
        
        val message2 = MeshMessage(
            sourceId = "device1",
            destinationId = "device2",
            payload = "Test"
        )
        
        // Message IDs should be unique
        assertNotEquals(message1.messageId, message2.messageId)
    }
    
    @Test
    fun messageTTLDecrementAfterMultipleForwards() {
        var message = MeshMessage(
            sourceId = "device1",
            destinationId = "device5",
            payload = "Test",
            ttl = 5
        )
        
        // Forward through multiple hops
        message = message.forward() // hop 1
        assertEquals(4, message.ttl)
        assertEquals(1, message.hopCount)
        
        message = message.forward() // hop 2
        assertEquals(3, message.ttl)
        assertEquals(2, message.hopCount)
        
        message = message.forward() // hop 3
        assertEquals(2, message.ttl)
        assertEquals(3, message.hopCount)
    }
}
