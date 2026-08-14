package vn.edu.uit.tpkd.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniformImuResamplerTest {
    @Test
    fun `resamples 50 Hz sensor streams to exact 20 Hz grid`() {
        val resampler = UniformImuResampler(sampleRateHz = 20)
        val output = mutableListOf<UniformImuResampler.Sample>()

        for (millis in 0L..1_000L step 20L) {
            val timestamp = millis * 1_000_000L
            output += resampler.addAccelerometer(
                timestamp,
                floatArrayOf(millis.toFloat(), 2f * millis, 3f * millis)
            )
            output += resampler.addGyroscope(
                timestamp,
                floatArrayOf(4f * millis, 5f * millis, 6f * millis)
            )
        }

        assertEquals(21, output.size)
        output.forEachIndexed { index, sample ->
            val expectedMillis = index * 50L
            assertEquals(expectedMillis * 1_000_000L, sample.timestampNs)
            assertEquals(expectedMillis.toFloat(), sample.values[0], 0.001f)
            assertEquals((6L * expectedMillis).toFloat(), sample.values[5], 0.001f)
        }
    }

    @Test
    fun `synchronizes streams with different timestamp offsets`() {
        val resampler = UniformImuResampler(sampleRateHz = 20)
        val output = mutableListOf<UniformImuResampler.Sample>()

        for (index in 0..60) {
            val accMillis = index * 20L
            val gyroMillis = accMillis + 10L
            output += resampler.addAccelerometer(
                accMillis * 1_000_000L,
                floatArrayOf(accMillis.toFloat(), 0f, 0f)
            )
            output += resampler.addGyroscope(
                gyroMillis * 1_000_000L,
                floatArrayOf(gyroMillis.toFloat(), 0f, 0f)
            )
        }

        assertTrue(output.size >= 20)
        output.zipWithNext().forEach { (first, second) ->
            assertEquals(50_000_000L, second.timestampNs - first.timestampNs)
        }
        output.forEach { sample ->
            assertEquals(sample.values[0], sample.values[3], 0.001f)
        }
    }

    @Test
    fun `does not interpolate across a long sensor outage`() {
        val resampler = UniformImuResampler(
            sampleRateHz = 20,
            maxInterpolationGapNs = 200_000_000L
        )
        val output = mutableListOf<UniformImuResampler.Sample>()

        for (millis in listOf(0L, 50L, 100L, 1_000L, 1_050L, 1_100L)) {
            val timestamp = millis * 1_000_000L
            output += resampler.addAccelerometer(timestamp, floatArrayOf(1f, 2f, 3f))
            output += resampler.addGyroscope(timestamp, floatArrayOf(4f, 5f, 6f))
        }

        assertTrue(output.none { it.timestampNs in 150_000_000L..999_999_999L })
        assertTrue(output.any { it.timestampNs == 1_000_000_000L })
    }
}

