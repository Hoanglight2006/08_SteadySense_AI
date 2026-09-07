/*
 * Copyright 2026 SteadySense AI Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
