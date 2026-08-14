package vn.edu.uit.tpkd.wear

/**
 * Resamples asynchronous accelerometer and gyroscope streams onto one uniform
 * timestamp grid. Sensor timestamps are monotonic nanoseconds supplied by
 * [android.hardware.SensorEvent.timestamp].
 *
 * Linear interpolation avoids tying the model input rate to a device-specific
 * `SENSOR_DELAY_*` delivery rate. A long sensor gap resets the interpolation
 * state rather than fabricating samples across missing data.
 */
class UniformImuResampler(
    sampleRateHz: Int = 20,
    private val maxInterpolationGapNs: Long = 250_000_000L
) {
    data class Sample(val timestampNs: Long, val values: FloatArray)

    private data class TimedVector(val timestampNs: Long, val values: FloatArray)

    private val periodNs = 1_000_000_000L / sampleRateHz
    private var previousAcc: TimedVector? = null
    private var currentAcc: TimedVector? = null
    private var previousGyro: TimedVector? = null
    private var currentGyro: TimedVector? = null
    private var nextTimestampNs: Long? = null

    init {
        require(sampleRateHz > 0) { "sampleRateHz must be positive" }
        require(1_000_000_000L % sampleRateHz == 0L) {
            "sampleRateHz must divide one second exactly"
        }
        require(maxInterpolationGapNs >= periodNs) {
            "maxInterpolationGapNs must be at least one sample period"
        }
    }

    fun addAccelerometer(timestampNs: Long, values: FloatArray): List<Sample> {
        val updated = updateStream(timestampNs, values, currentAcc) { previous, current ->
            previousAcc = previous
            currentAcc = current
        }
        return if (updated) drain() else emptyList()
    }

    fun addGyroscope(timestampNs: Long, values: FloatArray): List<Sample> {
        val updated = updateStream(timestampNs, values, currentGyro) { previous, current ->
            previousGyro = previous
            currentGyro = current
        }
        return if (updated) drain() else emptyList()
    }

    fun reset() {
        previousAcc = null
        currentAcc = null
        previousGyro = null
        currentGyro = null
        nextTimestampNs = null
    }

    private fun updateStream(
        timestampNs: Long,
        values: FloatArray,
        current: TimedVector?,
        assign: (TimedVector?, TimedVector) -> Unit
    ): Boolean {
        require(values.size >= AXES) { "A sensor sample must contain three axes" }
        if (current != null && timestampNs <= current.timestampNs) return false

        val next = TimedVector(timestampNs, values.copyOf(AXES))
        if (current != null && timestampNs - current.timestampNs > maxInterpolationGapNs) {
            assign(null, next)
            nextTimestampNs = null
        } else {
            assign(current, next)
        }
        return true
    }

    private fun drain(): List<Sample> {
        val acc0 = previousAcc ?: return emptyList()
        val acc1 = currentAcc ?: return emptyList()
        val gyro0 = previousGyro ?: return emptyList()
        val gyro1 = currentGyro ?: return emptyList()

        var next = nextTimestampNs ?: maxOf(acc0.timestampNs, gyro0.timestampNs)
        val bracketStart = maxOf(acc0.timestampNs, gyro0.timestampNs)
        while (next < bracketStart) next += periodNs

        val bracketEnd = minOf(acc1.timestampNs, gyro1.timestampNs)
        if (next > bracketEnd) {
            nextTimestampNs = next
            return emptyList()
        }

        val output = ArrayList<Sample>()
        while (next <= bracketEnd) {
            val acc = interpolate(acc0, acc1, next)
            val gyro = interpolate(gyro0, gyro1, next)
            output += Sample(next, floatArrayOf(
                acc[0], acc[1], acc[2], gyro[0], gyro[1], gyro[2]
            ))
            next += periodNs
        }
        nextTimestampNs = next
        return output
    }

    private fun interpolate(start: TimedVector, end: TimedVector, timestampNs: Long): FloatArray {
        val span = end.timestampNs - start.timestampNs
        if (span == 0L) return start.values.copyOf()
        val ratio = (timestampNs - start.timestampNs).toDouble() / span.toDouble()
        return FloatArray(AXES) { axis ->
            (start.values[axis] + (end.values[axis] - start.values[axis]) * ratio).toFloat()
        }
    }

    companion object {
        private const val AXES = 3
    }
}

