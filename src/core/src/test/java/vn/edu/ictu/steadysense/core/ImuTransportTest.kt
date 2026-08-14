package vn.edu.ictu.steadysense.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImuTransportTest {
    @Test
    fun assemblerPairsSensorsByTimestampAndResamples() {
        val assembler = ImuWindowAssembler(
            epochOffsetNanos = 1_000_000L,
            targetIntervalNanos = 50L,
            maxSensorSkewNanos = 10L,
            windowSize = 2,
        )
        assembler.onGyroscope(vector(95L, 4f))
        assertNull(assembler.onAccelerometer(vector(100L, 1f)))
        assembler.onGyroscope(vector(120L, 5f))
        assertNull(assembler.onAccelerometer(vector(125L, 2f))) // too soon
        assembler.onGyroscope(vector(154L, 6f))
        val window = assembler.onAccelerometer(vector(155L, 3f))

        requireNotNull(window)
        assertEquals(2, window.frames.size)
        assertEquals(1_000_100L, window.frames.first().timestampEpochNanos)
        assertEquals(4f, window.frames.first().gyroX)
        assertEquals(3f, window.frames.last().accelX)
    }

    @Test
    fun assemblerRejectsSamplesWithExcessiveSkew() {
        val assembler = ImuWindowAssembler(0L, 1L, 5L, 1)
        assembler.onGyroscope(vector(10L, 1f))
        assertNull(assembler.onAccelerometer(vector(20L, 1f)))
    }

    @Test
    fun payloadEnvelopeAndAckRoundTrip() {
        val window = ImuWindow(
            listOf(ImuFrame(123L, 1f, 2f, 3f, 4f, 5f, 6f)),
        )
        val payload = ImuPayloadCodec.encode(window)
        val envelope = TransportEnvelope("session-1", 7L, 123L, payload)

        assertEquals(window, ImuPayloadCodec.decode(payload))
        assertEquals(envelope, TransportEnvelopeCodec.decode(TransportEnvelopeCodec.encode(envelope)))
        val ack = TransportAck("session-1", 7L)
        assertEquals(ack, TransportAckCodec.decode(TransportAckCodec.encode(ack)))
        assertTrue(TransportEnvelopeCodec.encode(envelope).size < 100_000)
    }

    private fun vector(timestamp: Long, x: Float) = SensorVector(timestamp, x, 0f, 0f)
}
