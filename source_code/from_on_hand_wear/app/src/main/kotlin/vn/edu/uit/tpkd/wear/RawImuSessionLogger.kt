package vn.edu.uit.tpkd.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

/**
 * Writes raw, unresampled, unrotated accelerometer + gyroscope events to two
 * WISDM-raw-format text files (one per sensor), so a future offline
 * preprocessing pass can reuse `read_raw_file()` / `merge_accel_gyro()` /
 * `estimate_actual_hz()` from preprocess/01_wisdm_6axis_win300.py almost
 * unchanged -- see paper/AIMCS_2026/DATA_COLLECTION_PROTOCOL.md.
 *
 * Line format matches the original WISDM raw dump exactly:
 *   <subject_id>,<activity_code>,<timestamp_ns>,<x>,<y>,<z>;
 *
 * Deliberately bypasses IMUCollector/UniformImuResampler/OrientationCorrector
 * -- this is a data-COLLECTION path, not an inference path. Capturing the
 * true raw stream (native delivery rate, unrotated axes, real timestamps)
 * keeps every downstream choice (target Hz, orientation convention, window
 * size) reversible; baking any of those in at collection time would not be.
 */
class RawImuSessionLogger(
    private val context: Context,
    private val subjectId: String
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accWriter: OutputStreamWriter? = null
    private var gyroWriter: OutputStreamWriter? = null

    @Volatile private var activityCode: Char? = null
    @Volatile var accSampleCount = 0L; private set
    @Volatile var gyroSampleCount = 0L; private set

    fun outputDir(): File =
        File(context.getExternalFilesDir(null), "collected").apply { mkdirs() }

    fun start() {
        val dir = outputDir()
        accWriter = OutputStreamWriter(
            FileOutputStream(File(dir, "data_${subjectId}_accel_watch.txt"), /* append = */ true)
        )
        gyroWriter = OutputStreamWriter(
            FileOutputStream(File(dir, "data_${subjectId}_gyro_watch.txt"), /* append = */ true)
        )

        val acc = requireNotNull(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) {
            "Accelerometer is not available"
        }
        val gyro = requireNotNull(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)) {
            "Gyroscope is not available"
        }
        // SENSOR_DELAY_GAME (not a custom period): for data collection we want
        // whatever the device's native rate actually is, unmodified, so the
        // offline pipeline's own frequency-bug detection (estimate_actual_hz)
        // has real data to work with -- see paper/AIMCS_2026 mục 25 for why
        // this project no longer trusts an assumed sensor rate.
        sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
    }

    /** Pass null to pause recording (events are dropped, not written) between activities. */
    fun setActivity(code: Char?) {
        activityCode = code
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        accWriter?.flush(); accWriter?.close(); accWriter = null
        gyroWriter?.flush(); gyroWriter?.close(); gyroWriter = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val code = activityCode ?: return // no label selected right now -> discard
        val line = "$subjectId,$code,${event.timestamp},${event.values[0]},${event.values[1]},${event.values[2]};\n"
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accWriter?.write(line)
                accSampleCount++
                if (accSampleCount % FLUSH_EVERY_N == 0L) accWriter?.flush()
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroWriter?.write(line)
                gyroSampleCount++
                if (gyroSampleCount % FLUSH_EVERY_N == 0L) gyroWriter?.flush()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    companion object {
        private const val FLUSH_EVERY_N = 50L
    }
}

