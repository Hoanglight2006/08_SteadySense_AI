package vn.edu.ictu.steadysense.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReliableOutboxTest {
    @Test
    fun itemRemainsPendingUntilAcknowledged() {
        val outbox = ReliableOutbox()
        val item = envelope(1)
        assertTrue(outbox.enqueue(item))
        assertEquals(listOf(item), outbox.pendingBatch(10))
        assertTrue(outbox.acknowledge("session-a", 1))
        assertEquals(0, outbox.pendingCount())
    }

    @Test
    fun duplicateSequenceIsRejectedBeforeAndAfterAck() {
        val outbox = ReliableOutbox()
        assertTrue(outbox.enqueue(envelope(2)))
        assertFalse(outbox.enqueue(envelope(2)))
        assertTrue(outbox.acknowledge("session-a", 2))
        assertFalse(outbox.enqueue(envelope(2)))
    }

    @Test
    fun failedAckDoesNotDeleteAnotherItem() {
        val outbox = ReliableOutbox()
        outbox.enqueue(envelope(3))
        assertFalse(outbox.acknowledge("session-a", 99))
        assertEquals(1, outbox.pendingCount())
    }

    private fun envelope(sequence: Long) = TransportEnvelope(
        sessionId = "session-a",
        sequenceId = sequence,
        capturedAtEpochNanos = 1_000L + sequence,
        payload = byteArrayOf(sequence.toByte()),
    )
}
