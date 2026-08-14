package vn.edu.uit.tpkd.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Collects 6-axis IMU data (acc + gyro) and resamples both streams to 20 Hz.
 * Buffers 300 samples (15-second window) then delivers to [onWindowReady].
 *
 * Sampling: request sensor events at 50 Hz, then interpolate by event timestamp
 * onto an exact 20-Hz grid. This is independent of the actual device delivery
 * rate and replaces the old every-fifth-event downsampling assumption.
 * Window: 300 samples, stride 150 (50% overlap). 2026-07-30: was
 * WINDOW_SIZE=100/STRIDE=50 (5s) -- T=300 (15s) is the best-validated model
 * (Student F1 63.84% single-split / 63.56% LOSO vs 58.29%/60.28% at T=100),
 * see CLAUDE.md muc 19.
 *
 * 2026-08-11: every resampled sample is also passed through
 * [OrientationCorrector] before entering the window buffer, so the model
 * sees gravity aligned the same way training data was (see that class's
 * doc comment) instead of whatever angle the watch happens to be worn at.
 */
class IMUCollector(
    context: Context,
    private val onWindowReady: (FloatArray) -> Unit   // (300 × 6) row-major
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Circular buffer: stores (acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z) per sample
    private val window = Array(WINDOW_SIZE) { FloatArray(6) }
    private var count  = 0
    private val resampler = UniformImuResampler(sampleRateHz = TARGET_SAMPLE_RATE_HZ)
    private val orientationCorrector = OrientationCorrector()

    fun start() {
        val acc = requireNotNull(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) {
            "Accelerometer is not available"
        }
        val gyro = requireNotNull(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)) {
            "Gyroscope is not available"
        }
        check(sensorManager.registerListener(this, acc, SENSOR_SOURCE_PERIOD_US)) {
            "Unable to register accelerometer listener"
        }
        check(sensorManager.registerListener(this, gyro, SENSOR_SOURCE_PERIOD_US)) {
            sensorManager.unregisterListener(this, acc)
            "Unable to register gyroscope listener"
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        resampler.reset()
        orientationCorrector.reset()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val samples = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> resampler.addAccelerometer(event.timestamp, event.values)
            Sensor.TYPE_GYROSCOPE -> resampler.addGyroscope(event.timestamp, event.values)
            else -> emptyList()
        }
        for (sample in samples) {
            val acc = sample.values.copyOfRange(0, 3)
            val gyro = sample.values.copyOfRange(3, 6)
            val (correctedAcc, correctedGyro) = orientationCorrector.correct(sample.timestampNs, acc, gyro)
            acceptSample(floatArrayOf(
                correctedAcc[0], correctedAcc[1], correctedAcc[2],
                correctedGyro[0], correctedGyro[1], correctedGyro[2]
            ))
        }
    }

    private fun acceptSample(values: FloatArray) {
        val idx = count % WINDOW_SIZE
        System.arraycopy(values, 0, window[idx], 0, 6)
        count++

        if (count >= WINDOW_SIZE && (count - WINDOW_SIZE) % STRIDE == 0) {
            val flat = FloatArray(WINDOW_SIZE * 6)
            for (i in 0 until WINDOW_SIZE) {
                val src = window[(count - WINDOW_SIZE + i) % WINDOW_SIZE]
                System.arraycopy(src, 0, flat, i * 6, 6)
            }
            onWindowReady(flat)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    companion object {
        const val WINDOW_SIZE       = 300
        const val STRIDE            = 150
        const val TARGET_SAMPLE_RATE_HZ = 20
        const val SENSOR_SOURCE_PERIOD_US = 20_000 // request ~50 Hz before resampling
    }
}

